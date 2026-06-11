# TeamNotify Testing Documentation

This document contains testing information, validation rules, and test case documentation for the TeamNotify plugin.

## Quick Reference

- **Automated Tests:** See `src/test/kotlin/` directory
- **Manual Testing:** See Manual Testing section below
- **URL Validation Rules:** See URL Validation section below

---

## URL Validation Rules

### Slack Webhooks
**Format:** `https://hooks.slack.com/(services|workflows)/{workspace}/{channel}/{token}`

**Examples:**
- ✅ `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX`
- ✅ `https://hooks.slack.com/workflows/T00000000/A00000000/123456789012345678/abcdefg`

### Microsoft Teams Webhooks (4 Formats)
**Format 1 - Classic:** `https://outlook.office.com/webhook/{guid}/IncomingWebhook/{id}/{guid}`

**Format 2 - Standard:** `https://{tenant}.webhook.office.com/webhookb2/{guid}/IncomingWebhook/{id}/{guid}`

**Format 3 - Power Automate (Legacy):** `https://{id}.logic.azure.com/workflows/{id}/triggers/manual/paths/invoke`

**Format 4 - Power Automate (New):** `https://{id}.environment.api.powerplatform.com/powerautomate/automations/direct/workflows/{id}/...`

**Examples:**
- ✅ `https://outlook.office.com/webhook/abc-123/IncomingWebhook/def-456/ghi-789`
- ✅ `https://company.webhook.office.com/webhookb2/abc123/IncomingWebhook/def456/ghi789`
- ✅ `https://prod-27.logic.azure.com/workflows/abc123/triggers/manual/paths/invoke`
- ✅ `https://default123abc.35.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/abc123/triggers/manual/paths/invoke`

### Discord Webhooks
**Format:** `https://discord.com/api/webhooks/{id}/{token}` or `https://discordapp.com/api/webhooks/{id}/{token}`

**Examples:**
- ✅ `https://discord.com/api/webhooks/1234567890/abcdefghijklmnopqrstuvwxyz0123456789`
- ✅ `https://discordapp.com/api/webhooks/9876543210/ABCDEFGHIJKLMNOPQRSTUVWXYZ9876543210`

### Validation Rules
- Maximum URL length: 2048 characters
- Case-insensitive domain matching
- HTTPS required (HTTP rejected)
- Proper path structure required

---

## Manual Testing Checklist

### Webhook Management UI
- [ ] Add Slack webhook via UI
- [ ] Add Teams webhook (classic format) via UI
- [ ] Add Teams webhook (PowerPlatform format) via UI
- [ ] Add Discord webhook via UI
- [ ] Test "Test Connection" button for each platform
- [ ] Edit webhook configuration
- [ ] Delete webhook
- [ ] Toggle webhook enable/disable
- [ ] Add webhook with invalid URL (should show error)
- [ ] Add webhook with very long URL (>2048 chars, should fail)

### Versioned Settings (Kotlin DSL)
- [ ] Add webhook via Kotlin DSL using `buildFeature` in versioned settings
- [ ] Verify webhook appears in TeamCity UI
- [ ] Trigger build and verify notification sent
- [ ] Test parameter references (e.g., `%slack.webhook.url%`)
- [ ] Test branch filters in DSL
- [ ] Test multiple webhooks in one build type
- [ ] Verify DSL webhooks merge with UI webhooks
- [ ] Test all triggers including `onCancel`

### Build Notifications
- [ ] Trigger build start notification
- [ ] Trigger build success notification
- [ ] Trigger build failure notification
- [ ] Trigger first failure notification
- [ ] Trigger build fixed notification
- [ ] Trigger build cancelled notification
- [ ] Trigger long duration notification
- [ ] Verify notification content (project, build number, etc.)
- [ ] Verify links work (build URL, artifacts URL)
- [ ] Verify recent changes appear in notification

### Error Handling
- [ ] Try to save webhook with invalid URL format
- [ ] Try to save webhook to non-existent project
- [ ] Try to save webhook to non-existent build type
- [ ] Verify error messages are clear and helpful
- [ ] Verify no HTML error pages in JSON API responses

---

## Automated Test Coverage

See `src/test/kotlin/` for automated tests:
- `WebhookUrlValidationTest.kt` - URL validation tests
- `WebhookManagerTest.kt` - Webhook CRUD operations tests
- `PayloadGeneratorTest.kt` - Payload generation tests
- `BuildStallTrackerTest.kt` - Build stall detection tests
- `BuildDurationServiceTest.kt` - Build duration tracking tests

Run tests with:
```bash
./gradlew test
```

---

## Known Issues and Limitations

### Microsoft Teams
- Power Automate (Legacy) URLs using `logic.azure.com` will be deprecated on November 30, 2025
- Users should migrate to the new PowerPlatform format

### Branch Filters
- Use TeamCity branch filter syntax: `+:main,-:feature/*`
- Filters apply to the branch name, not VCS root

### URL Security
- Never commit webhook URLs to version control
- Use TeamCity parameters: `%webhook.url.parameter%`
- Use password-type parameters for extra security

---

## Debugging Tips

### Webhook Not Triggering
1. Check TeamCity server logs: `teamcity-server.log`
2. Look for "Dispatching webhook" or "Webhook delivery FAILED"
3. Verify webhook is enabled
4. Check branch filter matches
5. Verify at least one trigger condition is enabled

### "Unexpected token" Errors
- These should no longer occur (fixed in v1.2.0+)
- If they do, check server logs for actual exception
- Verify plugin version is 1.2.0 or higher

### DSL Webhooks Not Loading
1. Verify versioned settings are enabled
2. Check DSL syntax (use `param()` with string values)
3. Verify feature type is exactly `"teamnotify.webhook"`
4. Check TeamCity logs for DSL parsing errors

---

For more information, see:
- **DSL Usage:** `DSL_USAGE.md`
- **Change Log:** `CHANGELOG.md`
- **README:** `README.md`
