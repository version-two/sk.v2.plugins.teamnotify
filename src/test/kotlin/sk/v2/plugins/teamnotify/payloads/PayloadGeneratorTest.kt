package sk.v2.plugins.teamnotify.payloads

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import jetbrains.buildServer.serverSide.SRunningBuild
import java.util.*
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PayloadGeneratorTest {

    private val slackGenerator = SlackPayloadGenerator()
    private val teamsGenerator = TeamsPayloadGenerator()
    private val discordGenerator = DiscordPayloadGenerator()

    private fun createMockBuild(): SRunningBuild {
        val build = mock<SRunningBuild>()
        whenever(build.buildNumber).thenReturn("123")
        whenever(build.buildId).thenReturn(456L)
        whenever(build.startDate).thenReturn(Date())
        whenever(build.finishDate).thenReturn(Date())
        whenever(build.agentName).thenReturn("agent-1")
        return build
    }

    private fun createContext(
        status: NotificationStatus = NotificationStatus.SUCCESS,
        message: String = "Build completed successfully",
        projectName: String? = "MyProject",
        buildTypeName: String? = "MyBuildType",
        buildNumber: String? = "123",
        triggeredBy: String? = "John Doe",
        changes: List<ChangeSummary> = emptyList(),
        artifacts: List<ArtifactSummary> = emptyList()
    ): NotificationContext {
        val build = createMockBuild()
        return NotificationContext(
            status = status,
            build = build,
            title = "",
            message = message,
            rootUrl = "https://teamcity.example.com",
            buildUrl = "https://teamcity.example.com/viewLog.html?buildId=456",
            artifactsUrl = "https://teamcity.example.com/viewLog.html?buildId=456&tab=artifacts",
            projectName = projectName,
            buildTypeName = buildTypeName,
            buildExternalId = "MyBuildType_Id",
            buildNumber = buildNumber,
            triggeredBy = triggeredBy,
            agentName = "agent-1",
            startTime = Date(),
            finishTime = Date(),
            changes = changes,
            artifacts = artifacts
        )
    }

    // ========== SLACK TESTS ==========

    @Test
    fun `Slack payload should contain required fields`() {
        val ctx = createContext()
        val payload = slackGenerator.generatePayload(ctx)

        assertTrue(payload.isNotEmpty())
        assertContains(payload, "\"attachments\"")
        assertContains(payload, "MyProject")
        assertContains(payload, "MyBuildType")
        assertContains(payload, "Build #123")
    }

    @Test
    fun `Slack payload should use correct color for success`() {
        val ctx = createContext(status = NotificationStatus.SUCCESS)
        val payload = slackGenerator.generatePayload(ctx)

        assertContains(payload, "#2eb886") // green
    }

    @Test
    fun `Slack payload should use correct color for failure`() {
        val ctx = createContext(status = NotificationStatus.FAILURE)
        val payload = slackGenerator.generatePayload(ctx)

        assertContains(payload, "#dc3545") // red
    }

    @Test
    fun `Slack payload should use correct color for started`() {
        val ctx = createContext(status = NotificationStatus.STARTED)
        val payload = slackGenerator.generatePayload(ctx)

        assertContains(payload, "#0088cc") // blue
    }

    @Test
    fun `Slack payload should escape special characters`() {
        val ctx = createContext(
            message = "Build \"failed\" with error:\nLine 1\tTabbed"
        )
        val payload = slackGenerator.generatePayload(ctx)

        assertContains(payload, "\\\"failed\\\"")
        assertContains(payload, "\\n")
    }

    @Test
    fun `Slack payload should include changes for started builds`() {
        val changes = listOf(
            ChangeSummary(version = "abc123", user = "John Doe", comment = "Fix bug"),
            ChangeSummary(version = "def456", user = "Jane Smith", comment = "Add feature")
        )
        val ctx = createContext(status = NotificationStatus.STARTED, changes = changes)
        val payload = slackGenerator.generatePayload(ctx)

        assertContains(payload, "John Doe")
        assertContains(payload, "Fix bug")
        assertContains(payload, "abc123")
    }

    @Test
    fun `Slack payload should include changes for completed builds`() {
        val changes = listOf(
            ChangeSummary(version = "abc123", user = "John Doe", comment = "Fix bug")
        )
        val ctx = createContext(status = NotificationStatus.SUCCESS, changes = changes)
        val payload = slackGenerator.generatePayload(ctx)

        // Changes are shown for all statuses (knowing what changed in a failed/successful build is useful)
        assertContains(payload, "Recent Changes")
        assertContains(payload, "John Doe")
    }

    @Test
    fun `Slack payload should include View Build action`() {
        val ctx = createContext()
        val payload = slackGenerator.generatePayload(ctx)

        assertContains(payload, "\"actions\"")
        assertContains(payload, "View Build")
        assertContains(payload, "viewLog.html?buildId=456")
    }

    @Test
    fun `Slack payload should handle null values gracefully`() {
        val ctx = createContext(
            projectName = null,
            buildTypeName = null,
            triggeredBy = null
        )
        val payload = slackGenerator.generatePayload(ctx)

        assertTrue(payload.isNotEmpty())
        assertContains(payload, "Unknown Project")
        assertContains(payload, "Unknown Config")
    }

    @Test
    fun `Slack payload should truncate long commit messages`() {
        val longMessage = "A".repeat(150)
        val changes = listOf(
            ChangeSummary(version = "abc123", user = "John", comment = longMessage)
        )
        val ctx = createContext(status = NotificationStatus.STARTED, changes = changes)
        val payload = slackGenerator.generatePayload(ctx)

        assertContains(payload, "…") // Ellipsis indicating truncation
    }

    // ========== TEAMS TESTS ==========

    @Test
    fun `Teams payload should contain required Adaptive Card fields`() {
        val ctx = createContext()
        val payload = teamsGenerator.generatePayload(ctx)

        assertTrue(payload.isNotEmpty())
        assertContains(payload, "\"type\": \"message\"")
        assertContains(payload, "\"contentType\": \"application/vnd.microsoft.card.adaptive\"")
        assertContains(payload, "\"type\": \"AdaptiveCard\"")
        assertContains(payload, "\"version\": \"1.2\"")
    }

    @Test
    fun `Teams payload should include project information`() {
        val ctx = createContext()
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "MyProject")
        assertContains(payload, "MyBuildType")
        assertContains(payload, "Build #123")
    }

    @Test
    fun `Teams payload should use correct title for success`() {
        val ctx = createContext(status = NotificationStatus.SUCCESS)
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "✅")
        assertContains(payload, "Successful")
    }

    @Test
    fun `Teams payload should use correct title for failure`() {
        val ctx = createContext(status = NotificationStatus.FAILURE)
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "❌")
        assertContains(payload, "Failed")
    }

    @Test
    fun `Teams payload should use correct title for started`() {
        val ctx = createContext(status = NotificationStatus.STARTED)
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "▶️")
        assertContains(payload, "Started")
    }

    @Test
    fun `Teams payload should color-code the title by status`() {
        val success = teamsGenerator.generatePayload(createContext(status = NotificationStatus.SUCCESS))
        assertContains(success, "\"color\": \"Good\"")

        val failure = teamsGenerator.generatePayload(createContext(status = NotificationStatus.FAILURE))
        assertContains(failure, "\"color\": \"Attention\"")

        val started = teamsGenerator.generatePayload(createContext(status = NotificationStatus.STARTED))
        assertContains(started, "\"color\": \"Accent\"")

        val stalled = teamsGenerator.generatePayload(createContext(status = NotificationStatus.STALLED))
        assertContains(stalled, "\"color\": \"Warning\"")
    }

    @Test
    fun `Teams payload should escape special characters`() {
        val ctx = createContext(
            message = "Build \"failed\" with error:\nLine 1\tTabbed\\Path"
        )
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "\\\"failed\\\"")
        assertContains(payload, "\\n")
        assertContains(payload, "\\t")
        assertContains(payload, "\\\\")
    }

    @Test
    fun `Teams payload should include View Build action`() {
        val ctx = createContext()
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "\"actions\"")
        assertContains(payload, "View Build")
        assertContains(payload, "Action.OpenUrl")
    }

    @Test
    fun `Teams payload should include changes for started builds`() {
        val changes = listOf(
            ChangeSummary(version = "abc123", user = "John Doe", comment = "Fix bug")
        )
        val ctx = createContext(status = NotificationStatus.STARTED, changes = changes)
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "Recent Changes")
        assertContains(payload, "John Doe")
        assertContains(payload, "Fix bug")
    }

    @Test
    fun `Teams payload should include artifacts for completed builds`() {
        val artifacts = listOf(
            ArtifactSummary(
                name = "app.jar",
                path = "build/libs/app.jar",
                size = 1024L,
                downloadUrl = "https://teamcity.example.com/repository/download/MyBuild/123:id/build/libs/app.jar"
            )
        )
        val ctx = createContext(status = NotificationStatus.SUCCESS, artifacts = artifacts)
        val payload = teamsGenerator.generatePayload(ctx)

        assertContains(payload, "app.jar")
        assertContains(payload, "repository/download")
    }

    @Test
    fun `Teams payload should handle null values gracefully`() {
        val ctx = createContext(
            projectName = null,
            buildTypeName = null,
            triggeredBy = null
        )
        val payload = teamsGenerator.generatePayload(ctx)

        assertTrue(payload.isNotEmpty())
        assertContains(payload, "Unknown Project")
        assertContains(payload, "Unknown Config")
    }

    // ========== DISCORD TESTS ==========

    @Test
    fun `Discord payload should contain required embed fields`() {
        val ctx = createContext()
        val payload = discordGenerator.generatePayload(ctx)

        assertTrue(payload.isNotEmpty())
        assertContains(payload, "\"embeds\"")
        assertContains(payload, "\"title\"")
        assertContains(payload, "\"description\"")
        assertContains(payload, "\"color\"")
    }

    @Test
    fun `Discord payload should use correct color for success`() {
        val ctx = createContext(status = NotificationStatus.SUCCESS)
        val payload = discordGenerator.generatePayload(ctx)

        assertContains(payload, "3066993") // green #2ECC71 in decimal
    }

    @Test
    fun `Discord payload should use correct color for failure`() {
        val ctx = createContext(status = NotificationStatus.FAILURE)
        val payload = discordGenerator.generatePayload(ctx)

        assertContains(payload, "15158332") // red #E74C3C in decimal
    }

    @Test
    fun `Discord payload should include project information`() {
        val ctx = createContext()
        val payload = discordGenerator.generatePayload(ctx)

        assertContains(payload, "MyProject")
        assertContains(payload, "MyBuildType")
        assertContains(payload, "#123")
    }

    @Test
    fun `Discord payload should escape special characters`() {
        val ctx = createContext(
            message = "Build \"failed\" with error:\nLine 1"
        )
        val payload = discordGenerator.generatePayload(ctx)

        assertContains(payload, "\\\"failed\\\"")
        assertContains(payload, "\\n")
    }

    @Test
    fun `Discord payload should include fields for metadata`() {
        val ctx = createContext()
        val payload = discordGenerator.generatePayload(ctx)

        assertContains(payload, "\"fields\"")
        assertContains(payload, "Project")
        assertContains(payload, "Build Config")
    }

    @Test
    fun `Discord payload should handle null values gracefully`() {
        val ctx = createContext(
            projectName = null,
            buildTypeName = null,
            triggeredBy = null
        )
        val payload = discordGenerator.generatePayload(ctx)

        assertTrue(payload.isNotEmpty())
        assertContains(payload, "Unknown Project")
        assertContains(payload, "Unknown Config")
    }

    // ========== EDGE CASES ==========

    @Test
    fun `all generators should handle empty message`() {
        val ctx = createContext(message = "")

        val slackPayload = slackGenerator.generatePayload(ctx)
        val teamsPayload = teamsGenerator.generatePayload(ctx)
        val discordPayload = discordGenerator.generatePayload(ctx)

        assertTrue(slackPayload.isNotEmpty())
        assertTrue(teamsPayload.isNotEmpty())
        assertTrue(discordPayload.isNotEmpty())
    }

    @Test
    fun `all generators should handle very long messages`() {
        val longMessage = "A".repeat(5000)
        val ctx = createContext(message = longMessage)

        val slackPayload = slackGenerator.generatePayload(ctx)
        val teamsPayload = teamsGenerator.generatePayload(ctx)
        val discordPayload = discordGenerator.generatePayload(ctx)

        assertTrue(slackPayload.isNotEmpty())
        assertTrue(teamsPayload.isNotEmpty())
        assertTrue(discordPayload.isNotEmpty())
    }

    @Test
    fun `all generators should handle special characters in project names`() {
        val ctx = createContext(
            projectName = "My \"Special\" Project & Co.",
            buildTypeName = "Build <Type> #1"
        )

        val slackPayload = slackGenerator.generatePayload(ctx)
        val teamsPayload = teamsGenerator.generatePayload(ctx)
        val discordPayload = discordGenerator.generatePayload(ctx)

        // Should escape quotes
        assertContains(slackPayload, "\\\"")
        assertContains(teamsPayload, "\\\"")
        assertContains(discordPayload, "\\\"")
    }

    @Test
    fun `all generators should handle Unicode characters`() {
        val ctx = createContext(
            message = "Build finished 🎉 with ✅ success! Emoji: 😀 Japanese: こんにちは Chinese: 你好"
        )

        val slackPayload = slackGenerator.generatePayload(ctx)
        val teamsPayload = teamsGenerator.generatePayload(ctx)
        val discordPayload = discordGenerator.generatePayload(ctx)

        assertTrue(slackPayload.isNotEmpty())
        assertTrue(teamsPayload.isNotEmpty())
        assertTrue(discordPayload.isNotEmpty())
    }

    @Test
    fun `all generators should handle null change fields`() {
        val changes = listOf(
            ChangeSummary(version = null, user = null, comment = null)
        )
        val ctx = createContext(status = NotificationStatus.STARTED, changes = changes)

        val slackPayload = slackGenerator.generatePayload(ctx)
        val teamsPayload = teamsGenerator.generatePayload(ctx)

        assertTrue(slackPayload.isNotEmpty())
        assertTrue(teamsPayload.isNotEmpty())
    }

    @Test
    fun `all generators should limit changes to first 3`() {
        val changes = (1..10).map { i ->
            ChangeSummary(version = "commit$i", user = "User$i", comment = "Change $i")
        }
        val ctx = createContext(status = NotificationStatus.STARTED, changes = changes)

        val slackPayload = slackGenerator.generatePayload(ctx)
        val teamsPayload = teamsGenerator.generatePayload(ctx)

        // Should contain first 3
        assertContains(slackPayload, "User1")
        assertContains(slackPayload, "User2")
        assertContains(slackPayload, "User3")
        // Should not contain the rest
        assertFalse(slackPayload.contains("User10"))

        // Teams card applies the same limit
        assertContains(teamsPayload, "User1")
        assertContains(teamsPayload, "User3")
        assertFalse(teamsPayload.contains("User10"))
    }

    @Test
    fun `all generators should limit artifacts to first 3 buttons`() {
        val artifacts = (1..10).map { i ->
            ArtifactSummary(
                name = "artifact$i.jar",
                path = "build/libs/artifact$i.jar",
                size = 1024L,
                downloadUrl = "https://example.com/artifact$i.jar"
            )
        }
        val ctx = createContext(status = NotificationStatus.SUCCESS, artifacts = artifacts)

        val slackPayload = slackGenerator.generatePayload(ctx)
        val teamsPayload = teamsGenerator.generatePayload(ctx)

        // Should contain first 3
        assertContains(slackPayload, "artifact1.jar")
        assertContains(slackPayload, "artifact2.jar")
        assertContains(slackPayload, "artifact3.jar")

        // Should have "More artifacts" button
        assertContains(slackPayload, "More artifacts")

        // Teams card applies the same limit and "More artifacts..." overflow
        assertContains(teamsPayload, "artifact1.jar")
        assertContains(teamsPayload, "artifact3.jar")
        assertContains(teamsPayload, "More artifacts")
    }

    @Test
    fun `change comment with a quote is JSON-escaped exactly once (not double-escaped)`() {
        // Regression: previously the per-change line was escaped, then the whole changes block was
        // escaped again, producing a literal backslash sequence (\\") in the rendered card.
        val changes = listOf(
            ChangeSummary(version = "abc123", user = "Jane \"JD\" Doe", comment = "Fix \"auth\" bug")
        )
        val ctx = createContext(status = NotificationStatus.SUCCESS, changes = changes)

        for ((name, payload) in listOf(
            "Slack" to slackGenerator.generatePayload(ctx),
            "Teams" to teamsGenerator.generatePayload(ctx),
            "Discord" to discordGenerator.generatePayload(ctx)
        )) {
            // The quote must appear escaped once...
            assertContains(payload, "\\\"auth\\\"", message = "$name should escape the quote once")
            // ...and never double-escaped (a literal backslash before the escaped quote).
            assertFalse(
                payload.contains("\\\\\""),
                "$name double-escaped the change comment (found \\\\\")"
            )
        }
    }
}
