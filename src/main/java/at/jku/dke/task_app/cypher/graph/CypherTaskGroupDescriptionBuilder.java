package at.jku.dke.task_app.cypher.graph;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CypherTaskGroupDescriptionBuilder {

    private final MessageSource messageSource;

    public CypherTaskGroupDescriptionBuilder(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String build(Locale locale, GraphSummary summary, String imageBase64, boolean truncated) {
        StringBuilder sb = new StringBuilder("<div>");
        sb.append("<p>").append(message(locale, "defaultTaskGroupDescription")).append("</p>");

        if (summary != null && !summary.isEmpty())
            appendSummary(sb, locale, summary);

        if (imageBase64 != null && !imageBase64.isBlank()) {
            sb.append("<img alt=\"")
                .append(message(locale, "graphImage.alt"))
                .append("\" src=\"data:image/png;base64,")
                .append(imageBase64)
                .append("\" style=\"max-width:100%;height:auto;\"/>");
        }

        if (truncated) {
            sb.append("<p><em>")
                .append(message(locale, "graphImage.truncated"))
                .append("</em></p>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private void appendSummary(StringBuilder sb, Locale locale, GraphSummary summary) {
        sb.append("<p>")
            .append(message(locale, "graphSummary.intro", summary.totalNodes(), summary.totalEdges()))
            .append("</p>");

        if (!summary.labels().isEmpty()) {
            sb.append("<p><strong>").append(message(locale, "graphSummary.nodes")).append("</strong></p><ul>");
            for (GraphSummary.LabelInfo info : summary.labels())
                sb.append("<li>").append(renderNodeLine(locale, info)).append("</li>");
            sb.append("</ul>");
        }

        if (!summary.relationships().isEmpty()) {
            sb.append("<p><strong>").append(message(locale, "graphSummary.relationships")).append("</strong></p><ul>");
            for (GraphSummary.RelationshipInfo info : summary.relationships())
                sb.append("<li>").append(renderRelationshipLine(locale, info)).append("</li>");
            sb.append("</ul>");
        }
    }

    private String renderNodeLine(Locale locale, GraphSummary.LabelInfo info) {
        String label = displayLabel(locale, info.label());
        String key = info.count() == 1 ? "graphSummary.nodeLine.singular" : "graphSummary.nodeLine.plural";
        String line = info.count() == 1 ? message(locale, key, label) : message(locale, key, label, info.count());
        return line + " &mdash; " + propertiesPart(locale, info.properties());
    }

    private String renderRelationshipLine(Locale locale, GraphSummary.RelationshipInfo info) {
        String source = displayLabels(locale, info.sourceLabels());
        String target = displayLabels(locale, info.targetLabels());
        String key = info.count() == 1 ? "graphSummary.relationshipLine.singular" : "graphSummary.relationshipLine.plural";
        String line = info.count() == 1
            ? message(locale, key, info.type(), source, target)
            : message(locale, key, info.type(), info.count(), source, target);
        return line + " &mdash; " + propertiesPart(locale, info.properties());
    }

    private String propertiesPart(Locale locale, List<GraphSummary.PropertyInfo> properties) {
        if (properties == null || properties.isEmpty())
            return message(locale, "graphSummary.noProperties");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            if (i > 0) sb.append(", ");
            GraphSummary.PropertyInfo property = properties.get(i);
            sb.append(escape(property.key()));
            if (!property.sampleValues().isEmpty()) {
                List<String> escaped = property.sampleValues().stream().map(CypherTaskGroupDescriptionBuilder::escape).toList();
                sb.append(' ').append(message(locale, "graphSummary.samples", String.join(", ", escaped)));
            }
        }
        return message(locale, "graphSummary.properties", sb.toString());
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private String displayLabel(Locale locale, String label) {
        return label == null || label.isBlank() ? message(locale, "graphSummary.unlabeled") : label;
    }

    private String displayLabels(Locale locale, List<String> labels) {
        if (labels == null || labels.isEmpty()) return message(locale, "graphSummary.unlabeled");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(displayLabel(locale, labels.get(i)));
        }
        return sb.toString();
    }

    private String message(Locale locale, String key, Object... args) {
        return this.messageSource.getMessage(key, args, locale);
    }
}
