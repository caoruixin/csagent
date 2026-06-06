import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import TraceViewer from './TraceViewer';
import * as client from '../../api/client';
import type { Session, ToolCall, TraceResponse, TraceStep } from '../../types';

vi.mock('../../api/client');

function makeSession(over: Partial<Session>): Session {
  return {
    session_id: 's1',
    first_name: 'Alex',
    email: 'alex@example.com',
    topic_subject: 'Ad Support',
    description: 'desc',
    use_case: 'UC-A',
    outcome: '',
    turns: 1,
    created_at: '2026-06-06T00:00:00Z',
    updated_at: '2026-06-06T00:00:00Z',
    status: 'active',
    handling_state: 'BOT_HANDLING',
    escalation_reason: '',
    ...over,
  };
}

// Build a ToolCall, filling the required legacy {tool, args, result} fields
// that the backend mapper always supplies (mapToolCalls in api/client.ts).
function tc(over: Partial<ToolCall>): ToolCall {
  return {
    tool: over.tool_name ?? over.tool ?? '',
    args: over.arguments ?? over.args ?? {},
    result: {},
    ...over,
  };
}

function makeStep(over: Partial<TraceStep>): TraceStep {
  return {
    step: 1,
    action: 'resolve',
    input: 'user message',
    output: 'bot reply',
    timestamp: '2026-06-06T00:00:00Z',
    ...over,
  };
}

function trace(steps: TraceStep[]): TraceResponse {
  return { session_id: 's1', steps };
}

beforeEach(() => {
  vi.clearAllMocks();
  // Default: no extra session metadata unless a test overrides it.
  vi.mocked(client.listSessions).mockResolvedValue([makeSession({})]);
});

async function expandFirstStep() {
  // Steps are collapsed by default; click the step header (action label) to
  // reveal its tool calls. The click bubbles to the header's onToggle.
  const header = await screen.findByText('resolve');
  fireEvent.click(header);
}

describe('TraceViewer #4 — terminal handling_state header', () => {
  it('renders the terminal handling_state + escalation_reason for an escalated session', async () => {
    vi.mocked(client.getTrace).mockResolvedValue(trace([makeStep({})]));
    vi.mocked(client.listSessions).mockResolvedValue([
      makeSession({ handling_state: 'QUEUE_TO_HUMAN', escalation_reason: 'user_requested' }),
    ]);

    render(<TraceViewer sessionId="s1" onBack={() => {}} />);

    const header = await screen.findByTestId('terminal-handling-state');
    expect(header).toHaveTextContent('Queued to Human');
    expect(screen.getByTestId('escalation-reason')).toHaveTextContent('user_requested');
  });

  it('renders CLOSED terminal state without an escalation_reason row', async () => {
    vi.mocked(client.getTrace).mockResolvedValue(trace([makeStep({})]));
    vi.mocked(client.listSessions).mockResolvedValue([
      makeSession({ handling_state: 'CLOSED', escalation_reason: '' }),
    ]);

    render(<TraceViewer sessionId="s1" onBack={() => {}} />);

    const header = await screen.findByTestId('terminal-handling-state');
    expect(header).toHaveTextContent('Closed');
    expect(screen.queryByTestId('escalation-reason')).toBeNull();
  });
});

