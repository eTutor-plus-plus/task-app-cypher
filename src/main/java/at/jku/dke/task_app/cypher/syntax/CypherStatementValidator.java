package at.jku.dke.task_app.cypher.syntax;

import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class CypherStatementValidator {
    private static final Set<String> READ_PROHIBITED = Set.of(
        "CREATE", "MERGE", "SET", "DELETE", "DETACH", "REMOVE", "DROP", "LOAD", "FOREACH", "CALL",
        "USE", "ALTER", "GRANT", "DENY", "REVOKE", "START", "STOP", "SHOW"
    );

    private static final Set<String> ADMIN_KEYWORDS = Set.of(
        "DROP", "ALTER", "GRANT", "DENY", "REVOKE", "START", "STOP", "SHOW"
    );

    private static final Set<String> CREATE_ADMIN_TARGETS = Set.of(
        "DATABASE", "USER", "ROLE"
    );

    private final CypherStatementSplitter splitter;

    public CypherStatementValidator(CypherStatementSplitter splitter) {
        this.splitter = splitter;
    }

    public void validateSetupScript(String script) {
        List<String> statements = this.splitter.split(script);
        if (statements.isEmpty())
            throw new CypherValidationException("Setup statements must not be empty.");

        for (String statement : statements) {
            List<Token> tokens = keywordTokens(statement);
            if (contains(tokens, "CALL"))
                throw new CypherValidationException("Setup statements must not call procedures.");
            if (containsPair(tokens, "LOAD", "CSV"))
                throw new CypherValidationException("Setup statements must not use LOAD CSV.");
            if (contains(tokens, "USE"))
                throw new CypherValidationException("Setup statements must not switch databases.");
            if (containsAny(tokens, ADMIN_KEYWORDS))
                throw new CypherValidationException("Setup statements contain prohibited administrative clauses.");
            if (contains(tokens, "INDEX") || contains(tokens, "CONSTRAINT"))
                throw new CypherValidationException("Setup statements must not manage indexes or constraints.");
            if (containsAdminCreate(tokens))
                throw new CypherValidationException("Setup statements must not create databases, users, or roles.");
        }
    }

    public void validateReadQuery(String query) {
        List<String> statements = this.splitter.split(query);
        if (statements.size() != 1)
            throw new CypherValidationException("A solution or submission must contain exactly one Cypher query.");

        List<Token> tokens = keywordTokens(statements.getFirst());
        for (Token token : tokens) {
            if (READ_PROHIBITED.contains(token.text()))
                throw new CypherValidationException("Read-only queries must not contain " + token.text() + " clauses.");
        }
    }

    public boolean containsOrderBy(String query) {
        return containsPair(keywordTokens(query), "ORDER", "BY");
    }

    private static boolean containsAdminCreate(List<Token> tokens) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (tokens.get(i).text().equals("CREATE") && CREATE_ADMIN_TARGETS.contains(tokens.get(i + 1).text()))
                return true;
        }
        return false;
    }

    private static boolean contains(List<Token> tokens, String value) {
        return tokens.stream().anyMatch(token -> token.text().equals(value));
    }

    private static boolean containsAny(List<Token> tokens, Set<String> values) {
        return tokens.stream().anyMatch(token -> values.contains(token.text()));
    }

    private static boolean containsPair(List<Token> tokens, String first, String second) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (tokens.get(i).text().equals(first) && tokens.get(i + 1).text().equals(second))
                return true;
        }
        return false;
    }

    private static List<Token> keywordTokens(String statement) {
        List<Token> tokens = new ArrayList<>();
        State state = State.DEFAULT;
        int wordStart = -1;
        char wordPreviousNonWhitespace = '\0';
        char previousNonWhitespace = '\0';
        char quoteChar = '\0';
        boolean escaped = false;

        for (int i = 0; i < statement.length(); i++) {
            char ch = statement.charAt(i);
            char next = i + 1 < statement.length() ? statement.charAt(i + 1) : '\0';

            switch (state) {
                case DEFAULT -> {
                    if (Character.isLetter(ch) || ch == '_') {
                        if (wordStart < 0) {
                            wordStart = i;
                            wordPreviousNonWhitespace = previousNonWhitespace;
                        }
                    } else {
                        wordStart = finishWord(statement, tokens, wordStart, i, wordPreviousNonWhitespace);
                        if (ch == '\'' || ch == '"') {
                            state = State.QUOTED;
                            quoteChar = ch;
                        } else if (ch == '`') {
                            state = State.BACKTICK;
                        } else if (ch == '/' && next == '/') {
                            state = State.LINE_COMMENT;
                            i++;
                        } else if (ch == '/' && next == '*') {
                            state = State.BLOCK_COMMENT;
                            i++;
                        }
                    }
                    if (!Character.isWhitespace(ch))
                        previousNonWhitespace = ch;
                }
                case QUOTED -> {
                    if (escaped) {
                        escaped = false;
                    } else if (ch == '\\') {
                        escaped = true;
                    } else if (ch == quoteChar) {
                        state = State.DEFAULT;
                    }
                }
                case BACKTICK -> {
                    if (ch == '`')
                        state = State.DEFAULT;
                }
                case LINE_COMMENT -> {
                    if (ch == '\n' || ch == '\r')
                        state = State.DEFAULT;
                }
                case BLOCK_COMMENT -> {
                    if (ch == '*' && next == '/') {
                        state = State.DEFAULT;
                        i++;
                    }
                }
            }
        }

        finishWord(statement, tokens, wordStart, statement.length(), wordPreviousNonWhitespace);
        return tokens;
    }

    private static int finishWord(String statement, List<Token> tokens, int wordStart, int wordEnd, char previousNonWhitespace) {
        if (wordStart >= 0) {
            String word = statement.substring(wordStart, wordEnd).toUpperCase();
            if (previousNonWhitespace != ':' && previousNonWhitespace != '.')
                tokens.add(new Token(word));
        }
        return -1;
    }

    private record Token(String text) {
    }

    private enum State {
        DEFAULT,
        QUOTED,
        BACKTICK,
        LINE_COMMENT,
        BLOCK_COMMENT
    }
}
