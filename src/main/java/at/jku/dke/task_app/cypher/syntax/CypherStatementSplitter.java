package at.jku.dke.task_app.cypher.syntax;

import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CypherStatementSplitter {

    public List<String> split(String script) {
        if (script == null || script.isBlank())
            return List.of();

        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        State state = State.DEFAULT;
        boolean escaped = false;

        for (int i = 0; i < script.length(); i++) {
            char ch = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';

            switch (state) {
                case DEFAULT -> {
                    if (ch == '\'' || ch == '"') {
                        state = ch == '\'' ? State.SINGLE_QUOTE : State.DOUBLE_QUOTE;
                        current.append(ch);
                    } else if (ch == '`') {
                        state = State.BACKTICK;
                        current.append(ch);
                    } else if (ch == '/' && next == '/') {
                        state = State.LINE_COMMENT;
                        current.append(ch).append(next);
                        i++;
                    } else if (ch == '/' && next == '*') {
                        state = State.BLOCK_COMMENT;
                        current.append(ch).append(next);
                        i++;
                    } else if (ch == ';') {
                        addIfNotBlank(statements, current);
                    } else {
                        current.append(ch);
                    }
                }
                case SINGLE_QUOTE -> {
                    current.append(ch);
                    if (escaped) {
                        escaped = false;
                    } else if (ch == '\\') {
                        escaped = true;
                    } else if (ch == '\'') {
                        state = State.DEFAULT;
                    }
                }
                case DOUBLE_QUOTE -> {
                    current.append(ch);
                    if (escaped) {
                        escaped = false;
                    } else if (ch == '\\') {
                        escaped = true;
                    } else if (ch == '"') {
                        state = State.DEFAULT;
                    }
                }
                case BACKTICK -> {
                    current.append(ch);
                    if (ch == '`')
                        state = State.DEFAULT;
                }
                case LINE_COMMENT -> {
                    current.append(ch);
                    if (ch == '\n' || ch == '\r')
                        state = State.DEFAULT;
                }
                case BLOCK_COMMENT -> {
                    current.append(ch);
                    if (ch == '*' && next == '/') {
                        current.append(next);
                        i++;
                        state = State.DEFAULT;
                    }
                }
            }
        }

        if (state == State.SINGLE_QUOTE || state == State.DOUBLE_QUOTE || state == State.BACKTICK || state == State.BLOCK_COMMENT)
            throw new CypherValidationException("Cypher script contains an unterminated string, escaped identifier, or block comment.");

        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty() && containsCode(statement))
            statements.add(statement);
        current.setLength(0);
    }

    private static boolean containsCode(String statement) {
        State state = State.DEFAULT;
        for (int i = 0; i < statement.length(); i++) {
            char ch = statement.charAt(i);
            char next = i + 1 < statement.length() ? statement.charAt(i + 1) : '\0';
            switch (state) {
                case DEFAULT -> {
                    if (Character.isWhitespace(ch)) {
                        continue;
                    }
                    if (ch == '/' && next == '/') {
                        state = State.LINE_COMMENT;
                        i++;
                    } else if (ch == '/' && next == '*') {
                        state = State.BLOCK_COMMENT;
                        i++;
                    } else {
                        return true;
                    }
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
                case SINGLE_QUOTE, DOUBLE_QUOTE, BACKTICK -> {
                    return true;
                }
            }
        }
        return false;
    }

    private enum State {
        DEFAULT,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        BACKTICK,
        LINE_COMMENT,
        BLOCK_COMMENT
    }
}
