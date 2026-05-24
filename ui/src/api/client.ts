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

// Sprint 8.1 follow-up #2 (2026-05-06): widened from 30 s to 60 s to match
// the new server-side wall-clock budget (30 s) plus comfortable headroom.
// During the verification phase we'd rather see the slow tail than auto-
// cancel a request that the server is still working on; the server itself
// renders an honest "Sorry, I'm a bit slow right now" message inside the
// 30 s budget, so axios timing out at 60 s is a true network give-up.
const api = axios.create({
  baseURL: '/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 60_000,
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

// Sprint 9 §O2: normalise backend tool_calls so the trace viewer always
// has both legacy ({tool, args, result}) and new ({tool_name, arguments,
// success, error_message, result_data, result_summary}) shapes available.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function mapToolCalls(raw: unknown): any[] | undefined {
  if (!Array.isArray(raw)) return undefined;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return raw.map((tc: any) => {
    if (tc == null || typeof tc !== 'object') return tc;
    const toolName = tc.tool_name ?? tc.tool ?? '';
    const args = tc.arguments ?? tc.args ?? {};
    // Legacy rows used `result` for the full payload; the new contract
    // uses `result_data` / `result_summary`. Provide both so old rows
    // and new rows render through the same code path.
    const resultLegacy =
      tc.result ??
      tc.result_data ??
      (tc.result_summary != null ? { summary: tc.result_summary } : undefined) ??
      (tc.error_message != null ? { error: tc.error_message } : undefined);
    return {
      ...tc,
      tool: toolName,
      tool_name: toolName,
      args,
      arguments: args,
      result: resultLegacy ?? {},
      result_data: tc.result_data ?? tc.result,
      result_summary: tc.result_summary,
      success: tc.success,
      latency_ms: tc.latency_ms,
      error_message: tc.error_message,
      source: tc.source,
    };
  });
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
      tool_calls: t.toolCalls
        ? mapToolCalls(typeof t.toolCalls === 'string' ? tryParse(t.toolCalls) : t.toolCalls)
        : undefined,
      timestamp: t.createdAt ?? t.timestamp ?? '',
      projected_context: t.projectedContext
        ? (typeof t.projectedContext === 'string' ? tryParse(t.projectedContext) : t.projectedContext)
        : undefined,
      llm_raw_response: t.llmRawResponse ?? t.llm_raw_response ?? undefined,
      action_parameters: t.actionParameters
        ? (typeof t.actionParameters === 'string' ? tryParse(t.actionParameters) : t.actionParameters)
        : undefined,
      source_ids: t.sourceIds ?? t.source_ids ?? undefined,
      phase_before: t.phaseBefore ?? t.phase_before ?? undefined,
      phase_after: t.phaseAfter ?? t.phase_after ?? undefined,
      active_use_case: t.activeUseCase ?? t.active_use_case ?? undefined,
      latency_ms: t.latencyMs ?? t.latency_ms ?? undefined,
      // Sprint 51 / M5 S2 — per-invocation records nested under each turn
      // by the /trace endpoint (BotTurnTrace DTO). Optional; older sessions
      // and tests without S2 data ship without it.
      llm_calls: (t.llmCalls ?? t.llm_calls)
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ? ((t.llmCalls ?? t.llm_calls) as any[]).map((c: any) => ({
            id: c.id,
            bot_turn_id: c.botTurnId ?? c.bot_turn_id,
            step_index: c.stepIndex ?? c.step_index ?? 0,
            call_type: c.callType ?? c.call_type ?? '',
            model: c.model,
            latency_ms: c.latencyMs ?? c.latency_ms,
            llm_raw_response: c.llmRawResponse ?? c.llm_raw_response,
            projected_context: c.projectedContext ?? c.projected_context,
            tool_calls: c.toolCalls ?? c.tool_calls,
            created_at: c.createdAt ?? c.created_at,
          }))
        : undefined,
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