describe('TraceViewer #2 (R3.b) — A1-dedup ToolEvent folding', () => {
  const dedupStep = makeStep({
    tool_calls: [
      tc({
        tool_name: 'search_knowledge',
        arguments: { q: 'how to renew ad' },
        success: true,
        result_data: { hits: ['kb-1'] },
        step_index: 1,
      }),
      tc({
        tool_name: 'search_knowledge',
        arguments: { q: 'how to renew ad' },
        success: true,
        deduplicated: true,
        original_at_step: 1,
        step_index: 2,
        latency_ms: 0,
      }),
      tc({
        tool_name: 'search_knowledge',
        arguments: { q: 'how to renew ad' },
        success: true,
        deduplicated: true,
        original_at_step: 1,
        step_index: 3,
        latency_ms: 0,
      }),
    ],
  });

  it('folds dedup repeats under the primary by default (collapsed)', async () => {
    vi.mocked(client.getTrace).mockResolvedValue(trace([dedupStep]));
    render(<TraceViewer sessionId="s1" onBack={() => {}} />);
    await expandFirstStep();

    // Only the primary dispatch renders as a tool-call row; the 2 repeats are
    // folded into a single indicator.
    expect(screen.getAllByTestId('tool-call-row')).toHaveLength(1);
    const indicator = screen.getByTestId('dedup-indicator');
    expect(indicator).toHaveTextContent("2 dedup'd repeats");
    // Collapsed by default — the per-repeat audit list is not shown yet.
    expect(screen.queryByTestId('dedup-expanded')).toBeNull();
  });

  it('expands to reveal the full dedup audit (step + original_at_step) on click', async () => {
    vi.mocked(client.getTrace).mockResolvedValue(trace([dedupStep]));
    render(<TraceViewer sessionId="s1" onBack={() => {}} />);
    await expandFirstStep();

    fireEvent.click(screen.getByTestId('dedup-indicator'));

    expect(screen.getByTestId('dedup-expanded')).toBeInTheDocument();
    const rows = screen.getAllByTestId('dedup-repeat-row');
    expect(rows).toHaveLength(2);
    // Audit not lost: each repeat carries its original_at_step reference.
    expect(rows[0]).toHaveTextContent('original_at_step=1');
  });
});

describe('TraceViewer #3 (R3.c) — informational vs blocking rejection badge', () => {
  it('renders a known guard rejection as an informational badge (collapsed)', async () => {
    vi.mocked(client.getTrace).mockResolvedValue(
      trace([
        makeStep({
          tool_calls: [
            tc({
              tool_name: 'record_outcome',
              arguments: { outcome_class: 'resolve' },
              success: false,
              error_message: 'progressive_resolve_record_outcome_premature',
              step_index: 1,
            }),
          ],
        }),
      ]),
    );
    render(<TraceViewer sessionId="s1" onBack={() => {}} />);
    await expandFirstStep();

    expect(screen.getByTestId('informational-guard-badge')).toBeInTheDocument();
    // Not a blocking red error.
    expect(screen.queryByTestId('tool-call-error')).toBeNull();
    expect(screen.getByTestId('tool-call-row')).toHaveAttribute('data-rejection-kind', 'informational');
    // Detail (rejection code + rationale) is collapsed by default.
    expect(screen.queryByTestId('rejection-code')).toBeNull();
  });

  it('expands the informational rejection to reveal code + rationale (audit preserved)', async () => {
    vi.mocked(client.getTrace).mockResolvedValue(
      trace([
        makeStep({
          tool_calls: [
            tc({
              tool_name: 'request_handover',
              arguments: { escalation_reason: 'intake_complete_for_uc_h' },
              success: false,
              error_message: 'intake_required_fields_missing_for_intake_complete',
              step_index: 1,
            }),
          ],
        }),
      ]),
    );
    render(<TraceViewer sessionId="s1" onBack={() => {}} />);
    await expandFirstStep();

    fireEvent.click(screen.getByTestId('informational-guard-badge'));

    expect(screen.getByTestId('rejection-code')).toHaveTextContent(
      'intake_required_fields_missing_for_intake_complete',
    );
    expect(screen.getByTestId('rejection-rationale')).toBeInTheDocument();
  });

  it('renders an UNRECOGNISED rejection code as a blocking error (SAFER default)', async () => {
    vi.mocked(client.getTrace).mockResolvedValue(
      trace([
        makeStep({
          tool_calls: [
            tc({
              tool_name: 'create_case_controlled',
              arguments: {},
              success: false,
              error_message: 'tool_dispatch_failed_http_500',
              step_index: 1,
            }),
          ],
        }),
      ]),
    );
    render(<TraceViewer sessionId="s1" onBack={() => {}} />);
    await expandFirstStep();

    // SAFER default: unknown failure stays a blocking red error, never an
    // informational badge.
    expect(screen.queryByTestId('informational-guard-badge')).toBeNull();
    expect(screen.getByTestId('tool-call-error')).toHaveTextContent('tool_dispatch_failed_http_500');
    expect(screen.getByTestId('tool-call-row')).toHaveAttribute('data-rejection-kind', 'blocking');
  });
});
