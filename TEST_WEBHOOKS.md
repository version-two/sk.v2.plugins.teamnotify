# Test Webhook URLs

This document contains example (non-working) webhook URLs for testing purposes. These URLs follow the correct format but are not actual working webhooks.

⚠️ **IMPORTANT:** These are example URLs only. Do not use these in production. Generate your own webhooks from the respective platforms.

---

## Slack Webhooks

### Services Format (Standard)
```
https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX
https://hooks.slack.com/services/T00000000/B00000000/EXAMPLE-PLACEHOLDER-TOKEN
https://hooks.slack.com/services/T00000000/B00000000/EXAMPLE-PLACEHOLDER-TOKEN
```

### Workflows Format
```
https://hooks.slack.com/workflows/T00000000/A00000000/123456789012345678/abcdefghijklmnopqrst
https://hooks.slack.com/workflows/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
```

### How to Get Real Slack Webhooks
1. Go to https://api.slack.com/apps
2. Create a new app or select existing
3. Go to "Incoming Webhooks"
4. Activate incoming webhooks
5. Click "Add New Webhook to Workspace"
6. Select channel and authorize
7. Copy the webhook URL

---

## Microsoft Teams Webhooks

### Classic Format (Legacy)
```
https://outlook.office.com/webhook/12345678-1234-1234-1234-123456789012/IncomingWebhook/abcdefgh-ijkl-mnop-qrst-uvwxyz123456/98765432-4321-4321-4321-210987654321
https://outlook.office.com/webhook/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/IncomingWebhook/ffffffff-gggg-hhhh-iiii-jjjjjjjjjjjj/kkkkkkkk-llll-mmmm-nnnn-oooooooooooo
```

### Standard Tenant Format
```
https://mycompany.webhook.office.com/webhookb2/12345678-1234-1234-1234-123456789012@98765432-4321-4321-4321-210987654321/IncomingWebhook/abcdefgh-ijkl-mnop-qrst-uvwxyz123456/fedcba98-7654-3210-fedc-ba9876543210
https://contoso.webhook.office.com/webhookb2/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa@bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/IncomingWebhook/cccccccc-cccc-cccc-cccc-cccccccccccc/dddddddd-dddd-dddd-dddd-dddddddddddd
https://prod-27.webhook.office.com/webhookb2/11111111-2222-3333-4444-555555555555/IncomingWebhook/66666666-7777-8888-9999-000000000000/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
```

### Power Automate (Legacy - Azure Logic Apps)
⚠️ **Dead since Nov 30, 2025:** the `logic.azure.com` host no longer delivers – Microsoft migrated these flows to the `powerplatform.com` host. (Separately, classic Office 365 Connectors retire May 18–22, 2026.) Use a current Power Automate Workflow webhook instead.
```
https://prod-123.logic.azure.com/workflows/abcd1234efgh5678ijkl9012mnop3456/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=AbCdEfGhIjKlMnOpQrStUvWxYz0123456789
https://test-environment.logic.azure.com/workflows/12345678-1234-1234-1234-123456789012/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0
https://westus-prod.logic.azure.com/workflows/aaaabbbbccccddddeeeeffffgggg1111/triggers/manual/paths/invoke
```

### Power Automate (New - PowerPlatform) ⭐ Recommended
```
https://default686ea1d3bc2b4c6fa92cd99c5c3016.35.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/12345678-1234-1234-1234-123456789012/triggers/manual/paths/invoke?api-version=2022-05-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=AbCdEfGhIjKlMnOpQrStUvWxYz
https://prod123abc456def789ghi012jkl345.12.environment.api.powerplatform.com/powerautomate/automations/direct/workflows/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/triggers/manual/paths/invoke?api-version=2022-05-01
https://environment1234567890abcdefghij.99.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/11111111-2222-3333-4444-555555555555/triggers/manual/paths/invoke
```

### How to Get Real Teams Webhooks

#### Method 1: Classic Incoming Webhook (Being Deprecated)
1. Open Microsoft Teams
2. Navigate to the channel
3. Click ••• (More options) → Connectors
4. Search for "Incoming Webhook"
5. Configure and add
6. Provide a name and upload image (optional)
7. Copy the webhook URL

#### Method 2: Power Automate (Recommended)
1. Open Microsoft Teams
2. Navigate to the channel
3. Click ••• (More options) → Workflows
4. Click "Create from blank" or search "post to a channel"
5. Select "Post to a channel when a webhook request is received"
6. Configure the workflow
7. Copy the HTTP POST URL

---

## Discord Webhooks

### Standard Format
```
https://discord.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_-
https://discord.com/api/webhooks/9876543210987654321/aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_-_-_-_-_-_-_-_-_-_-_-_-_-_-
https://discord.com/api/webhooks/1111111111111111111/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
```

