import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import SessionList from './SessionList';
import * as client from '../../api/client';
import type { Session } from '../../types';

vi.mock('../../api/client');

let counter = 0;
function makeSession(over: Partial<Session>): Session {
  counter += 1;
  return {
    session_id: `sess-${counter}`,
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

beforeEach(() => {
  counter = 0;
  vi.clearAllMocks();
});

describe('SessionList StatusBadge — R3.a full handling_state coverage', () => {
  // The complete set of BotSession.handling_state values the backend can
  // persist (SessionManager / ControlKernel / PhaseEvaluator setHandlingState
  // call sites) plus their expected admin labels.
  const KNOWN: { state: string; label: string }[] = [
    { state: 'BOT_HANDLING', label: 'Bot Handling' },
    { state: 'QUEUE_TO_HUMAN', label: 'Queued to Human' },
    { state: 'HUMAN_HANDLING', label: 'Human Handling' },
    { state: 'CLOSED', label: 'Closed' },
  ];

  it('renders every handling_state value with a distinct label + colour', async () => {
    vi.mocked(client.listSessions).mockResolvedValue(
      KNOWN.map((k) => makeSession({ handling_state: k.state })),
    );

    render(<SessionList onSelectSession={() => {}} />);

    const badges = await screen.findAllByTestId('status-badge');
    // Every session is rendered — no UI-level filtering by handling_state.
    expect(badges).toHaveLength(KNOWN.length);

    const byState = new Map(
      badges.map((b) => [b.getAttribute('data-handling-state'), b]),
    );
    for (const { state, label } of KNOWN) {
      const badge = byState.get(state);
      expect(badge, `badge for ${state} should render`).toBeTruthy();
      expect(badge!).toHaveTextContent(label);
    }

    // Distinct visual treatment: each known state resolves to a unique
    // background colour (no two states collapse to the same swatch).
    const backgrounds = new Set(
      KNOWN.map(({ state }) => byState.get(state)!.style.background),
    );
    expect(backgrounds.size).toBe(KNOWN.length);
  });

  it('renders an unrecognised handling_state safely (raw text, never hidden)', async () => {
    vi.mocked(client.listSessions).mockResolvedValue([
      makeSession({ handling_state: 'SOME_FUTURE_STATE' }),
    ]);

    render(<SessionList onSelectSession={() => {}} />);

    const badge = await screen.findByTestId('status-badge');
    expect(badge).toHaveAttribute('data-handling-state', 'SOME_FUTURE_STATE');
    // Falls back to the raw value as visible text rather than dropping the row.
    expect(badge).toHaveTextContent('SOME_FUTURE_STATE');
  });

  it('does not filter ESCALATE-terminated (QUEUE_TO_HUMAN) sessions out of the list', async () => {
    vi.mocked(client.listSessions).mockResolvedValue([
      makeSession({ session_id: 'bot-1', handling_state: 'BOT_HANDLING' }),
      makeSession({ session_id: 'esc-1', handling_state: 'QUEUE_TO_HUMAN' }),
      makeSession({ session_id: 'closed-1', handling_state: 'CLOSED' }),
    ]);

    render(<SessionList onSelectSession={() => {}} />);

    const badges = await screen.findAllByTestId('status-badge');
    expect(badges).toHaveLength(3);
    const states = badges.map((b) => b.getAttribute('data-handling-state'));
    expect(states).toContain('QUEUE_TO_HUMAN');
  });
});
