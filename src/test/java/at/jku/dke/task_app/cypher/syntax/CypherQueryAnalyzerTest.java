package at.jku.dke.task_app.cypher.syntax;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherQueryAnalyzerTest {

    private final CypherQueryAnalyzer analyzer = new CypherQueryAnalyzer();

    @Test
    void blankQueryProducesEmptyStructure() {
        assertTrue(analyzer.analyze("   ").isEmpty());
        assertTrue(analyzer.analyze(null).isEmpty());
    }

    @Test
    void extractsSingleNodeLabel() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person) RETURN p.name AS name");
        assertEquals(1, s.nodes().size());
        assertEquals(List.of("Person"), s.nodes().get(0).labels());
        assertTrue(s.relationships().isEmpty());
    }

    @Test
    void extractsMultiLabelNode() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person:Actor) RETURN p");
        assertEquals(List.of("Person", "Actor"), s.nodes().get(0).labels());
    }

    @Test
    void orientsOutgoingRelationship() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person)-[:ACTED_IN]->(m:Movie) RETURN m");
        assertEquals(1, s.relationships().size());
        CypherQueryStructure.RelationshipPattern rel = s.relationships().get(0);
        assertEquals(List.of("ACTED_IN"), rel.type());
        assertEquals(List.of("Person"), rel.sourceLabels());
        assertEquals(List.of("Movie"), rel.targetLabels());
        assertTrue(rel.directed());
    }

    @Test
    void orientsIncomingRelationshipBySwappingEndpoints() {
        CypherQueryStructure s = analyzer.analyze("MATCH (m:Movie)<-[:ACTED_IN]-(p:Person) RETURN p");
        CypherQueryStructure.RelationshipPattern rel = s.relationships().get(0);
        assertEquals(List.of("Person"), rel.sourceLabels());
        assertEquals(List.of("Movie"), rel.targetLabels());
        assertTrue(rel.directed());
    }

    @Test
    void marksUndirectedRelationship() {
        CypherQueryStructure s = analyzer.analyze("MATCH (a:Person)-[:KNOWS]-(b:Person) RETURN a");
        assertFalse(s.relationships().get(0).directed());
    }

    @Test
    void humanizesWhereFilter() {
        CypherQueryStructure s = analyzer.analyze(
            "MATCH (p:Person) WHERE p.age > 30 AND p.name STARTS WITH 'A' RETURN p.name");
        assertEquals(List.of("age > 30 and name starts with 'A'"), s.filters());
    }

    @Test
    void surfacesInlinePropertyMapAsFilter() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person {name: 'Alice'}) RETURN p");
        assertTrue(s.filters().contains("name = 'Alice'"));
    }

    @Test
    void detectsDistinct() {
        assertTrue(analyzer.analyze("MATCH (p:Person) RETURN DISTINCT p.name").distinct());
        assertFalse(analyzer.analyze("MATCH (p:Person) RETURN p.name").distinct());
    }

    @Test
    void detectsAggregation() {
        assertTrue(analyzer.analyze("MATCH (p:Person) RETURN count(*)").aggregated());
        assertTrue(analyzer.analyze("MATCH (p:Person) RETURN p.dept, avg(p.age)").aggregated());
        assertFalse(analyzer.analyze("MATCH (p:Person) RETURN p.name").aggregated());
    }

    @Test
    void parsesOrderByDirectionAndStripsPrefix() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person) RETURN p.name ORDER BY p.name DESC");
        assertEquals(1, s.orderBy().size());
        assertEquals("name", s.orderBy().get(0).expression());
        assertTrue(s.orderBy().get(0).descending());
    }

    @Test
    void parsesMultipleOrderByItems() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person) RETURN p ORDER BY p.age DESC, p.name");
        assertEquals(2, s.orderBy().size());
        assertTrue(s.orderBy().get(0).descending());
        assertFalse(s.orderBy().get(1).descending());
        assertEquals("name", s.orderBy().get(1).expression());
    }

    @Test
    void parsesSkipAndLimit() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person) RETURN p SKIP 5 LIMIT 10");
        assertEquals("5", s.skip());
        assertEquals("10", s.limit());
    }

    @Test
    void detectsUnionAndAggregatesNodesFromBothBranches() {
        CypherQueryStructure s = analyzer.analyze(
            "MATCH (a:Person) RETURN a.name AS x UNION MATCH (b:Movie) RETURN b.title AS x");
        assertTrue(s.union());
        assertEquals(2, s.nodes().size());
    }

    @Test
    void ignoresClauseKeywordsInsideStringLiterals() {
        CypherQueryStructure s = analyzer.analyze("MATCH (n) WHERE n.note = 'no RETURN here' RETURN n.x");
        assertEquals(List.of("note = 'no RETURN here'"), s.filters());
        assertEquals(1, s.nodes().size());
    }

    @Test
    void handlesOptionalMatch() {
        CypherQueryStructure s = analyzer.analyze("MATCH (p:Person) OPTIONAL MATCH (p)-[:ACTED_IN]->(m:Movie) RETURN p");
        assertEquals(List.of("ACTED_IN"), s.relationships().get(0).type());
    }

    @Test
    void deduplicatesRepeatedPatterns() {
        CypherQueryStructure s = analyzer.analyze(
            "MATCH (a:Person)-[:KNOWS]->(b:Person) MATCH (c:Person)-[:KNOWS]->(d:Person) RETURN a");
        assertEquals(1, s.nodes().size());
        assertEquals(1, s.relationships().size());
    }
}
