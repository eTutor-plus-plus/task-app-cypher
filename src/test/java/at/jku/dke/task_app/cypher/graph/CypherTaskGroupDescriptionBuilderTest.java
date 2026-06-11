package at.jku.dke.task_app.cypher.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherTaskGroupDescriptionBuilderTest {

    private CypherTaskGroupDescriptionBuilder builder;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages");
        source.setDefaultEncoding("UTF-8");
        builder = new CypherTaskGroupDescriptionBuilder(source);
    }

    @Test
    void rendersIntroOnlyWhenSummaryIsEmpty() {
        String html = builder.build(Locale.ENGLISH, GraphSummary.from(null), null, false);
        assertTrue(html.startsWith("<div>"));
        assertTrue(html.contains("graph data is available"));
        assertFalse(html.contains("<ul>"));
    }

    @Test
    void rendersNodeAndRelationshipBullets() {
        GraphSnapshot snapshot = new GraphSnapshot(
            List.of(
                new GraphSnapshot.Node("1", List.of("Person"), Map.of("name", "Alice")),
                new GraphSnapshot.Node("2", List.of("Movie"), Map.of("title", "M"))),
            List.of(
                new GraphSnapshot.Edge("1", "2", "ACTED_IN", Map.of("role", "lead"))),
            false);
        String html = builder.build(Locale.ENGLISH, GraphSummary.from(snapshot), null, false);

        assertTrue(html.contains("<strong>Person</strong>"));
        assertTrue(html.contains("<strong>Movie</strong>"));
        assertTrue(html.contains("<strong>ACTED_IN</strong>"));
        assertTrue(html.contains("Person") && html.contains("Movie"));
        assertTrue(html.contains("name"));
        assertTrue(html.contains("role"));
    }

    @Test
    void embedsBase64ImageWhenProvided() {
        String html = builder.build(Locale.ENGLISH, GraphSummary.from(null), "AAAA", false);
        assertTrue(html.contains("data:image/png;base64,AAAA"));
    }

    @Test
    void rendersTruncationHintWhenTruncated() {
        String html = builder.build(Locale.ENGLISH, GraphSummary.from(null), null, true);
        assertTrue(html.contains("too large"));
    }
}
