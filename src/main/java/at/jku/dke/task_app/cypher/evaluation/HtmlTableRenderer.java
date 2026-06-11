package at.jku.dke.task_app.cypher.evaluation;

import at.jku.dke.task_app.cypher.evaluation.model.CypherQueryResult;

import java.util.List;

public final class HtmlTableRenderer {
    private HtmlTableRenderer() {
    }

    public static String render(CypherQueryResult result) {
        return render(result.keys(), result.displayRows());
    }

    public static String render(List<String> columns, List<List<String>> rows) {
        StringBuilder builder = new StringBuilder("<table border=\"1\"><thead><tr>");
        columns.forEach(column -> builder.append("<th>").append(escape(column)).append("</th>"));
        builder.append("</tr></thead><tbody>");
        rows.forEach(row -> {
            builder.append("<tr>");
            row.forEach(value -> builder.append("<td>").append(escape(value)).append("</td>"));
            builder.append("</tr>");
        });
        return builder.append("</tbody></table>").toString();
    }

    public static String escape(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
