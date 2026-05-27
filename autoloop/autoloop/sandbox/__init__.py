"""autoloop sandbox — structural defenses for meta-agent proposals."""

from .anti_hardcode_check import AntiHardcodeResult, anti_hardcode_check
from .yaml_diff_validator import ValidationResult, validate_skill_yaml_diff

__all__ = [
    "AntiHardcodeResult",
    "ValidationResult",
    "anti_hardcode_check",
    "validate_skill_yaml_diff",
]
