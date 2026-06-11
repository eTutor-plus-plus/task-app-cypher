package at.jku.dke.task_app.cypher.evaluation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CypherComparison {
    private final CypherQueryResult solution;
    private final CypherQueryResult submission;
    private final boolean orderRelevant;
    private final boolean keysCorrect;
    private final boolean rowsCorrect;
    private final boolean orderCorrect;
    private final Map<String, String> nearMatches;
    private final List<String> renamedSubmissionKeys;

    public CypherComparison(CypherQueryResult solution, CypherQueryResult submission, boolean orderRelevant) {
        this.solution = solution;
        this.submission = submission;
        this.orderRelevant = orderRelevant;
        this.nearMatches = computeNearMatches(solution.keys(), submission.keys());
        this.renamedSubmissionKeys = applyMapping(submission.keys(), this.nearMatches);
        this.keysCorrect = solution.keys().equals(submission.keys());
        this.rowsCorrect = this.keysCorrect && solution.rowMultiset().equals(submission.rowMultiset());
        this.orderCorrect = !orderRelevant || solution.canonicalRows().equals(submission.canonicalRows());
    }

    public boolean isCorrect() {
        return this.keysCorrect && this.rowsCorrect && this.orderCorrect;
    }

    public boolean keysCorrect() {
        return keysCorrect;
    }

    public boolean rowsCorrect() {
        return rowsCorrect;
    }

    public boolean orderCorrect() {
        return orderCorrect;
    }

    public boolean orderRelevant() {
        return orderRelevant;
    }

    public List<String> keys() {
        return this.solution.keys();
    }

    public int expectedRowCount() {
        return this.solution.rows().size();
    }

    public int actualRowCount() {
        return this.submission.rows().size();
    }

    public List<String> missingKeys() {
        Set<String> effective = new HashSet<>(this.renamedSubmissionKeys);
        return this.solution.keys().stream().filter(key -> !effective.contains(key)).toList();
    }

    public List<String> superfluousKeys() {
        Set<String> solutionKeys = new HashSet<>(this.solution.keys());
        return this.submission.keys().stream()
            .filter(key -> !this.nearMatches.containsKey(key))
            .filter(key -> !solutionKeys.contains(key))
            .toList();
    }

    public Map<String, String> nearMatches() {
        return this.nearMatches;
    }

    public List<List<String>> missingRows() {
        return diff(this.solution.rowMultiset(), this.submission.rowMultiset());
    }

    public List<List<String>> superfluousRows() {
        return diff(this.submission.rowMultiset(), this.solution.rowMultiset());
    }

    private static List<List<String>> diff(Map<List<String>, Long> expected, Map<List<String>, Long> actual) {
        List<List<String>> result = new ArrayList<>();
        for (Map.Entry<List<String>, Long> entry : expected.entrySet()) {
            long missing = entry.getValue() - actual.getOrDefault(entry.getKey(), 0L);
            for (long i = 0; i < missing; i++) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static Map<String, String> computeNearMatches(List<String> solutionKeys, List<String> submissionKeys) {
        Set<String> takenSolution = new HashSet<>();
        Set<String> solutionSet = new HashSet<>(solutionKeys);
        for (String s : submissionKeys) {
            if (solutionSet.contains(s))
                takenSolution.add(s);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String sub : submissionKeys) {
            if (solutionSet.contains(sub))
                continue;
            String best = null;
            int bestDist = Integer.MAX_VALUE;
            for (String sol : solutionKeys) {
                if (takenSolution.contains(sol))
                    continue;
                int dist = levenshtein(sub, sol);
                int shorter = Math.min(sub.length(), sol.length());
                if (dist <= 2 && dist * 2 <= shorter && dist < bestDist) {
                    bestDist = dist;
                    best = sol;
                }
            }
            if (best != null) {
                result.put(sub, best);
                takenSolution.add(best);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<String> applyMapping(List<String> keys, Map<String, String> mapping) {
        if (mapping.isEmpty())
            return keys;
        return keys.stream().map(k -> mapping.getOrDefault(k, k)).toList();
    }

    private static int levenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }
}
