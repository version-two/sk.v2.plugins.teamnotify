# TeamCity Notifier Plugin Implementation Plan

## Phase 1: Core Functionality (Complete)

- [x] Project Scaffolding
- [x] Basic Build Event Listener
- [x] Webhook Configuration Storage (Save/Load)
- [x] Basic Notification on Build Finish (Success/Failure)
- [x] Implement "On Stall" condition
- [x] Implement "Build longer than X" condition
- [x] Implement "Build longer than average" condition
- [x] Improve Settings UI (Edit/Delete webhooks)

## Phase 2: Multi-Platform Support (Complete)

- [x] Abstract payload generation
- [x] Implement Slack payload format
- [x] Implement Microsoft Teams payload format
- [x] Implement Discord payload format
- [x] Update Settings UI to select platform per webhook

## Phase 3: Refinement and Polish (In Progress)

- [x] Add more build events (e.g., On First Failure, On Build Fixed)
- [x] Comprehensive Testing (WebhookManager, BuildDurationService, BuildStallTracker)
- [x] Improve UI/UX (Redirect after form submission, Success messages)
- [x] Documentation

### Build Fixes (In Progress)

- [x] Refactor `WebhookProjectFeature` to `WebhookConfiguration` (data class).
- [x] Implement `WebhookProjectFeaturesProvider` correctly.
- [x] Adjust `WebhookManager` to use `PluginDataStorage`.
- [ ] Update `spring-plugin.xml` for new class names and dependencies.
- [ ] Resolve `NotifierSettingsController.kt`: `Too many arguments for getWebhooks()` and `saveWebhooks()`
- [ ] Resolve `NotifierSettingsController.kt`: `Type mismatch: inferred type is String! but List<WebhookConfiguration> was expected`
- [ ] Resolve `NotifierBuildServerListener.kt`: `Too many arguments for getWebhooks()`
- [ ] Resolve `NotifierBuildServerListener.kt`: `Type mismatch: inferred type is String but Boolean was expected` (line 54)
- [ ] Resolve `StalledBuildTimer.kt`: `Too many arguments for getWebhooks()`
- [ ] Resolve Gradle deprecation warnings.