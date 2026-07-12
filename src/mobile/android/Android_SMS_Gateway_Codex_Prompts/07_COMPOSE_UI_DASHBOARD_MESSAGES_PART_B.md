# Continuation of 07 Compose Ui Dashboard Messages
Continue from the immediately preceding prompt part.
Preserve all work already completed in this phase.
Do not repeat completed tasks unless required to fix a defect.
## 16. Formatting
Create reusable formatters:
- ISO/API timestamp
- Local display timestamp
- Relative time
- Phone masking
- URL preview
- Message preview
- Duration
Use locale-safe formatting.
Keep stored timestamps in UTC.
## 17. Accessibility
Add:
- Content descriptions
- Minimum touch targets
- Clear focus order
- Screen reader-friendly status text
- Do not rely on color alone
- Dynamic font support
- Proper button labels
## 18. State recovery
UI must survive:
- Rotation
- Process recreation
- Temporary database delay
- Service state change
- Permission change
- Network change
Use ViewModels and saved state appropriately.
## 19. Tests
Add Compose/UI tests for:
- Dashboard running state
- Dashboard stopped state
- Degraded permission state
- Stop confirmation
- API key regeneration confirmation
- Sent list empty state
- Received list empty state
- Status filters
- Message detail rendering
- Manual retry disabled while active
- Test SMS validation
- Dark theme smoke test
Use fake repositories.
## 20. Build verification
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run UI or instrumentation tests where possible.
Run lint if practical.
Fix build-breaking errors.
## 21. Final response
Report:
- Screens completed
- Navigation
- UI state architecture
- Permission flow
- API key flow
- Paging
- Tests
- Build result
- Remaining work for Phase 8
