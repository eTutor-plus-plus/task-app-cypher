package at.jku.dke.task_app.cypher.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphSummaryTest {

    @Test
    void emptySnapshotProducesEmptySummary() {
        GraphSummary summary = GraphSummary.from(new GraphSnapshot(List.of(), List.of(), false));
        assertTrue(summary.isEmpty());
        assertEquals(0, summary.totalNodes());
        assertEquals(0, summary.totalEdges());
    }

    @Test
    void nullSnapshotProducesEmptySummary() {
        assertTrue(GraphSummary.from(null).isEmpty());
    }

    @Test
    void groupsNodesByLabelTupleAndCollectsPropertyKeys() {
        GraphSnapshot snapshot = new GraphSnapshot(
            List.of(
                node("1", List.of("Person"), Map.of("name", "Alice", "age", 30)),
                node("2", List.of("Person"), Map.of("name", "Bob")),
                node("3", List.of("Movie"), Map.of("title", "M1", "year", 1999)),
                node("4", List.of("Person", "Actor"), Map.of("name", "Carol"))),
            List.of(),
            false);

        GraphSummary summary = GraphSummary.from(snapshot);

        assertEquals(4, summary.totalNodes());
        assertEquals(3, summary.labels().size());

        assertEquals("Actor:Person", summary.labels().get(0).label());
        assertEquals(1, summary.labels().get(0).count());

        assertEquals("Movie", summary.labels().get(1).label());
        assertEquals(1, summary.labels().get(1).count());
        assertEquals(List.of("title", "year"), keys(summary.labels().get(1).properties()));

        assertEquals("Person", summary.labels().get(2).label());
        assertEquals(2, summary.labels().get(2).count());
        assertEquals(List.of("age", "name"), keys(summary.labels().get(2).properties()));
    }

    @Test
    void collectsCappedDistinctSampleValuesPerProperty() {
        GraphSnapshot snapshot = new GraphSnapshot(
            List.of(
                node("1", List.of("Person"), Map.of("name", "Alice")),
                node("2", List.of("Person"), Map.of("name", "Bob")),
                node("3", List.of("Person"), Map.of("name", "Carol")),
                node("4", List.of("Person"), Map.of("name", "Dave"))),
            List.of(),
            false);

        GraphSummary.PropertyInfo name = GraphSummary.from(snapshot).labels().get(0).properties().get(0);
        assertEquals("name", name.key());
        assertEquals(GraphSummary.MAX_SAMPLES_PER_PROPERTY, name.sampleValues().size());
        assertTrue(name.sampleValues().contains("'Alice'"));
    }

    private static List<String> keys(List<GraphSummary.PropertyInfo> properties) {
        return properties.stream().map(GraphSummary.PropertyInfo::key).toList();
    }

    @Test
    void groupsRelationshipsByTypeAndCollectsLabelsAndProperties() {
        GraphSnapshot snapshot = new GraphSnapshot(
            List.of(
                node("p1", List.of("Person"), Map.of()),
                node("p2", List.of("Person"), Map.of()),
                node("m1", List.of("Movie"), Map.of()),
                node("m2", List.of("Movie"), Map.of())),
            List.of(
                edge("p1", "m1", "ACTED_IN", Map.of("role", "lead")),
                edge("p2", "m1", "ACTED_IN", Map.of()),
                edge("p1", "p2", "KNOWS", Map.of("since", 2020))),
            false);

        GraphSummary summary = GraphSummary.from(snapshot);

        assertEquals(3, summary.totalEdges());
        assertEquals(2, summary.relationships().size());

        GraphSummary.RelationshipInfo acted = summary.relationships().get(0);
        assertEquals("ACTED_IN", acted.type());
        assertEquals(2, acted.count());
        assertEquals(List.of("Person"), acted.sourceLabels());
        assertEquals(List.of("Movie"), acted.targetLabels());
        assertEquals(List.of("role"), keys(acted.properties()));

        GraphSummary.RelationshipInfo knows = summary.relationships().get(1);
        assertEquals("KNOWS", knows.type());
        assertEquals(1, knows.count());
        assertEquals(List.of("Person"), knows.sourceLabels());
        assertEquals(List.of("Person"), knows.targetLabels());
        assertEquals(List.of("since"), keys(knows.properties()));
    }

    private static GraphSnapshot.Node node(String id, List<String> labels, Map<String, Object> props) {
        return new GraphSnapshot.Node(id, labels, props);
    }

    private static GraphSnapshot.Edge edge(String source, String target, String type, Map<String, Object> props) {
        return new GraphSnapshot.Edge(source, target, type, props);
    }
}
