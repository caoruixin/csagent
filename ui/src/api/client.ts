import axios from 'axios';
import type {
  CreateSessionRequest,
  CreateSessionResponse,
  SendMessageRequest,
  SendMessageResponse,
  Session,
  TraceResponse,
  SessionEvent,
  HandoverLog,
  TranscriptEntry,
  FunnelMetrics,
  PerUCMetric,
  DemoCase,
} from '../types';

const api = axios.create({
  baseURL: '/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
});

// ── Chat API ──

export async function createSession(
  data: CreateSessionRequest,
): Promise<CreateSessionResponse> {
  const res = await api.post<CreateSessionResponse>('/chat/sessions', data);
  return res.data;
}

export async function sendMessage(
  sessionId: string,
  data: SendMessageRequest,
): Promise<SendMessageResponse> {
  const res = await api.post<SendMessageResponse>(
    `/chat/sessions/${sessionId}/messages`,
    data,
  );
  return res.data;
}

export async function getSession(sessionId: string): Promise<Session> {
  const res = await api.get<Session>(`/chat/sessions/${sessionId}`);
  return res.data;
}

// ── Response Mappers (backend camelCase → frontend snake_case) ──

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapSession(s: any): Session {
  return {
    session_id: s.sessionId ?? s.session_id ?? '',
    first_name: s.formContext ? String(tryParse(s.formContext)?.first_name ?? '') : '',
    email: s.formContext ? String(tryParse(s.formContext)?.email ?? '') : '',
    topic_subject: s.formTopicSubject ?? s.topic_subject ?? '',
    ad_id: s.formContext ? String(tryParse(s.formContext)?.ad_id ?? '') : '',
    description: s.formContext ? String(tryParse(s.formContext)?.description ?? '') : '',
    use_case: s.activeUseCase ?? s.use_case ?? '',
    outcome: s.containmentOutcome ?? s.outcome ?? '',
    turns: s.totalBotTurns ?? s.turns ?? 0,
    created_at: s.createdAt ?? s.created_at ?? '',
    updated_at: s.updatedAt ?? s.updated_at ?? '',
    status: mapHandlingState(s.handlingState ?? s.currentPhase ?? s.status ?? ''),
  };
}

function mapHandlingState(state: string): string {
  const map: Record<string, string> = {
    BOT_HANDLING: 'active',
    QUEUE_TO_HUMAN: 'escalated',
    HUMAN_HANDLING: 'escalated',
    CLOSED: 'ended',
    ESCALATE: 'escalated',
    CLOSE: 'ended',
  };
  return map[state] ?? state?.toLowerCase() ?? 'active';
}

function tryParse(json: string | object): Record<string, unknown> {
  if (typeof json === 'object') return json as Record<string, unknown>;
  try { return JSON.parse(json); } catch { return {}; }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapEvent(e: any): SessionEvent {
  return {
    event_id: e.eventId ?? e.event_id ?? '',
    event_type: e.eventType ?? e.event_type ?? '',
    timestamp: e.createdAt ?? e.timestamp ?? '',
    data: typeof e.payload === 'string' ? tryParse(e.payload) : (e.payload ?? e.data ?? {}),
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapHandoverLog(h: any): HandoverLog {
  const payload = typeof h.handoverPayload === 'string' ? tryParse(h.handoverPayload) : (h.handoverPayload ?? h.payload ?? {});
  const rawTranscript = h.transcript ?? h.transcriptJson;
  let transcript: TranscriptEntry[] | undefined;
  if (rawTranscript) {
    try {
      transcript = typeof rawTranscript === 'string' ? JSON.parse(rawTranscript) : rawTranscript;
    } catch { transcript = undefined; }
  }
  return {
    log_id: h.logId ?? h.log_id ?? '',
    session_id: h.sessionId ?? h.session_id ?? '',
    case_number: (payload.case_id as string) ?? h.case_number ?? '',
    priority: h.transferResult ?? h.priority ?? 'normal',
    reason: (payload.escalation_reason as string) ?? h.reason ?? '',
    payload,
    customer_message: h.customerMessage ?? h.customer_message ?? undefined,
    transcript,
    created_at: h.createdAt ?? h.created_at ?? '',
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapFunnel(m: any): FunnelMetrics {
  return {
    total_sessions: m.totalSessions ?? m.total_sessions ?? 0,
    self_served: m.resolvedSessions ?? m.self_served ?? 0,
    escalated: m.escalatedSessions ?? m.escalated ?? 0,
    abandoned: m.abandonedSessions ?? m.abandoned ?? 0,
    avg_turns: m.avg_turns ?? 0,
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapTrace(sessionId: string, turns: any[]): TraceResponse {
  return {
    session_id: sessionId,
    steps: (turns ?? []).map((t: any) => ({
      step: t.turnIndex ?? t.step ?? 0,
      action: t.actionSelected ?? t.action ?? '',
      input: t.userMessage ?? t.input ?? '',
      output: t.botResponse ?? t.output ?? '',
      tool_calls: t.toolCalls ? (typeof t.toolCalls === 'string' ? tryParse(t.toolCalls) : t.toolCalls) : undefined,
      timestamp: t.createdAt ?? t.timestamp ?? '',
    })),
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapCase(c: any): DemoCase {
  return {
    case_id: c.caseId ?? c.case_id ?? '',
    title: c.subject ?? c.title ?? '',
    use_case: c.useCaseId ?? c.use_case ?? '',
    description: c.description ?? '',
    expected_outcome: c.status ?? c.expected_outcome ?? '',
  };
}

// ── Demo / Admin API ──

export async function listSessions(): Promise<Session[]> {
  const res = await api.get('/demo/sessions');
  return (res.data ?? []).map(mapSession);
}

export async function getTrace(sessionId: string): Promise<TraceResponse> {
  const res = await api.get(`/demo/sessions/${sessionId}/trace`);
  return mapTrace(sessionId, res.data ?? []);
}

export async function getEvents(sessionId: string): Promise<SessionEvent[]> {
  const res = await api.get(`/demo/sessions/${sessionId}/events`);
  return (res.data ?? []).map(mapEvent);
}

export async function getHandoverLogs(): Promise<HandoverLog[]> {
  const res = await api.get('/demo/handover-logs');
  return (res.data ?? []).map(mapHandoverLog);
}

export async function getHandoverLogById(id: string): Promise<HandoverLog> {
  const res = await api.get(`/demo/handover-logs/${id}`);
  return mapHandoverLog(res.data);
}

export async function getFunnelMetrics(): Promise<FunnelMetrics> {
  const res = await api.get('/demo/metrics/funnel');
  return mapFunnel(res.data ?? {});
}

export async function getPerUCMetrics(): Promise<PerUCMetric[]> {
  const res = await api.get('/demo/metrics/per-uc');
  // Backend returns Map<String, UcMetrics> — transform to array
  const data = res.data ?? {};
  if (Array.isArray(data)) return data;
  return Object.entries(data).map(([ucId, m]: [string, any]) => ({
    use_case: ucId,
    total: m.totalSessions ?? m.total ?? 0,
    self_served: m.resolved ?? m.self_served ?? 0,
    escalated: m.escalated ?? 0,
    avg_turns: m.avg_turns ?? 0,
    avg_csat: m.avg_csat ?? 0,
  }));
}

export async function getDemoCases(): Promise<DemoCase[]> {
  const res = await api.get('/demo/cases');
  return (res.data ?? []).map(mapCase);
}

export async function updateDemoConfig(
  config: Record<string, unknown>,
): Promise<void> {
  await api.post('/demo/config', config);
}
