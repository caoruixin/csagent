// ── Chat Types ──

export interface CreateSessionRequest {
  first_name: string;
  email: string;
  topic_subject: string;
  ad_id?: string;
  description: string;
}

export interface CreateSessionResponse {
  session_id: string;
  reply_text: string;
}

export interface SendMessageRequest {
  message: string;
}

export interface SendMessageResponse {
  reply_text: string;
  intent: string;
  should_end_chat: boolean;
  additional_data: Record<string, unknown>;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'bot';
  text: string;
  timestamp: Date;
  additional_data?: Record<string, unknown>;
  intent?: string;
  should_end_chat?: boolean;
}

export type ChatPhase = 'CLOSED' | 'PRE_CHAT' | 'CHATTING' | 'ENDED';

// ── Admin Types ──

export interface Session {
  session_id: string;
  first_name: string;
  email: string;
  topic_subject: string;
  ad_id?: string;
  description: string;
  use_case: string;
  outcome: string;
  turns: number;
  created_at: string;
  updated_at: string;
  // Coarse semantic status (active / escalated / ended) derived from
  // handling_state; retained for sorting + backward compatibility.
  status: string;
  // Raw BotSession.handling_state value (BOT_HANDLING / QUEUE_TO_HUMAN /
  // HUMAN_HANDLING / CLOSED). Surfaced so the admin badge can render every
  // terminal state distinctly instead of collapsing to three buckets.
  handling_state: string;
  // Canonical escalation_reason enum value when present (ESCALATE /
  // QUEUE_TO_HUMAN sessions); empty otherwise.
  escalation_reason: string;
}

export interface TraceStep {
  step: number;
  action: string;
  input: string;
  output: string;
  tool_calls?: ToolCall[];
  timestamp: string;
  projected_context?: Record<string, unknown>;
  llm_raw_response?: string;
  action_parameters?: Record<string, unknown>;
  source_ids?: string[];
  phase_before?: string;
  phase_after?: string;
  active_use_case?: string;
  latency_ms?: number;
  // Sprint 51 / M5 S2 — per-invocation full-fidelity records from the new
  // `bot_turn_llm_calls` table. One entry per LLM call inside the
  // AgentRunLoop step boundary for this turn (FAQ Skill: up to
  // `max_tool_steps`). The existing `llm_raw_response` / `projected_context`
  // single fields above keep carrying the FINAL step's value
  // (backward-compat); this array carries every step. Default-collapsed in
  // the UI because per-step projections are heavy.
  llm_calls?: LlmCall[];
}

// Sprint 51 / M5 S2 — per-invocation record. Mirrors the
// `bot_turn_llm_calls` row shape exposed via /sessions/{id}/trace.
export interface LlmCall {
  id?: number;
  bot_turn_id?: string;
  step_index: number;
  call_type: string; // "chat" | "routing" | "rerank"
  model?: string;
  latency_ms?: number;
  llm_raw_response?: string;
  // Backend serialises jsonb as a JSON-encoded string (same as
  // BotTurn.projected_context). The viewer parses it lazily on expand.
  projected_context?: string;
  tool_calls?: string;
  created_at?: string;
}

export interface ToolCall {
  tool: string;
  args: Record<string, unknown>;
  result: Record<string, unknown>;
  // Sprint 9 §O2 — backend tool_calls shape: tool_name + arguments +
  // success / latency_ms / error_message / result_data / result_summary.
  // Older legacy rows ship only {tool, args, result}; both are rendered
  // safely by TraceViewer.
  tool_name?: string;
  arguments?: Record<string, unknown>;
  success?: boolean;
  latency_ms?: number;
  error_message?: string;
  result_data?: unknown;
  result_summary?: string;
  source?: string;
  // Sprint 067 / S-Auto-12 (A1 idempotency) — persisted on byte-identical
  // repeats served from the per-run cache (tool not re-dispatched). The
  // backend only emits these keys on a deduplicated event; a normal dispatch
  // carries neither. Surfaced so the trace can fold the repeats by default
  // while preserving the full audit on expand.
  deduplicated?: boolean;
  original_at_step?: number;
  step_index?: number;
  sequence_index?: number;
}

export interface TraceResponse {
  session_id: string;
  steps: TraceStep[];
}

export interface SessionEvent {
  event_id: string;
  event_type: string;
  timestamp: string;
  data: Record<string, unknown>;
}

export interface HandoverLog {
  log_id: string;
  session_id: string;
  case_number: string;
  priority: string;
  reason: string;
  payload: Record<string, unknown>;
  customer_message?: string;
  transcript?: TranscriptEntry[];
  created_at: string;
}

export interface TranscriptEntry {
  role: 'user' | 'bot';
  message: string;
  turn_index: number;
}

export interface FunnelMetrics {
  total_sessions: number;
  self_served: number;
  escalated: number;
  abandoned: number;
  avg_turns: number;
}

export interface PerUCMetric {
  use_case: string;
  total: number;
  self_served: number;
  escalated: number;
  avg_turns: number;
  avg_csat: number;
}

export interface DemoCase {
  case_id: string;
  title: string;
  use_case: string;
  description: string;
  expected_outcome: string;
}

export type AdminTab = 'sessions' | 'traces' | 'handover' | 'events' | 'metrics';

export const TOPIC_OPTIONS = [
  'Ad Support',
  'Account Support',
  'Replies or Messaging',
  'Technical Support',
  'Payments',
  'Delete My Account or Data',
  'Report a Safety Issue',
  'Delivery',
  'Pro Contract',
  'Ratings Reviews',
  'Account Manager Support',
] as const;

export type TopicSubject = (typeof TOPIC_OPTIONS)[number];
