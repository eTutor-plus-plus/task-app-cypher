package at.jku.dke.task_app.cypher.graph;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherGraphPngExporterTest {

    @Test
    void rendersImageForSimpleGraph() throws Exception {
        GraphSnapshot snapshot = new GraphSnapshot(
            List.of(
                node("1", "Person", Map.of("name", "Alice")),
                node("2", "Person", Map.of("name", "Bob")),
                node("3", "Movie", Map.of("title", "The Matrix"))),
            List.of(
                edge("1", "2", "KNOWS"),
                edge("1", "3", "ACTED_IN")),
            false);

        BufferedImage img = CypherGraphPngExporter.render(snapshot, "en", 42L);

        assertNotNull(img);
        assertTrue(img.getWidth() > 0);
        assertTrue(img.getHeight() > 0);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(img, "png", out));
            assertTrue(out.size() > 200, "PNG payload should be non-trivial");

            BufferedImage reread = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
            assertEquals(img.getWidth(), reread.getWidth());
            assertEquals(img.getHeight(), reread.getHeight());
        }
    }

    @Test
    void returnsNullForEmptyGraph() {
        assertNull(CypherGraphPngExporter.render(new GraphSnapshot(List.of(), List.of(), false), "en", 0L));
    }

    @Test
    void sameSeedProducesIdenticalImage() throws Exception {
        GraphSnapshot snapshot = new GraphSnapshot(
            List.of(
                node("a", "Person", Map.of("name", "Alice")),
                node("b", "Person", Map.of("name", "Bob")),
                node("c", "Person", Map.of("name", "Carol"))),
            List.of(edge("a", "b", "KNOWS"), edge("b", "c", "KNOWS")),
            false);

        byte[] first = encode(CypherGraphPngExporter.render(snapshot, "de", 7L));
        byte[] second = encode(CypherGraphPngExporter.render(snapshot, "de", 7L));

        assertArrayEquals(first, second, "deterministic seed should produce byte-identical PNGs");
    }

    private static GraphSnapshot.Node node(String id, String label, Map<String, Object> props) {
        return new GraphSnapshot.Node(id, List.of(label), props);
    }

    private static GraphSnapshot.Edge edge(String src, String dst, String type) {
        return new GraphSnapshot.Edge(src, dst, type, Map.of());
    }

    private static byte[] encode(BufferedImage img) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        }
    }
}