### Legacy Format (discordapp.com)
```
https://discordapp.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_-
https://discordapp.com/api/webhooks/5555555555555555555/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
```

### With Query Parameters
```
https://discord.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_-?wait=true
https://discord.com/api/webhooks/9876543210987654321/zyxwvutsrqponmlkjihgfedcba9876543210ZYXWVUTSRQPONMLKJIHGFEDCBA_-?thread_id=1234567890
```

### How to Get Real Discord Webhooks
1. Open Discord
2. Go to Server Settings (click server name)
3. Navigate to "Integrations" → "Webhooks"
4. Click "New Webhook"
5. Give it a name and select channel
6. Click "Copy Webhook URL"

---

## URL Format Reference

### Slack
- **Protocol:** HTTPS (required)
- **Domain:** `hooks.slack.com`
- **Path:** `/services/{workspace}/{channel}/{token}` OR `/workflows/{workspace}/{workflow}/{id}/{token}`
- **Length:** Variable, typically 70-100 characters

### Microsoft Teams
- **Protocol:** HTTPS (required)
- **Domains:**
  - Classic: `outlook.office.com`
  - Tenant: `{tenant}.webhook.office.com`
  - Power Automate Legacy: `{id}.logic.azure.com`
  - Power Automate New: `{id}.environment.api.powerplatform.com`
- **Path:** Varies by format (see examples above)
- **Length:** Variable, typically 100-300 characters

### Discord
- **Protocol:** HTTPS (required)
- **Domain:** `discord.com` or `discordapp.com`
- **Path:** `/api/webhooks/{id}/{token}`
- **Webhook ID:** 18-19 digit number
- **Token:** 68 character alphanumeric string with `_` and `-`
- **Length:** Typically 110-120 characters

---

## Testing Notes

### Valid URL Characteristics
- ✅ Must use HTTPS (HTTP is rejected)
- ✅ Maximum length: 2048 characters
- ✅ Case-insensitive domain matching
- ✅ Query parameters allowed
- ✅ URL encoding supported

### Invalid URL Examples (Will Be Rejected)
```
# HTTP instead of HTTPS
http://hooks.slack.com/services/T00/B00/XXX

# Wrong domain
https://hooks.slack.net/services/T00/B00/XXX

# Wrong path structure
https://discord.com/webhooks/123456/token (missing /api/)

# Incomplete URL
https://hooks.slack.com

# Too long (>2048 characters)
https://hooks.slack.com/services/[... 2050 characters ...]

# Invalid protocol
ftp://hooks.slack.com/services/T00/B00/XXX
javascript:alert('xss')
```

---

## Security Best Practices

### ⚠️ DO NOT:
- ❌ Commit webhook URLs to version control (use parameters instead)
- ❌ Share webhook URLs publicly
- ❌ Use the same webhook for multiple projects/teams
- ❌ Log webhook URLs in plain text

### ✅ DO:
- ✅ Use TeamCity parameters: `%webhook.url.parameter%`
- ✅ Use password-type parameters for sensitive URLs
- ✅ Regenerate webhooks if exposed
- ✅ Use different webhooks for dev/staging/production
- ✅ Test webhooks with "Test Connection" before saving

---

## Example TeamCity Parameter Configuration

### Define Parameters (Recommended Approach)
```
# In TeamCity UI: Parameters tab

Name: slack.webhook.url
Type: Password
Value: https://hooks.slack.com/services/[your-real-webhook]

Name: teams.webhook.url
Type: Password
Value: https://outlook.office.com/webhook/[your-real-webhook]

Name: discord.webhook.url
Type: Password
Value: https://discord.com/api/webhooks/[your-real-webhook]
```

### Use in DSL
```kotlin
buildFeature {
    type = "teamnotify.webhook"
    param("webhook.url", "%slack.webhook.url%")
    param("webhook.platform", "SLACK")
    param("webhook.enabled", "true")
    param("webhook.onFailure", "true")
}
```

### Use in UI
Simply reference the parameter in the webhook URL field:
```
%slack.webhook.url%
```

---

## Additional Resources

- **Slack Webhooks:** https://api.slack.com/messaging/webhooks
- **Teams Webhooks:** https://learn.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/add-incoming-webhook
- **Teams Power Automate:** https://support.microsoft.com/en-us/office/create-incoming-webhooks-with-workflows-for-microsoft-teams-8ae491c7-0394-4861-ba59-055e33f75498
- **Discord Webhooks:** https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks

---

**Last Updated:** 2026-06-11
**Plugin Version:** 1.3.0+
**Compatible TeamCity Versions:** 2026.1+
