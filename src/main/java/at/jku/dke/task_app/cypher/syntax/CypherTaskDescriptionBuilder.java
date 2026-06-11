package at.jku.dke.task_app.cypher.syntax;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CypherTaskDescriptionBuilder {

    private final MessageSource messageSource;

    public CypherTaskDescriptionBuilder(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String build(Locale locale, CypherQueryStructure structure, List<String> columns) {
        StringBuilder body = new StringBuilder();

        if (structure != null && (!structure.nodes().isEmpty() || !structure.relationships().isEmpty()))
            appendMatch(body, locale, structure);
        if (structure != null && !structure.filters().isEmpty())
            appendFilters(body, locale, structure.filters());
        appendReturn(body, locale, structure, columns);
        if (structure != null && !structure.orderBy().isEmpty())
            appendOrder(body, locale, structure.orderBy());
        if (structure != null && (structure.skip() != null || structure.limit() != null))
            appendPagination(body, locale, structure);
        if (structure != null && structure.union())
            appendListItem(body, locale, "task.section.combine", message(locale, "task.union"));

        if (body.length() == 0)
            return "<div><p>" + message(locale, "defaultTaskDescription") + "</p></div>";

        return "<div><p>" + message(locale, "task.intro") + "</p><ul>" + body + "</ul></div>";
    }

    private void appendMatch(StringBuilder sb, Locale locale, CypherQueryStructure structure) {
        StringBuilder content = new StringBuilder();
        if (!structure.nodes().isEmpty())
            content.append(nodesPhrase(locale, structure.nodes()));

        if (!structure.relationships().isEmpty()) {
            content.append("<ul>");
            for (CypherQueryStructure.RelationshipPattern rel : structure.relationships())
                content.append("<li>").append(relationshipPhrase(locale, rel)).append("</li>");
            content.append("</ul>");
        }
        appendListItem(sb, locale, "task.section.match", content.toString());
    }

    private String nodesPhrase(Locale locale, List<CypherQueryStructure.NodePattern> nodes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("<strong>").append(labelText(locale, nodes.get(i).labels())).append("</strong>");
        }
        return message(locale, "task.match.nodes", sb.toString());
    }

    private String relationshipPhrase(Locale locale, CypherQueryStructure.RelationshipPattern rel) {
        String source = "<strong>" + labelText(locale, rel.sourceLabels()) + "</strong>";
        String target = "<strong>" + labelText(locale, rel.targetLabels()) + "</strong>";
        String type = rel.type().isEmpty()
            ? message(locale, "task.match.anyType")
            : "<em>" + escape(String.join("|", rel.type())) + "</em>";
        String arrow = rel.directed() ? " &rarr; " : " &mdash; ";
        return source + arrow + type + arrow + target;
    }

    private void appendFilters(StringBuilder sb, Locale locale, List<String> filters) {
        StringBuilder content = new StringBuilder("<ul>");
        for (String filter : filters)
            content.append("<li><code>").append(escape(filter)).append("</code></li>");
        content.append("</ul>");
        appendListItem(sb, locale, "task.section.filter", content.toString());
    }

    private void appendReturn(StringBuilder sb, Locale locale, CypherQueryStructure structure, List<String> columns) {
        boolean distinct = structure != null && structure.distinct();
        boolean aggregated = structure != null && structure.aggregated();
        if (columns.isEmpty() && !distinct && !aggregated)
            return;

        StringBuilder content = new StringBuilder();
        if (columns.isEmpty())
            content.append(message(locale, "task.return.result"));
        else
            content.append(message(locale, "task.return.columns", columnList(columns)));

        if (distinct)
            content.append(" ").append(message(locale, "task.return.distinct"));
        if (aggregated)
            content.append(" ").append(message(locale, "task.return.aggregated"));
        appendListItem(sb, locale, "task.section.return", content.toString());
    }

    private void appendOrder(StringBuilder sb, Locale locale, List<CypherQueryStructure.OrderItem> order) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) items.append(", ");
            CypherQueryStructure.OrderItem item = order.get(i);
            items.append("<code>").append(escape(item.expression())).append("</code>");
            items.append(" (").append(message(locale, item.descending() ? "task.order.desc" : "task.order.asc")).append(")");
        }
        appendListItem(sb, locale, "task.section.order", message(locale, "task.order.by", items.toString()));
    }

    private void appendPagination(StringBuilder sb, Locale locale, CypherQueryStructure structure) {
        StringBuilder content = new StringBuilder();
        if (structure.skip() != null)
            content.append(message(locale, "task.limit.skip", "<code>" + escape(structure.skip()) + "</code>"));
        if (structure.limit() != null) {
            if (content.length() > 0) content.append(" ");
            content.append(message(locale, "task.limit.limit", "<code>" + escape(structure.limit()) + "</code>"));
        }
        appendListItem(sb, locale, "task.section.pagination", content.toString());
    }

    private void appendListItem(StringBuilder sb, Locale locale, String sectionKey, String content) {
        sb.append("<li><strong>").append(message(locale, sectionKey)).append(":</strong> ").append(content).append("</li>");
    }

    private String columnList(List<String> columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("<code>").append(escape(columns.get(i))).append("</code>");
        }
        return sb.toString();
    }

    private String labelText(Locale locale, List<String> labels) {
        if (labels == null || labels.isEmpty())
            return message(locale, "task.match.anyNode");
        return escape(String.join(":", labels));
    }

    private String message(Locale locale, String key, Object... args) {
        return this.messageSource.getMessage(key, args, locale);
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
