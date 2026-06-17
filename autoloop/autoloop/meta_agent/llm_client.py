"""Thin wrapper around the meta-agent LLM provider.

The meta-agent uses an Anthropic-compatible API (default: AICodeWith
which exposes Claude through Anthropic-style endpoints; see
`autoloop/config.yaml`'s `meta_agent:` block for the deployed
configuration).

Design rules:

- The wrapper is a thin shim, not an abstraction layer. It holds a
  `provider` string and a `model` string and dispatches to one
  underlying SDK. Per `autoloop/config.yaml` the v1 provider is
  `anthropic`; other providers are reserved for later milestones
  and would each be a discrete code path here.
- Credentials come from env vars NAMED in config (default
  `AUTOLOOP_META_LLM_API_KEY` + `AUTOLOOP_META_LLM_BASE_URL`),
  loaded from the repo root `.env.local` by `python-dotenv` if
  available. We do NOT use the global Anthropic env vars to avoid
  colliding with system-wide tools.
- All tests mock this client. The real LLM is invoked ONLY during
  live `python -m autoloop run` sessions; no pytest path calls
  out to the network.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Optional


class LLMClientError(Exception):
    """Raised on configuration or network errors from the meta-agent LLM."""


@dataclass
class LLMResponse:
    """Captured output of one chat completion call."""

    text: str
    raw: Any | None = None


class LLMClient:
    """Provider-dispatching wrapper.

    The constructor accepts the resolved meta_agent config dict (the
    block under `meta_agent:` in `autoloop/config.yaml`). It loads
    `.env.local` from the repo root on first instantiation if
    python-dotenv is importable; otherwise relies on the shell to
    have exported the named env vars.
    """

    def __init__(self, config: dict[str, Any]):
        self.config = config or {}
        self.provider = str(self.config.get("provider", "anthropic")).lower()
        self.model = str(self.config.get("model", "claude-sonnet-4-6"))
        self.temperature = float(self.config.get("temperature", 0.3))
        self.max_tokens = int(self.config.get("max_tokens", 4096))
        self.timeout_seconds = int(self.config.get("request_timeout_seconds", 120))

        api_key_env = self.config.get("api_key_env", "AUTOLOOP_META_LLM_API_KEY")
        base_url_env = self.config.get("base_url_env", "AUTOLOOP_META_LLM_BASE_URL")
        model_env = self.config.get("model_env", "AUTOLOOP_META_LLM_MODEL")

        _load_env_local()
        self.api_key: Optional[str] = os.environ.get(api_key_env) or None
        self.base_url: Optional[str] = os.environ.get(base_url_env) or None
        # Env override: AUTOLOOP_META_LLM_MODEL (loaded from autoloop/.env.local by
        # the CLI at startup, cli.py:62) wins over the config.yaml `model` default.
        _env_model = os.environ.get(model_env)
        if _env_model:
            self.model = _env_model

    def is_configured(self) -> bool:
        """True if the API key env var is set.

        The orchestrator checks this once before starting a live
        iteration so a missing key surfaces as a clean error message
        rather than a cryptic provider exception mid-run.
        """
        return bool(self.api_key)

    def chat(self, system: str, user: str) -> LLMResponse:
        """Run one chat completion. system + user are the only inputs.

        Provider dispatch is keyword-driven; only the v1 default
        `anthropic` is implemented in S-Auto-3. Adding a new provider
        means adding a new branch here.
        """
        if not self.is_configured():
            raise LLMClientError(
                f"LLM API key env var '{self.config.get('api_key_env')}' is not set"
            )

        if self.provider == "anthropic":
            return self._chat_anthropic(system, user)
        raise LLMClientError(
            f"unsupported meta_agent.provider '{self.provider}'; "
            "v1 supports only 'anthropic'"
        )

    def _chat_anthropic(self, system: str, user: str) -> LLMResponse:
        try:
            import anthropic  # type: ignore
        except ImportError as e:
            raise LLMClientError(
                "anthropic SDK not installed; add it via `uv add anthropic` "
                "in the autoloop project (one-time setup before live runs)."
            ) from e

        client_kwargs: dict[str, Any] = {"api_key": self.api_key}
        if self.base_url:
            client_kwargs["base_url"] = self.base_url

        client = anthropic.Anthropic(**client_kwargs)
        response = client.messages.create(
            model=self.model,
            max_tokens=self.max_tokens,
            temperature=self.temperature,
            system=system,
            messages=[{"role": "user", "content": user}],
            timeout=self.timeout_seconds,
        )
        text = _extract_anthropic_text(response)
        return LLMResponse(text=text, raw=response)


def build_client_from_config(config: dict[str, Any]) -> LLMClient:
    """Build an LLMClient from the top-level autoloop config dict.

    Looks up `config['meta_agent']` and instantiates the wrapper.
    A missing meta_agent block raises `LLMClientError` rather than
    silently constructing an unusable client.
    """
    block = (config or {}).get("meta_agent") or {}
    if not block:
        raise LLMClientError(
            "autoloop config has no `meta_agent:` block; cannot build LLM client"
        )
    return LLMClient(block)


def _extract_anthropic_text(response: Any) -> str:
    """Pull the `text` field out of an Anthropic SDK response object.

    Defensive: anthropic SDK occasionally returns mixed content
    blocks (text + tool_use). We concatenate only the text blocks.
    """
    content = getattr(response, "content", None)
    if content is None:
        return ""
    chunks: list[str] = []
    for block in content:
        block_type = getattr(block, "type", None)
        block_text = getattr(block, "text", None)
        if block_type == "text" and isinstance(block_text, str):
            chunks.append(block_text)
    return "\n".join(chunks)


def _load_env_local() -> None:
    """Load the repo-root `.env.local` once via python-dotenv if available.

    Falls back to no-op if python-dotenv is not installed (e.g. in
    minimal CI environments where meta-agent calls are mocked).
    """
    try:
        from dotenv import load_dotenv  # type: ignore
    except ImportError:
        return
    repo_root = Path(__file__).resolve().parents[3]
    env_path = repo_root / ".env.local"
    if env_path.exists():
        load_dotenv(env_path, override=False)
