package at.jku.dke.task_app.cypher.syntax;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherTaskDescriptionBuilderTest {

    private final CypherQueryAnalyzer analyzer = new CypherQueryAnalyzer();
    private CypherTaskDescriptionBuilder builder;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages");
        source.setDefaultEncoding("UTF-8");
        builder = new CypherTaskDescriptionBuilder(source);
    }

    private String describe(String query, List<String> columns) {
        return builder.build(Locale.ENGLISH, analyzer.analyze(query), columns);
    }

    @Test
    void fallsBackToDefaultWhenNothingDescriptive() {
        String html = builder.build(Locale.ENGLISH, null, List.of());
        assertTrue(html.contains("returns the requested result"));
        assertFalse(html.contains("<ul>"));
    }

    @Test
    void rendersColumnsWhenStructureEmpty() {
        String html = builder.build(Locale.ENGLISH, null, List.of("name", "age"));
        assertTrue(html.contains("<code>name</code>"));
        assertTrue(html.contains("<code>age</code>"));
    }

    @Test
    void rendersFullBreakdown() {
        String html = describe(
            "MATCH (p:Person)-[:ACTED_IN]->(m:Movie) WHERE p.age > 30 "
                + "RETURN p.name AS name ORDER BY p.name DESC LIMIT 10",
            List.of("name"));

        assertTrue(html.contains("<strong>Person</strong>"));
        assertTrue(html.contains("<strong>Movie</strong>"));
        assertTrue(html.contains("<em>ACTED_IN</em>"));
        assertTrue(html.contains("&rarr;"));
        assertTrue(html.contains("<code>name</code>"));
        assertTrue(html.contains("descending"));
        assertTrue(html.contains("<code>10</code>"));
    }

    @Test
    void escapesComparisonOperatorsInFilters() {
        String html = describe("MATCH (p:Person) WHERE p.age < 18 RETURN p.name", List.of("name"));
        assertTrue(html.contains("age &lt; 18"));
        assertFalse(html.contains("age < 18"));
    }

    @Test
    void marksDistinctAndAggregation() {
        String distinct = describe("MATCH (p:Person) RETURN DISTINCT p.name", List.of("p.name"));
        assertTrue(distinct.contains("distinct"));

        String aggregated = describe("MATCH (p:Person) RETURN p.dept, count(*) AS c", List.of("p.dept", "c"));
        assertTrue(aggregated.contains("aggregation"));
    }

    @Test
    void rendersGermanSections() {
        String html = builder.build(Locale.GERMAN, analyzer.analyze("MATCH (p:Person) RETURN p.name"), List.of("name"));
        assertTrue(html.contains("Folgendes leistet"));
        assertTrue(html.contains("Zurückgeben"));
    }

    @Test
    void rendersEntityAndAliasForReturnColumns() {
        String html = describe("MATCH (p:Person) RETURN p.name AS testname", List.of("testname"));
        assertTrue(html.contains("<code>name</code> of <strong>Person</strong> as <code>testname</code>"));
    }

    @Test
    void rendersEntityAndAliasForReturnColumnsInGerman() {
        String html = builder.build(Locale.GERMAN,
            analyzer.analyze("MATCH (p:Person) RETURN p.name AS testname"), List.of("testname"));
        assertTrue(html.contains("<code>name</code> von <strong>Person</strong> als <code>testname</code>"));
    }

    @Test
    void rendersEntityForOrderBy() {
        String html = describe("MATCH (p:Person) RETURN p.name AS name ORDER BY p.name", List.of("name"));
        assertTrue(html.contains("<code>name</code> of <strong>Person</strong> (ascending)"));
    }

    @Test
    void omitsAliasWhenColumnNameEqualsProperty() {
        String html = describe("MATCH (p:Person) RETURN p.name AS name", List.of("name"));
        assertTrue(html.contains("<code>name</code> of <strong>Person</strong>"));
        assertFalse(html.contains(" as <code>name</code>"));
    }
}
