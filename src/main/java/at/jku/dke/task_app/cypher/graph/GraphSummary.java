package at.jku.dke.task_app.cypher.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record GraphSummary(int totalNodes,
                           int totalEdges,
                           List<LabelInfo> labels,
                           List<RelationshipInfo> relationships) {

    public static final int MAX_SAMPLES_PER_PROPERTY = 3;
    private static final int MAX_SAMPLE_LENGTH = 40;

    public record PropertyInfo(String key, List<String> sampleValues) {
    }

    public record LabelInfo(String label, int count, List<PropertyInfo> properties) {
    }

    public record RelationshipInfo(String type,
                                   int count,
                                   List<String> sourceLabels,
                                   List<String> targetLabels,
                                   List<PropertyInfo> properties) {
    }

    public static GraphSummary from(GraphSnapshot snapshot) {
        if (snapshot == null || snapshot.nodes() == null || snapshot.nodes().isEmpty())
            return new GraphSummary(0, 0, List.of(), List.of());

        Map<String, List<String>> nodeIdToLabel = new LinkedHashMap<>();
        Map<String, NodeAggregate> nodeAggregates = new TreeMap<>();
        for (GraphSnapshot.Node node : snapshot.nodes()) {
            String labelKey = joinLabels(node.labels());
            nodeIdToLabel.put(node.id(), List.of(labelKey));
            NodeAggregate agg = nodeAggregates.computeIfAbsent(labelKey, k -> new NodeAggregate());
            agg.count++;
            agg.properties.add(node.properties());
        }

        List<LabelInfo> labels = new ArrayList<>(nodeAggregates.size());
        for (Map.Entry<String, NodeAggregate> entry : nodeAggregates.entrySet())
            labels.add(new LabelInfo(entry.getKey(), entry.getValue().count, entry.getValue().properties.toList()));

        Map<String, RelationshipAggregate> relAggregates = new TreeMap<>();
        if (snapshot.edges() != null) {
            for (GraphSnapshot.Edge edge : snapshot.edges()) {
                RelationshipAggregate agg = relAggregates.computeIfAbsent(edge.type(), k -> new RelationshipAggregate());
                agg.count++;
                List<String> source = nodeIdToLabel.get(edge.sourceId());
                List<String> target = nodeIdToLabel.get(edge.targetId());
                if (source != null) agg.sourceLabels.addAll(source);
                if (target != null) agg.targetLabels.addAll(target);
                agg.properties.add(edge.properties());
            }
        }

        List<RelationshipInfo> relationships = new ArrayList<>(relAggregates.size());
        for (Map.Entry<String, RelationshipAggregate> entry : relAggregates.entrySet()) {
            RelationshipAggregate agg = entry.getValue();
            relationships.add(new RelationshipInfo(
                entry.getKey(),
                agg.count,
                List.copyOf(agg.sourceLabels),
                List.copyOf(agg.targetLabels),
                agg.properties.toList()));
        }

        int totalNodes = snapshot.nodes().size();
        int totalEdges = snapshot.edges() == null ? 0 : snapshot.edges().size();
        return new GraphSummary(totalNodes, totalEdges, labels, relationships);
    }

    public boolean isEmpty() {
        return totalNodes == 0;
    }

    private static String joinLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) return "";
        if (labels.size() == 1) return labels.get(0);
        List<String> copy = new ArrayList<>(labels);
        copy.sort(Comparator.naturalOrder());
        return String.join(":", copy);
    }

    private static String formatSample(Object value) {
        if (value == null) return null;
        String text = value instanceof CharSequence ? "'" + value + "'" : String.valueOf(value);
        if (text.length() > MAX_SAMPLE_LENGTH)
            text = text.substring(0, MAX_SAMPLE_LENGTH - 1) + "…";
        return text;
    }

    private static final class PropertyCollector {
        private final Map<String, LinkedHashSet<String>> samples = new TreeMap<>();

        void add(Map<String, Object> properties) {
            if (properties == null) return;
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                LinkedHashSet<String> values = this.samples.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>());
                String formatted = formatSample(entry.getValue());
                if (formatted != null && values.size() < MAX_SAMPLES_PER_PROPERTY)
                    values.add(formatted);
            }
        }

        List<PropertyInfo> toList() {
            List<PropertyInfo> result = new ArrayList<>(this.samples.size());
            for (Map.Entry<String, LinkedHashSet<String>> entry : this.samples.entrySet())
                result.add(new PropertyInfo(entry.getKey(), List.copyOf(entry.getValue())));
            return result;
        }
    }

    private static final class NodeAggregate {
        int count;
        final PropertyCollector properties = new PropertyCollector();
    }

    private static final class RelationshipAggregate {
        int count;
        final java.util.Set<String> sourceLabels = new java.util.TreeSet<>();
        final java.util.Set<String> targetLabels = new java.util.TreeSet<>();
        final PropertyCollector properties = new PropertyCollector();
    }
}
