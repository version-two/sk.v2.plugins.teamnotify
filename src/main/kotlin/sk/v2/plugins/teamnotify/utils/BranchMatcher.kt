package sk.v2.plugins.teamnotify.utils

object BranchMatcher {
    
    data class FilterRule(
        val pattern: String,
        val isExclude: Boolean
    )
    
    fun matches(branchName: String?, filterPattern: String?): Boolean {
        if (branchName == null) return true
        if (filterPattern.isNullOrBlank()) return true
        
        val rules = parseFilter(filterPattern)
        if (rules.isEmpty()) return true
        
        for (rule in rules) {
            if (matchesPattern(branchName, rule.pattern)) {
                return !rule.isExclude
            }
        }
        
        val hasIncludeRules = rules.any { !it.isExclude }
        return !hasIncludeRules
    }
    
    private fun parseFilter(filterPattern: String): List<FilterRule> {
        return filterPattern.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { pattern ->
                when {
                    pattern.startsWith("-:") -> FilterRule(pattern.substring(2).trim(), true)
                    pattern.startsWith("+:") -> FilterRule(pattern.substring(2).trim(), false)
                    pattern.startsWith("-") -> FilterRule(pattern.substring(1).trim(), true)
                    pattern.startsWith("+") -> FilterRule(pattern.substring(1).trim(), false)
                    else -> FilterRule(pattern, false)
                }
            }
    }
    
    private fun matchesPattern(branchName: String, pattern: String): Boolean {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("?", ".")
            .replace("*", ".*")
        
        return try {
            val regex = Regex("^$regexPattern$", RegexOption.IGNORE_CASE)
            regex.matches(branchName)
        } catch (e: Exception) {
            false
        }
    }
    
    fun isValidPattern(filterPattern: String?): Boolean {
        if (filterPattern.isNullOrBlank()) return true
        
        return try {
            val rules = parseFilter(filterPattern)
            rules.all { rule ->
                val regexPattern = rule.pattern
                    .replace(".", "\\.")
                    .replace("?", ".")
                    .replace("*", ".*")
                Regex("^$regexPattern$").pattern
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}