package at.jku.dke.task_app.cypher.syntax;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CypherQueryAnalyzer {

    private static final Set<String> CLAUSE_KEYWORDS = Set.of(
        "MATCH", "WHERE", "WITH", "RETURN", "SKIP", "LIMIT", "UNWIND", "UNION", "CALL",
        "CREATE", "MERGE", "SET", "DELETE", "DETACH", "REMOVE", "FOREACH");

    private static final Set<String> AGGREGATION_FUNCTIONS = Set.of(
        "COUNT", "SUM", "AVG", "MIN", "MAX", "COLLECT", "STDEV", "STDEVP", "PERCENTILECONT", "PERCENTILEDISC");

    public CypherQueryStructure analyze(String query) {
        if (query == null || query.isBlank())
            return empty();

        List<Clause> clauses = splitClauses(query);

        List<CypherQueryStructure.NodePattern> nodes = new ArrayList<>();
        Set<String> seenNodes = new LinkedHashSet<>();
        List<CypherQueryStructure.RelationshipPattern> relationships = new ArrayList<>();
        Set<String> seenRels = new LinkedHashSet<>();
        List<String> filters = new ArrayList<>();
        List<CypherQueryStructure.OrderItem> orderBy = new ArrayList<>();

        boolean distinct = false;
        boolean aggregated = false;
        boolean union = false;
        String skip = null;
        String limit = null;

        for (Clause clause : clauses) {
            switch (clause.keyword()) {
                case "MATCH" -> parsePattern(clause.body(), nodes, seenNodes, relationships, seenRels, filters);
                case "WHERE" -> addFilter(filters, humanize(clause.body()));
                case "RETURN" -> {
                    distinct = startsWithKeyword(clause.body(), "DISTINCT");
                    if (containsAggregation(clause.body())) aggregated = true;
                }
                case "WITH" -> {
                    if (containsAggregation(clause.body())) aggregated = true;
                }
                case "ORDER_BY" -> {
                    orderBy.clear();
                    orderBy.addAll(parseOrderBy(clause.body()));
                }
                case "SKIP" -> skip = collapse(clause.body());
                case "LIMIT" -> limit = collapse(clause.body());
                case "UNION" -> union = true;
                default -> { /* WITH/UNWIND/CALL bodies carry no descriptive pattern data here */ }
            }
        }

        return new CypherQueryStructure(
            List.copyOf(nodes), List.copyOf(relationships), List.copyOf(filters),
            distinct, aggregated, List.copyOf(orderBy),
            blankToNull(skip), blankToNull(limit), union);
    }

    private static CypherQueryStructure empty() {
        return new CypherQueryStructure(List.of(), List.of(), List.of(), false, false, List.of(), null, null, false);
    }

    private record Clause(String keyword, String body) {
    }

    private record Word(String upper, int start, int end) {
    }

    private static List<Clause> splitClauses(String query) {
        List<Word> words = topLevelWords(query);

        List<int[]> heads = new ArrayList<>(); // {wordIndex, bodyCharStart}
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            Word w = words.get(i);
            if (w.upper().equals("BY")) continue;
            if (w.upper().equals("OPTIONAL") && i + 1 < words.size() && words.get(i + 1).upper().equals("MATCH")) {
                keywords.add("MATCH");
                heads.add(new int[]{i, words.get(i + 1).end()});
                continue;
            }
            if (w.upper().equals("MATCH") && i > 0 && words.get(i - 1).upper().equals("OPTIONAL"))
                continue;
            if (w.upper().equals("ORDER") && i + 1 < words.size() && words.get(i + 1).upper().equals("BY")) {
                keywords.add("ORDER_BY");
                heads.add(new int[]{i, words.get(i + 1).end()});
                continue;
            }
            if (w.upper().equals("WITH") && i > 0
                && (words.get(i - 1).upper().equals("STARTS") || words.get(i - 1).upper().equals("ENDS")))
                continue;
            if (CLAUSE_KEYWORDS.contains(w.upper())) {
                keywords.add(w.upper());
                heads.add(new int[]{i, w.end()});
            }
        }

        List<Clause> clauses = new ArrayList<>();
        for (int k = 0; k < heads.size(); k++) {
            int bodyStart = heads.get(k)[1];
            int bodyEnd = k + 1 < heads.size() ? words.get(heads.get(k + 1)[0]).start() : query.length();
            clauses.add(new Clause(keywords.get(k), query.substring(bodyStart, bodyEnd).trim()));
        }
        return clauses;
    }

    private static List<Word> topLevelWords(String s) {
        List<Word> words = new ArrayList<>();
        int n = s.length();
        int depth = 0;
        int wordStart = -1;
        char wordPrev = '\0';
        char prev = '\0';
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            char nx = i + 1 < n ? s.charAt(i + 1) : '\0';

            if (c == '\'' || c == '"') {
                wordStart = flush(s, words, wordStart, i, depth, wordPrev);
                i = skipString(s, i, c);
                prev = '\'';
                continue;
            }
            if (c == '`') {
                wordStart = flush(s, words, wordStart, i, depth, wordPrev);
                i = skipBacktick(s, i);
                prev = '`';
                continue;
            }
            if (c == '/' && nx == '/') {
                wordStart = flush(s, words, wordStart, i, depth, wordPrev);
                i = skipLineComment(s, i);
                continue;
            }
            if (c == '/' && nx == '*') {
                wordStart = flush(s, words, wordStart, i, depth, wordPrev);
                i = skipBlockComment(s, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                wordStart = flush(s, words, wordStart, i, depth, wordPrev);
                depth++;
                prev = c;
                i++;
                continue;
            }
            if (c == ')' || c == ']' || c == '}') {
                wordStart = flush(s, words, wordStart, i, depth, wordPrev);
                if (depth > 0) depth--;
                prev = c;
                i++;
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == '_') {
                if (wordStart < 0 && (Character.isLetter(c) || c == '_')) {
                    wordStart = i;
                    wordPrev = prev;
                }
                prev = c;
                i++;
                continue;
            }
            wordStart = flush(s, words, wordStart, i, depth, wordPrev);
            if (!Character.isWhitespace(c))
                prev = c;
            i++;
        }
        flush(s, words, wordStart, n, depth, wordPrev);
        return words;
    }

    private static int flush(String s, List<Word> words, int wordStart, int wordEnd, int depth, char wordPrev) {
        if (wordStart >= 0 && depth == 0 && wordPrev != '.' && wordPrev != ':' && wordPrev != '$') {
            words.add(new Word(s.substring(wordStart, wordEnd).toUpperCase(Locale.ROOT), wordStart, wordEnd));
        }
        return -1;
    }

    private static void parsePattern(String body,
                                     List<CypherQueryStructure.NodePattern> nodes, Set<String> seenNodes,
                                     List<CypherQueryStructure.RelationshipPattern> rels, Set<String> seenRels,
                                     List<String> filters) {
        int n = body.length();
        int i = 0;
        List<String> prevLabels = null;
        StringBuilder connector = new StringBuilder();

        while (i < n) {
            char c = body.charAt(i);
            if (c == '\'' || c == '"') {
                i = skipString(body, i, c);
                continue;
            }
            if (c == ',') {
                prevLabels = null;
                connector.setLength(0);
                i++;
                continue;
            }
            if (c == '(') {
                int close = matchingClose(body, i, '(', ')');
                String inner = body.substring(i + 1, close);
                List<String> labels = parseLabelsAndProps(inner, filters, true);
                addNode(nodes, seenNodes, labels);

                if (prevLabels != null && connector.length() > 0)
                    addRelationship(rels, seenRels, filters, connector.toString(), prevLabels, labels);

                prevLabels = labels;
                connector.setLength(0);
                i = close + 1;
                continue;
            }
            if (c == '[') {
                int close = matchingClose(body, i, '[', ']');
                connector.append(body, i, close + 1);
                i = close + 1;
                continue;
            }
            connector.append(c);
            i++;
        }
    }

    private static void addNode(List<CypherQueryStructure.NodePattern> nodes, Set<String> seen, List<String> labels) {
        String key = String.join(":", labels);
        if (seen.add(key))
            nodes.add(new CypherQueryStructure.NodePattern(labels));
    }

    private static void addRelationship(List<CypherQueryStructure.RelationshipPattern> rels, Set<String> seen,
                                        List<String> filters, String connector, List<String> left, List<String> right) {
        boolean incoming = connector.indexOf("<") >= 0;
        boolean outgoing = connector.indexOf(">") >= 0;
        boolean directed = incoming ^ outgoing;

        List<String> source = incoming && !outgoing ? right : left;
        List<String> target = incoming && !outgoing ? left : right;

        List<String> types = relationshipTypes(connector, filters);
        String key = String.join("|", types) + "@" + String.join(":", source) + "->" + String.join(":", target) + "/" + directed;
        if (seen.add(key))
            rels.add(new CypherQueryStructure.RelationshipPattern(types, source, target, directed));
    }

    private static List<String> relationshipTypes(String connector, List<String> filters) {
        int open = connector.indexOf('[');
        int close = connector.lastIndexOf(']');
        if (open < 0 || close <= open)
            return List.of();
        String inner = connector.substring(open + 1, close);
        // Drop a variable-length suffix (e.g. *1..3) before label/prop parsing.
        int star = inner.indexOf('*');
        String forLabels = star >= 0 ? inner.substring(0, star) : inner;
        return parseLabelsAndProps(forLabels + propsTail(inner, star), filters, false);
    }

    private static String propsTail(String inner, int star) {
        if (star < 0) return "";
        int brace = inner.indexOf('{', star);
        return brace >= 0 ? inner.substring(brace) : "";
    }

    private static List<String> parseLabelsAndProps(String inner, List<String> filters, boolean emitVarFilters) {
        String labelPart = inner;
        int brace = indexOfTopLevel(inner, '{');
        String mapPart = null;
        if (brace >= 0) {
            int close = matchingClose(inner, brace, '{', '}');
            mapPart = inner.substring(brace + 1, Math.min(close, inner.length()));
            labelPart = inner.substring(0, brace);
        }
        int wherePos = indexOfWhere(labelPart);
        if (wherePos >= 0) {
            String pred = labelPart.substring(wherePos + 5);
            labelPart = labelPart.substring(0, wherePos);
            if (emitVarFilters)
                addFilter(filters, humanize(pred));
        }

        List<String> labels = extractLabels(labelPart);

        if (mapPart != null && emitVarFilters) {
            for (String entry : splitTopLevelCommas(mapPart)) {
                int colon = indexOfTopLevel(entry, ':');
                if (colon > 0) {
                    String key = entry.substring(0, colon).trim();
                    String value = entry.substring(colon + 1).trim();
                    if (!key.isEmpty() && !value.isEmpty())
                        addFilter(filters, stripQuotesUnchanged(key) + " = " + value);
                }
            }
        }
        return labels;
    }

    private static List<String> extractLabels(String labelPart) {
        List<String> labels = new ArrayList<>();
        boolean afterColon = false;
        int i = 0;
        int n = labelPart.length();
        StringBuilder cur = new StringBuilder();
        while (i <= n) {
            char c = i < n ? labelPart.charAt(i) : '\0';
            if (c == '`') {
                int close = skipBacktick(labelPart, i);
                if (afterColon) cur.append(labelPart, i + 1, Math.max(i + 1, close - 1));
                i = close;
                continue;
            }
            boolean idChar = Character.isLetterOrDigit(c) || c == '_';
            if (idChar && afterColon) {
                cur.append(c);
                i++;
                continue;
            }
            if (cur.length() > 0) {
                labels.add(cur.toString());
                cur.setLength(0);
            }
            if (c == ':') afterColon = true;
            else if (c == '&' || c == '|' || c == '!' || Character.isWhitespace(c)) {  }
            else afterColon = false;
            i++;
        }
        return labels;
    }

    private static List<CypherQueryStructure.OrderItem> parseOrderBy(String body) {
        List<CypherQueryStructure.OrderItem> items = new ArrayList<>();
        for (String raw : splitTopLevelCommas(body)) {
            String item = raw.trim();
            if (item.isEmpty()) continue;
            boolean descending = false;
            String upper = item.toUpperCase(Locale.ROOT);
            if (upper.endsWith(" DESC") || upper.endsWith(" DESCENDING")) {
                descending = true;
                item = item.substring(0, item.lastIndexOf(' ')).trim();
            } else if (upper.endsWith(" ASC") || upper.endsWith(" ASCENDING")) {
                item = item.substring(0, item.lastIndexOf(' ')).trim();
            }
            items.add(new CypherQueryStructure.OrderItem(stripVariablePrefixes(item), descending));
        }
        return items;
    }

    private static String humanize(String predicate) {
        String text = stripVariablePrefixes(predicate);
        text = replaceKeyword(text, "STARTS WITH", "starts with");
        text = replaceKeyword(text, "ENDS WITH", "ends with");
        text = replaceKeyword(text, "IS NOT NULL", "is not null");
        text = replaceKeyword(text, "IS NULL", "is null");
        text = replaceKeyword(text, "CONTAINS", "contains");
        text = replaceKeyword(text, "AND", "and");
        text = replaceKeyword(text, "XOR", "xor");
        text = replaceKeyword(text, "OR", "or");
        text = replaceKeyword(text, "NOT", "not");
        text = replaceKeyword(text, "IN", "in");
        return collapse(text);
    }

    private static String replaceKeyword(String text, String keyword, String replacement) {
        StringBuilder sb = new StringBuilder();
        String upper = text.toUpperCase(Locale.ROOT);
        String upperKeyword = keyword.toUpperCase(Locale.ROOT);
        int from = 0;
        int idx;
        while ((idx = upper.indexOf(upperKeyword, from)) >= 0) {
            boolean boundaryBefore = idx == 0 || !isWordChar(text.charAt(idx - 1));
            int after = idx + keyword.length();
            boolean boundaryAfter = after >= text.length() || !isWordChar(text.charAt(after));
            if (boundaryBefore && boundaryAfter) {
                sb.append(text, from, idx).append(replacement);
                from = after;
            } else {
                sb.append(text, from, idx + 1);
                from = idx + 1;
            }
        }
        sb.append(text.substring(from));
        return sb.toString();
    }

    private static String stripVariablePrefixes(String text) {
        StringBuilder sb = new StringBuilder();
        int n = text.length();
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\'' || c == '"') {
                int end = skipString(text, i, c);
                sb.append(text, i, end);
                i = end;
                continue;
            }
            if ((Character.isLetter(c) || c == '_') && (i == 0 || !isWordChar(text.charAt(i - 1)) && text.charAt(i - 1) != '.')) {
                int j = i;
                while (j < n && isWordChar(text.charAt(j))) j++;
                if (j < n && text.charAt(j) == '.') {
                    int k = j + 1;
                    while (k < n && isWordChar(text.charAt(k))) k++;
                    // Drop the prefix only when it looks like var.property (not a function call var.f(...)).
                    if (k > j + 1 && (k >= n || text.charAt(k) != '(')) {
                        sb.append(text, j + 1, k);
                        i = k;
                        continue;
                    }
                }
                sb.append(text, i, j);
                i = j;
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static boolean containsAggregation(String body) {
        for (Word w : topLevelWords(body)) {
            if (AGGREGATION_FUNCTIONS.contains(w.upper())) {
                int after = w.end();
                while (after < body.length() && Character.isWhitespace(body.charAt(after))) after++;
                if (after < body.length() && body.charAt(after) == '(')
                    return true;
            }
        }
        return false;
    }

    private static boolean startsWithKeyword(String body, String keyword) {
        String trimmed = body.stripLeading();
        if (!trimmed.regionMatches(true, 0, keyword, 0, keyword.length()))
            return false;
        int after = keyword.length();
        return after >= trimmed.length() || !isWordChar(trimmed.charAt(after));
    }

    private static void addFilter(List<String> filters, String filter) {
        String trimmed = filter == null ? "" : filter.trim();
        if (!trimmed.isEmpty() && !filters.contains(trimmed))
            filters.add(trimmed);
    }

    private static List<String> splitTopLevelCommas(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int last = 0;
        int i = 0;
        int n = body.length();
        while (i < n) {
            char c = body.charAt(i);
            if (c == '\'' || c == '"') {
                i = skipString(body, i, c);
                continue;
            }
            if (c == '`') {
                i = skipBacktick(body, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                if (depth > 0) depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(body.substring(last, i));
                last = i + 1;
            }
            i++;
        }
        if (last <= n)
            parts.add(body.substring(last));
        return parts;
    }

    private static int indexOfTopLevel(String s, char target) {
        int depth = 0;
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\'' || c == '"') {
                i = skipString(s, i, c);
                continue;
            }
            if (c == '`') {
                i = skipBacktick(s, i);
                continue;
            }
            if (c == target && depth == 0) return i;
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                if (depth > 0) depth--;
            }
            i++;
        }
        return -1;
    }

    private static int indexOfWhere(String s) {
        String upper = s.toUpperCase(Locale.ROOT);
        int from = 0;
        int idx;
        while ((idx = upper.indexOf("WHERE", from)) >= 0) {
            boolean before = idx == 0 || !isWordChar(s.charAt(idx - 1));
            int after = idx + 5;
            boolean afterOk = after >= s.length() || !isWordChar(s.charAt(after));
            if (before && afterOk) return idx;
            from = idx + 1;
        }
        return -1;
    }

    private static int matchingClose(String s, int openIdx, char open, char close) {
        int depth = 0;
        int i = openIdx;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\'' || c == '"') {
                i = skipString(s, i, c);
                continue;
            }
            if (c == '`') {
                i = skipBacktick(s, i);
                continue;
            }
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
            i++;
        }
        return n;
    }

    private static int skipString(String s, int i, char quote) {
        i++;
        int n = s.length();
        boolean escaped = false;
        while (i < n) {
            char c = s.charAt(i++);
            if (escaped) escaped = false;
            else if (c == '\\') escaped = true;
            else if (c == quote) return i;
        }
        return n;
    }

    private static int skipBacktick(String s, int i) {
        i++;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i++);
            if (c == '`') {
                if (i < n && s.charAt(i) == '`') i++;
                else return i;
            }
        }
        return n;
    }

    private static int skipLineComment(String s, int i) {
        i += 2;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i++);
            if (c == '\n' || c == '\r') return i;
        }
        return n;
    }

    private static int skipBlockComment(String s, int i) {
        i += 2;
        int n = s.length();
        while (i < n - 1) {
            if (s.charAt(i) == '*' && s.charAt(i + 1) == '/') return i + 2;
            i++;
        }
        return n;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String stripQuotesUnchanged(String key) {
        return key;
    }

    private static String collapse(String text) {
        return text == null ? null : text.replaceAll("\\s+", " ").trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
