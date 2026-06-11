package sk.v2.plugins.teamnotify.utils

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BranchMatcherTest {

    @Test
    fun `null branch always matches`() {
        assertTrue(BranchMatcher.matches(null, "+:main"))
    }

    @Test
    fun `blank or null filter always matches`() {
        assertTrue(BranchMatcher.matches("anything", null))
        assertTrue(BranchMatcher.matches("anything", ""))
        assertTrue(BranchMatcher.matches("anything", "   "))
    }

    @Test
    fun `single include rule matches only the named branch`() {
        assertTrue(BranchMatcher.matches("main", "+:main"))
        assertFalse(BranchMatcher.matches("develop", "+:main"))
    }

    @Test
    fun `exclude after broad include wins (last-match semantics)`() {
        // The whole point of the fix: -:feature/* must override the earlier +:*
        assertTrue(BranchMatcher.matches("main", "+:*,-:feature/*"))
        assertFalse(BranchMatcher.matches("feature/login", "+:*,-:feature/*"))
    }

    @Test
    fun `include after broad exclude wins`() {
        assertFalse(BranchMatcher.matches("feature/x", "-:*,+:main"))
        assertTrue(BranchMatcher.matches("main", "-:*,+:main"))
    }

    @Test
    fun `only-exclude filter includes non-matching branches by default`() {
        assertTrue(BranchMatcher.matches("main", "-:feature/*"))
        assertFalse(BranchMatcher.matches("feature/x", "-:feature/*"))
    }

    @Test
    fun `include filter excludes non-matching branches by default`() {
        assertTrue(BranchMatcher.matches("release/1.0", "+:release/*"))
        assertFalse(BranchMatcher.matches("main", "+:release/*"))
    }

    @Test
    fun `wildcards and single-char patterns match`() {
        assertTrue(BranchMatcher.matches("release/1.2.3", "+:release/*"))
        assertTrue(BranchMatcher.matches("v1", "+:v?"))
        assertFalse(BranchMatcher.matches("v12", "+:v?"))
    }
}
