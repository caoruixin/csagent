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
  status: string;
}

export interface TraceStep {
  step: number;
  action: string;
  input: string;
  output: string;
  tool_calls?: ToolCall[];
  timestamp: string;
}

export interface ToolCall {
  tool: string;
  args: Record<string, unknown>;
  result: Record<string, unknown>;
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
  created_at: string;
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
