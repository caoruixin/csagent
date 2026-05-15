interface Props {
  caseNumber: string;
}

export default function EscalationNotice({ caseNumber }: Props) {
  return (
    <div
      style={{
        padding: '12px 14px',
        background: '#FFF8E1',
        border: '1px solid #FFD54F',
        borderRadius: 'var(--radius)',
        fontSize: '0.85rem',
      }}
    >
      <div style={{ fontWeight: 600, marginBottom: 4, color: '#E65100' }}>
        Escalated to Support
      </div>
      <div style={{ color: 'var(--color-text-secondary)' }}>
        Your issue has been escalated to a support agent. Your case number is{' '}
        <strong>{caseNumber}</strong>. A team member will reach out to you shortly.
      </div>
    </div>
  );
}
