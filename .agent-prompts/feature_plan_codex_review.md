Review Claude's RFC/implementation plan for the feature/module change.

Review criteria:
1. Does it use the verified Context Pack correctly?
2. Does it distinguish current runtime truth from proposal/future docs?
3. Does it include required docs updates if code will move ahead of docs?
4. Does it avoid editing sprint archives?
5. Does it include tests and rollback?
6. Does it avoid broad rewrites?
7. Does it identify open decisions honestly?
8. Does it respect runtime boundaries:
   - ControlKernel deterministic lifecycle
   - PhaseEvaluator planning/interpreting
   - AgentRunLoop mechanical execution
   - ContextProjectionBuilder projection only
   - ToolDispatcher enforcement

Return:
- PASS or BLOCK
- required plan changes
- risky assumptions
- missing tests
- missing docs updates
