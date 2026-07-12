# Continuation of 08 Settings Logs And Operations
Continue from the immediately preceding prompt part.
Preserve all work already completed in this phase.
Do not repeat completed tasks unless required to fix a defect.
## 20. Tests
Add tests for:
- Port validation
- Port restart workflow
- Old port stops
- New port starts
- Restart failure handling
- Secret preserved when blank
- Secret clear requires explicit action
- Auto-start on
- Auto-start off
- Diagnostics excludes secrets
- Diagnostics excludes SMS bodies
- Clear history transactions
- Settings validation
- Log paging
- Audit UI models
- Confirmation dialogs
## 21. Documentation updates
Update README:
- Settings guide
- Port change
- Auto Start
- Battery optimization
- Diagnostics export
- Clearing data
- Security warning
Update API.md for final settings and log endpoints.
## 22. Build verification
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run lint.
Run instrumentation tests where possible.
Fix all build-breaking errors.
## 23. Final response
Report:
- Settings completed
- Port workflow
- Logs and audit screens
- Diagnostics export
- Data deletion safeguards
- Boot reliability
- Tests
- Build result
- Remaining work for Phase 9
