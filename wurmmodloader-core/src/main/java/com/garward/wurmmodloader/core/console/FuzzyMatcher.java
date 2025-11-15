package com.garward.wurmmodloader.core.console;

import java.util.*;

/**
 * Fuzzy string matching utility for "Did you mean?" suggestions.
 *
 * <p>Uses Levenshtein distance to find close matches when exact matches fail.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class FuzzyMatcher {

    /**
     * Find best match from a list of candidates.
     *
     * @param input User input
     * @param candidates List of valid options
     * @param maxDistance Maximum Levenshtein distance to consider (default: 3)
     * @return Best match or null if none found
     */
    public static String findBestMatch(String input, List<String> candidates, int maxDistance) {
        if (input == null || candidates == null || candidates.isEmpty()) {
            return null;
        }

        String normalizedInput = input.toLowerCase().trim();
        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            String normalizedCandidate = candidate.toLowerCase();

            // Exact match
            if (normalizedCandidate.equals(normalizedInput)) {
                return candidate;
            }

            // Prefix match (higher priority)
            if (normalizedCandidate.startsWith(normalizedInput)) {
                int distance = normalizedCandidate.length() - normalizedInput.length();
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = candidate;
                }
                continue;
            }

            // Contains match
            if (normalizedCandidate.contains(normalizedInput)) {
                int distance = normalizedCandidate.length() - normalizedInput.length();
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = candidate;
                }
                continue;
            }

            // Levenshtein distance
            int distance = levenshteinDistance(normalizedInput, normalizedCandidate);
            if (distance <= maxDistance && distance < bestDistance) {
                bestDistance = distance;
                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    /**
     * Find best match with default max distance of 3.
     *
     * @param input User input
     * @param candidates List of valid options
     * @return Best match or null if none found
     */
    public static String findBestMatch(String input, List<String> candidates) {
        return findBestMatch(input, candidates, 3);
    }

    /**
     * Find multiple matches (up to 3 suggestions).
     *
     * @param input User input
     * @param candidates List of valid options
     * @param maxDistance Maximum Levenshtein distance
     * @return List of suggestions (max 3)
     */
    public static List<String> findSuggestions(String input, List<String> candidates, int maxDistance) {
        if (input == null || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedInput = input.toLowerCase().trim();
        List<Match> matches = new ArrayList<>();

        for (String candidate : candidates) {
            String normalizedCandidate = candidate.toLowerCase();

            // Exact match
            if (normalizedCandidate.equals(normalizedInput)) {
                return Collections.singletonList(candidate);
            }

            int distance;
            int priority;

            // Prefix match (highest priority)
            if (normalizedCandidate.startsWith(normalizedInput)) {
                distance = normalizedCandidate.length() - normalizedInput.length();
                priority = 0;
            }
            // Contains match
            else if (normalizedCandidate.contains(normalizedInput)) {
                distance = normalizedCandidate.length() - normalizedInput.length();
                priority = 1;
            }
            // Levenshtein distance
            else {
                distance = levenshteinDistance(normalizedInput, normalizedCandidate);
                priority = 2;
            }

            if (distance <= maxDistance) {
                matches.add(new Match(candidate, distance, priority));
            }
        }

        // Sort by priority, then distance, then alphabetically
        Collections.sort(matches);

        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < Math.min(3, matches.size()); i++) {
            suggestions.add(matches.get(i).value);
        }

        return suggestions;
    }

    /**
     * Calculate Levenshtein distance between two strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Edit distance
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }

    /**
     * Helper class for sorting matches.
     */
    private static class Match implements Comparable<Match> {
        final String value;
        final int distance;
        final int priority;

        Match(String value, int distance, int priority) {
            this.value = value;
            this.distance = distance;
            this.priority = priority;
        }

        @Override
        public int compareTo(Match other) {
            // Sort by priority first
            if (this.priority != other.priority) {
                return Integer.compare(this.priority, other.priority);
            }
            // Then by distance
            if (this.distance != other.distance) {
                return Integer.compare(this.distance, other.distance);
            }
            // Then alphabetically
            return this.value.compareTo(other.value);
        }
    }
}
