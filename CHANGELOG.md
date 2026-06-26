# Changelog

## [Unreleased]

## [1.4.0] - 2026-06-26

### 🔧 Tooling
- **Added GitHub Actions CI** – `build.yml` builds and tests on JDK 21 and uploads the plugin zip; `release.yml` publishes a GitHub Release with the zip when a `v*` tag is pushed.
- Updated docs/examples to TeamCity 2026.1 (current Kotlin DSL imports, removed the obsolete `v2019_2` examples and the long-removed custom `teamNotifyWebhook {}` DSL example), and corrected stale storage/version/retirement notes.

### 🔒 Security & robustness
- **Branch filters now follow TeamCity's last-match-wins semantics** – an exclude rule after a broad include (e.g. `+:*,-:feature/*`) now correctly suppresses the excluded branches.
- **Teams "Recent Changes" now render line breaks** instead of literal `\n` text.
- **The admin overview tab is hidden from non-administrators** (it lists every project's webhooks), and reading per-project webhook config now requires `VIEW_PROJECT`.
- **Webhook delivery no longer blocks TeamCity's build-event thread** – notifications are dispatched on a bounded background executor, so a slow or unreachable webhook can no longer stall build processing.
- **Admin backup/restore and the admin overview now require server-administrator permission**; the backup endpoint previously exposed every webhook URL and auth token to any authenticated user.
- **Per-project webhook editing now requires `EDIT_PROJECT` permission**, and the test-webhook endpoint requires an authenticated user.
- **Artifact links are now real** – the notification artifact buttons enumerate actual build artifacts via the TeamCity API instead of emitting guessed, wildcard URLs that didn't resolve.
- Exception messages in JSON responses are now properly escaped (previously could produce malformed JSON).
- Build-average duration now samples only recent history instead of materializing the entire build history, and excludes personal and canceled builds.
- First-failure/fixed detection ignores personal and canceled builds when finding the previous build.
- **First-failure/fixed detection is now branch-aware** – the previous build is matched within the same branch, so a green `main` build can no longer be reported as "fixing" a red feature-branch build (or vice versa).
- **Shared per-project settings are now accessed under a lock** – the build-event thread, stall timer, and UI/REST threads all read and mutate the same settings collections; every read-modify-write (and surrounding `persist()`) is now serialized to prevent races and `ConcurrentModificationException`.
- **Hardened the settings controller against open redirects** – `returnUrl`/`pageUrl`/`Referer` values are now restricted to same-origin relative paths, so an attacker-supplied absolute or protocol-relative URL can no longer bounce the user to another origin.
- **Toast notifications render server/error messages as text, not HTML** – removes a DOM-based XSS sink in both the per-project and admin pages.
- The `branchFilter` value returned to the UI is now fully JSON-escaped (previously only quotes were escaped).
- **Recent-changes text is no longer double-escaped** – commit messages and committer names containing quotes, backslashes, or newlines were escaped twice in all three (Slack/Teams/Discord) generators and rendered with visible `\"` sequences; they are now escaped exactly once.
- Slack and Discord payloads now also escape carriage-return and tab characters, so a commit message containing them can no longer produce invalid JSON.
- **A saturated webhook queue can no longer run delivery on the build-event thread** – the executor now drops (and logs) the rejected notification instead of using a caller-runs policy, fully honoring the "never blocks build processing" guarantee.
- The stalled-build timer thread is now a daemon and one failing stall notification no longer aborts the rest of that check pass.
- **DSL webhook parsing is more robust** – blank URLs are rejected, the platform value is matched case-insensitively, and a non-positive `buildLongerThan` is ignored.
- Removed a duplicate `gson` dependency declaration in the build script.
- **A single build no longer fires duplicate notifications** – when both `onFailure` and `onFirstFailure` (or `onSuccess` and `onBuildFixed`) are enabled, only the more specific "First failure"/"Build fixed" message is sent. The previous-build lookup is now done once per build instead of once per webhook.
- **The Slack "Test" button now sends the real `attachments` payload** instead of a bare `{"text": …}` message, so a passing test matches actual delivery.
- **The plugin descriptor now pins a minimum TeamCity build (2026.1 / 222521)** so installing on an older server fails with a clear message instead of a class-version error.
- Corrected the Teams/Office 365 Connector retirement date in the docs to the actual cutoff (May 18–22, 2026) and flagged the legacy `logic.azure.com` host as non-delivering since November 30, 2025.
- **Slack "View Build" and artifact links now actually render** – they were emitted as legacy attachment action buttons, which Slack silently drops for incoming webhooks, so Slack users got no links at all. The Slack message is now built with Block Kit blocks (kept inside a colored attachment) and the links are real Block Kit URL buttons.
- **Fixed Slack webhook URL validation** – it accepted a bogus `/workflows/` path that never existed and rejected the real Workflow Builder trigger format `hooks.slack.com/triggers/...`. Now accepts `/services/` (incoming webhooks) and `/triggers/` (Workflow Builder).
- **The project TeamNotify tab now shows DSL (versioned-settings) and inherited parent-project webhooks** (read-only, with source badges), so it reflects everything that actually fires – previously it listed only the project's own UI webhooks, while the build-configuration tab already showed all sources.
- **The DSL webhook build feature no longer points at a non-existent edit page** (`editWebhookFeature.jsp`); since DSL webhooks are configured in `.teamcity/settings.kts`, the feature now exposes its raw parameters instead of a broken editor.

### 🐛 Bug Fixes (admin & UI)
- **Stalled-build notifications now honor the enabled flag, locally-disabled state, and branch filter**, consistent with the other triggers.
- **Build-configuration webhooks now appear in the admin overview** and can be deleted/toggled there (`getAllWebhooks` previously returned only project-level webhooks).
- **"On First Failure" and "On Fixed" can now be selected when creating a webhook in the UI**, and "On Cancel" is included in trigger validation and the form reset.

### ⬆️ Compatibility
- **Targets TeamCity 2026.1** (`server-api`/`common-api` bumped from 2025.07). TeamCity 2026.1 requires **Java 21**, so the plugin now builds and targets JVM 21 via a JDK 21 Gradle toolchain (auto-provisioned if not installed).
- **Added a Java 21 Docker build** (`docker build --target artifact --output type=local,dest=out .`) for building without a local JDK 21.

### 🐛 Bug Fixes

#### Build-level webhooks no longer fail to save
- **Fixed "Unable to create settings for team-notify.settings.bt…; corresponding factory was not registered"** when adding a webhook to a build configuration (project-level webhooks were unaffected).
- Root cause: build-level webhooks registered a new settings factory per build type at request time, which was unreliable. All TeamNotify data for a project now lives in a single settings object whose factory is registered once at startup – no dynamic per-build-type registration.

#### Microsoft Teams / Power Automate
- **Teams notifications are now color-coded by status** (the status color was previously computed but never applied to the Adaptive Card).
- **"Test" now sends the real Adaptive Card envelope for Teams** instead of the legacy `{"text": …}` MessageCard, so a successful test matches real delivery – including Power Automate Workflow webhooks.
- **Fixed validation of new Power Automate URLs** (`{org}.{region}.environment.api.powerplatform.com`) which were previously rejected.
- URLs containing raw whitespace are now rejected.

#### Deterministic stalled-build tests
- `BuildStallTracker` now uses an overridable time source so stall detection can be tested deterministically instead of depending on real elapsed wall-clock time (the previous test was flaky in faster environments). Production behavior is unchanged.

### 📋 Technical Details
- `TeamNotifyProjectSettings.kt`: now holds project webhooks plus per-build-type webhooks and per-build-type locally-disabled URLs in one object; removed `DisabledWebhooksSettings`.
- `WebhookManager.kt`: removed `ensureSettingsRegistered()`/dynamic key registration; all access goes through the single `team-notify.settings` key.
- `TeamsPayloadGenerator.kt`: maps status to an Adaptive Card `color`.
- `WebhookService.kt`: Teams test payload uses the Adaptive Card envelope.
- `NotifierSettingsController.kt`: Power Automate host regex allows multi-label subdomains; whitespace rejected.
- `build.gradle.kts`: test suite now runs under the JUnit Platform with both the Jupiter and Vintage engines (previously no tests executed); TeamCity API bumped to 2026.1; JDK 21 toolchain; added the `download.jetbrains.com/teamcity-repository` mirror.
- `settings.gradle.kts`: added the foojay toolchain resolver so JDK 21 auto-provisions.

## [1.3.0] - 2026-01-22

### 🎯 New Features

#### Changelog in All Notifications
- **Changes now shown in all notification types** - Previously changes were only shown in "Build Started" notifications
- **Consistent change display** across Success, Failure, Fixed, First Failure, and other notification types
- **Better build context** - See what changed in the build that succeeded/failed

#### Configurable Notification Sections
- **Added "Show Build Link" option** - Toggle visibility of "Open in TeamCity" link in notifications
- **Added "Show Artifacts" option** - Toggle visibility of artifact download links in notifications
- **Per-webhook configuration** - Each webhook can have different display settings
- **UI tags** - "No Link" and "No Artifacts" tags displayed on webhooks with these options disabled

### 🐛 Bug Fixes

#### Fixed Build-Type Specific Webhook Registration
- **Fixed "factory was not registered" error** when adding webhooks via UI for build configurations
- **Implemented dynamic settings registration** - Settings keys are now registered on-demand
- **Removed wildcard pattern registration** - TeamCity doesn't support wildcards in `registerSettingsFactory`

### 📋 Technical Details
- `WebhookConfiguration.kt`: Added `showBuildLink` and `showArtifacts` boolean options
- `TeamNotifyProjectSettings.kt`: Added persistence for new options
- `NotificationModels.kt`: Added display options to NotificationContext
- `DiscordPayloadGenerator.kt`, `SlackPayloadGenerator.kt`, `TeamsPayloadGenerator.kt`: Updated to use new options and show changes for all statuses
- `WebhookManager.kt`: Implemented `ensureSettingsRegistered()` for dynamic registration
- `TeamNotifySettingsRegistrar.kt`: Simplified to only register base settings key
- `NotifierSettingsController.kt`: Added handling for new parameters
- `editNotifierSettings.jsp`: Added UI checkboxes and display tags

### 🔢 Version Info
- Version: 1.3.0
- API Compatibility: TeamCity 2025.07+
- Release Date: January 22, 2026

---

## [1.2.2] - 2025-12-16

### 🐛 Bug Fixes

#### Fixed Webhook Enable/Disable and Delete Operations
- **Fixed "Invalid webhook index" error** when deleting or toggling webhooks
- **Fixed JavaScript syntax error** (`expected expression, got ','`) when clicking enable/disable buttons
- **Added missing `varStatus` to forEach loops** - JSP iteration now properly tracks index for webhook operations
- **Fixed index mismatch for build configurations** - Build config pages show inherited + local webhooks, but operations were using wrong index source
- **Properly handle inherited vs local webhooks** - Delete/toggle operations now correctly distinguish between:
  - Build-type specific webhooks (can be deleted/toggled directly)
  - Inherited webhooks from parent projects (use "local disable" toggle instead)
- **Fixed enum comparison** - Controller now properly compares `WebhookSource.BUILD_TYPE` enum instead of string

### 📋 Technical Details
- `NotifierSettingsController.kt`: Refactored delete/toggle handlers to use `webhooksWithSource` for build configurations
- `editNotifierSettings.jsp`: Added `varStatus="status"` to both `forEach` loops (lines 308, 467)
- Improved error messages for inherited webhook operations

### 🔢 Version Info
- Version: 1.2.2
- API Compatibility: TeamCity 2025.07+
- Release Date: December 16, 2025

---

## [1.2.1] - 2025-11-12

### 🔒 Critical Security Fixes

#### Webhook URL Exposure Vulnerability Patched
- **Removed webhook URLs from HTML attributes** - No more sensitive URLs in `data-webhook-url` attributes
- **Implemented index-based webhook references** - All client-side operations now use secure array indices
- **Masked webhook URLs in UI** - URLs display as "Platform Webhook (********)" instead of actual URLs
- **Secured admin page** - Admin interface no longer exposes webhook URLs in HTML
- **Protected sensitive tokens** - Webhook authentication tokens are no longer accessible from browser inspector

**Impact:** This prevents webhook URLs from being exposed to anyone with browser access to your TeamCity server. Upgrade recommended immediately.

### 📝 Documentation & Configuration

#### Simplified DSL Configuration
- **Streamlined versioned settings approach** - DSL configuration now uses TeamCity's standard `buildFeature` system exclusively
- **Removed custom DSL extensions** - No longer requires custom imports like `import sk.v2.plugins.teamnotify.dsl.*`
- **Standardized syntax** - All webhook configuration uses consistent `buildFeature` blocks with `param()` calls
- **Completely rewritten documentation:**
  - README.md updated with correct DSL examples
  - DSL_USAGE.md fully rewritten to reflect simplified approach
  - TESTING.md updated with accurate test file references
  - Removed all references to non-existent fluent DSL API
- **Backward compatible** - Existing versioned settings configurations continue to work without changes

**Migration Note:** The standard `buildFeature` approach (using `type = "teamnotify.webhook"`) has been the recommended method since 1.1.1 and remains the only supported DSL configuration method.

#### Enhanced Build System
- **Production build flag added** - Use `-Prelease` flag to create production builds without SNAPSHOT suffix
  - Development builds: `./gradlew serverPlugin` → `team-notify-1.2.1+153-SNAPSHOT.zip`
  - Production builds: `./gradlew serverPlugin -Prelease` → `team-notify-1.2.1+153.zip`
- **Auto-incrementing build numbers** - Build number automatically increments with each build

### 🎨 UI/UX Improvements

#### TeamNotify Admin Menu Relocated
- **Moved to Integrations section** - Admin menu item now appears under "Integrations" instead of "Server-related"
- **Better organization** - More logical placement alongside other third-party integrations

#### Character Encoding Fixes
- **Fixed Unicode display issues** - Replaced problematic Unicode characters with ASCII equivalents:
  - Arrow characters (→) replaced with `>`
  - Bullet points (•) replaced with `*`
  - Multiplication sign (×) replaced with HTML entity `&times;`
- **Improved cross-platform compatibility** - Help text now displays correctly on all systems
- **Fixed garbled characters** in webhook setup instructions

### 🐛 Bug Fixes

#### Admin Page Improvements
- **Fixed CSRF token for restore backup** - Backup restore function now properly authenticated
- **Added "On Cancel" trigger display** - Cancel trigger now visible in global webhook overview
- **Fixed webhook data persistence** - Webhook URLs properly stored server-side only

### 📋 Technical Details
- Refactored webhook identification system from URL-based to index-based
- Enhanced security by removing all client-side URL exposure
- Improved character encoding for better international support
- Fixed CSRF token handling in admin operations
- Simplified DSL implementation to use TeamCity's native build feature system
- Updated project structure documentation to reflect removed DSL package

### 🔢 Version Info
- Version: 1.2.1
- Build: 153
- API Compatibility: TeamCity 2025.07+
- Release Date: November 12, 2025

---

## [1.2.0] - 2025-09-13

### 🎯 New Features

#### Build Canceled Trigger Support
- **Added `onCancel` trigger** for notifications when builds are canceled
- **Full integration across:**
  - Web UI with dedicated checkbox and visual indicator
  - Kotlin DSL support via `webhook.onCancel` parameter
  - TeamCity listener for `buildInterrupted` events
  - Proper serialization and persistence

#### Enhanced Notification Titles
- **Detailed context in every notification** with format: `[Project] - [Build Config] - Build #[Number] [Status]`
- **Example:** `cloudweb.sk - Release - Build #73 Canceled`
- **Consistent emoji and color indicators** across all platforms:
  - ▶️ Started (blue)
  - ✅ Success (green)
  - ❌ Failed (red)
  - 🚫 Canceled (red)
  - ⚠️ Stalled (orange)
  - 🎉 Fixed (purple)
- **Makes notifications instantly identifiable** without opening TeamCity

#### Improved Notification Content
- **Individual artifact download links** - Shows actual artifact filenames as clickable download links
  - Discord: Shows up to 5 artifact links inline
  - Slack: Shows up to 3 artifact buttons (with "More..." if needed)
  - Teams: Shows up to 3 artifact action buttons
- **Smart artifact display** - Only shown for completed builds (success, failure, fixed)
- **Fallback to artifact browser** when individual artifacts can't be determined
- **Changes only in "Build Started"** notifications - reduces noise in other notifications
- **Cleaner notifications** for canceled, stalled, and in-progress builds

---

## [1.1.1] - Previous Release

### 🎯 Major Features

#### Branch Filtering Support (Commit 04098d6)
- **Added branch filter patterns** to control which branches trigger notifications
- **Pattern syntax supports:**
  - Wildcards (`*` for any sequence, `?` for single character)
  - Include/exclude patterns with `+`/`-` prefixes
  - Comma-separated multiple patterns
- **Examples:**
  - `main,develop` - only main and develop branches
  - `release/*` - all release branches
  - `+:*,-:feature/*` - all branches except feature branches
- **Full integration across:**
  - Web UI with validation
  - Kotlin DSL for versioned settings
  - All webhook platforms (Slack, Teams, Discord)

#### Toggle Change Inclusion (Commit 1eb9406)
- **Added `includeChanges` option** to control whether recent commits are included in notifications
- **Benefits:**
  - Reduce notification size for builds with many changes
  - Focus on build status rather than change details
  - Configurable per webhook
- **Available in:**
  - Web UI checkbox
  - Kotlin DSL configuration
  - API endpoints

#### Versioned Settings Support (Commit 48e68df)
- **Full Kotlin DSL support** for TeamCity versioned settings
- **Configure webhooks as code** in `.teamcity/settings.kts`
- **Using TeamCity's standard buildFeature system:**
  - No custom imports required
  - Standard `param()` configuration syntax
  - Full parameter reference support
- **Comprehensive documentation:**
  - DSL usage guide with examples
  - Security best practices
  - Parameter references for secure webhook URLs

### 🔧 Improvements

#### Enhanced Payload Formatting
- **Slack:** Rich attachments with colors, fields, and action buttons
- **Teams:** Adaptive Cards with structured fact sets and buttons
- **Discord:** Embeds with status indicators and formatted fields
- **All platforms:** Consistent emoji indicators for build status

#### UI/UX Enhancements
- Modern, responsive design with gradient headers
- Visual platform selector with icons
- Improved form validation and error messages
- Read-only mode for DSL-defined webhooks
- AJAX operations eliminate page refreshes

#### Backend Improvements
- Unified webhook handling for projects and build configurations
- Webhook inheritance from parent projects
- Memory leak fixes (proper listener cleanup)
- XSS vulnerability fixes
- Concurrent modification fixes
- Performance optimizations

### 📚 Documentation
- Comprehensive DSL usage guide (`DSL_USAGE.md`)
- Security best practices for webhook URLs
- Example TeamCity settings file
- Updated README with versioned settings examples

### 🔢 Version Info
- Version: 1.1.1
- Build: (historical)
- API Compatibility: TeamCity 2025.07+

### 🐛 Bug Fixes
- Fixed Discord webhook URL template variable replacement
- Fixed CSS/page rendering issues after webhook creation
- Fixed parent project webhooks not triggering for sub-project builds
- Fixed stalled build detection using wrong webhook source
- Fixed concurrent modification in BuildStallTracker
- Fixed XSS vulnerabilities in webhook URL display

### 🔒 Security
- Added documentation about secure webhook URL storage
- Emphasized using TeamCity parameters instead of hardcoding URLs
- Added proper HTML escaping for all user inputs
- Validation for webhook URL formats per platform
