package at.jku.dke.task_app.cypher.syntax;

import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class CypherReturnColumnExtractor {

    private static final Set<String> RETURN_TERMINATORS = Set.of("ORDER", "SKIP", "LIMIT", "UNION");

    public List<String> extractColumnNames(String query) {
        if (query == null || query.isBlank())
            throw new CypherValidationException("Query is empty.");

        int returnStart = findReturnStart(query);
        if (returnStart < 0)
            throw new CypherValidationException("No RETURN clause found.");

        int returnEnd = findReturnEnd(query, returnStart);
        String body = query.substring(returnStart, returnEnd).trim();
        body = stripLeadingKeyword(body, "DISTINCT");

        List<String> parts = splitTopLevelCommas(body);
        if (parts.isEmpty())
            throw new CypherValidationException("RETURN clause has no expressions.");

        List<String> columns = new ArrayList<>(parts.size());
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty())
                throw new CypherValidationException("RETURN clause contains an empty expression.");
            columns.add(extractAlias(trimmed));
        }
        return columns;
    }

    private static int findReturnStart(String query) {
        Scanner scanner = new Scanner(query);
        while (scanner.advance()) {
            if (scanner.depth() == 0 && scanner.atTopLevelKeyword("RETURN"))
                return scanner.position();
        }
        return -1;
    }

    private static int findReturnEnd(String query, int from) {
        Scanner scanner = new Scanner(query, from);
        while (scanner.advance()) {
            if (scanner.depth() == 0 && scanner.atAnyTopLevelKeyword(RETURN_TERMINATORS))
                return scanner.keywordStart();
        }
        return query.length();
    }

    private static String stripLeadingKeyword(String body, String keyword) {
        String trimmed = body.stripLeading();
        if (trimmed.length() < keyword.length()) return body;
        if (!trimmed.regionMatches(true, 0, keyword, 0, keyword.length())) return body;
        if (trimmed.length() > keyword.length()) {
            char after = trimmed.charAt(keyword.length());
            if (Character.isLetterOrDigit(after) || after == '_') return body;
        }
        return trimmed.substring(keyword.length()).trim();
    }

    private static List<String> splitTopLevelCommas(String body) {
        List<String> parts = new ArrayList<>();
        Scanner scanner = new Scanner(body);
        int last = 0;
        while (scanner.advance()) {
            if (scanner.depth() == 0 && body.charAt(scanner.position() - 1) == ',') {
                parts.add(body.substring(last, scanner.position() - 1));
                last = scanner.position();
            }
        }
        parts.add(body.substring(last));
        return parts;
    }

    private static String extractAlias(String expression) {
        Scanner scanner = new Scanner(expression);
        while (scanner.advance()) {
            if (scanner.depth() == 0 && scanner.atTopLevelKeyword("AS")) {
                String alias = expression.substring(scanner.position()).trim();
                return alias.isEmpty() ? expression : unquoteBackticks(alias);
            }
        }
        return expression;
    }

    private static String unquoteBackticks(String text) {
        if (text.length() >= 2 && text.charAt(0) == '`' && text.charAt(text.length() - 1) == '`')
            return text.substring(1, text.length() - 1).replace("``", "`");
        return text;
    }

    private static final class Scanner {
        private final String text;
        private final int end;
        private int pos;
        private int depth;
        private int keywordStart = -1;

        Scanner(String text) {
            this(text, 0);
        }

        Scanner(String text, int from) {
            this.text = text;
            this.pos = from;
            this.end = text.length();
        }

        int position() {
            return pos;
        }

        int depth() {
            return depth;
        }

        int keywordStart() {
            return keywordStart;
        }

        boolean advance() {
            if (pos >= end) return false;
            char ch = text.charAt(pos);
            char next = pos + 1 < end ? text.charAt(pos + 1) : '\0';

            if (ch == '\'' || ch == '"') {
                skipString(ch);
                return true;
            }
            if (ch == '`') {
                skipBacktick();
                return true;
            }
            if (ch == '/' && next == '/') {
                skipLineComment();
                return true;
            }
            if (ch == '/' && next == '*') {
                skipBlockComment();
                return true;
            }
            if (ch == '(' || ch == '[' || ch == '{') {
                depth++;
                pos++;
                return true;
            }
            if (ch == ')' || ch == ']' || ch == '}') {
                if (depth > 0) depth--;
                pos++;
                return true;
            }
            pos++;
            return true;
        }

        boolean atTopLevelKeyword(String keyword) {
            int wordEnd = pos;
            int wordStart = wordEnd - keyword.length();
            if (wordStart < 0) return false;
            if (!text.regionMatches(true, wordStart, keyword, 0, keyword.length())) return false;
            if (!isWordBoundaryBefore(wordStart) || !isWordBoundaryAfter(wordEnd)) return false;
            keywordStart = wordStart;
            return true;
        }

        boolean atAnyTopLevelKeyword(Set<String> keywords) {
            for (String keyword : keywords) {
                if (atTopLevelKeyword(keyword))
                    return true;
            }
            return false;
        }

        private boolean isWordBoundaryBefore(int idx) {
            if (idx == 0) return true;
            char prev = text.charAt(idx - 1);
            // Disqualify when the candidate word is part of a property accessor or label (e.g. "n.RETURN", ":RETURN").
            return !Character.isLetterOrDigit(prev) && prev != '_' && prev != '.' && prev != ':' && prev != '$' && prev != '`';
        }

        private boolean isWordBoundaryAfter(int idx) {
            if (idx >= end) return true;
            char ch = text.charAt(idx);
            return !Character.isLetterOrDigit(ch) && ch != '_';
        }

        private void skipString(char quote) {
            pos++;
            boolean escaped = false;
            while (pos < end) {
                char c = text.charAt(pos++);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    return;
                }
            }
        }

        private void skipBacktick() {
            pos++;
            while (pos < end) {
                char c = text.charAt(pos++);
                if (c == '`') {
                    if (pos < end && text.charAt(pos) == '`') {
                        pos++; // doubled backtick = escape
                    } else {
                        return;
                    }
                }
            }
        }

        private void skipLineComment() {
            pos += 2;
            while (pos < end) {
                char c = text.charAt(pos++);
                if (c == '\n' || c == '\r') return;
            }
        }

        private void skipBlockComment() {
            pos += 2;
            while (pos < end - 1) {
                if (text.charAt(pos) == '*' && text.charAt(pos + 1) == '/') {
                    pos += 2;
                    return;
                }
                pos++;
            }
            pos = end;
        }
    }
}
