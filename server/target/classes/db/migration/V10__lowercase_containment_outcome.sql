-- Wave C2 fix: align bot_sessions.containment_outcome with the trace
-- contract enum {resolved, escalated, abandoned, timeout}. Earlier code
-- wrote the values in uppercase, so any sessions persisted before the
-- code change need to be lower-cased once.
UPDATE bot_sessions
SET containment_outcome = LOWER(containment_outcome)
WHERE containment_outcome IN ('ESCALATED', 'RESOLVED', 'ABANDONED', 'TIMEOUT');
