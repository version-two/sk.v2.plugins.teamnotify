# Changelog

## [1.2.0] - 2025-09-13

### 🎯 New Features

#### Build Cancelled Trigger Support
- **Added `onCancel` trigger** for notifications when builds are cancelled
- **Platform-specific formatting:**
  - Slack: `:no_entry_sign:` emoji with red color (#dc3545)
  - Teams: `🚫` emoji with red theme color
  - Discord: `🚫` emoji with red embed color (#E74C3C)
- **Full integration across:**
  - Web UI with dedicated checkbox and visual indicator
  - Kotlin DSL support via `onCancel()` lifecycle trigger
  - TeamCity listener for `buildInterrupted` events
  - Proper serialization and persistence

#### Enhanced Notification Titles
- **Detailed context in every notification** with format: `[Project] - [Build Config] - Build #[Number] [Status]`
- **Example:** `cloudweb.sk - Release - Build #73 Cancelled`
- **Consistent emoji indicators** across all platforms:
  - ▶️ Started (blue)
  - ✅ Success (green)
  - ❌ Failed (red)
  - 🚫 Cancelled (red)
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
- **Cleaner notifications** for cancelled, stalled, and in-progress builds

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
- **Type-safe DSL with:**
  - Project and build type extensions
  - Platform-specific helpers (slack, teams, discord)
  - Trigger configuration builders
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
- Build: 83
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