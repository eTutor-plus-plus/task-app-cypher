package at.jku.dke.task_app.cypher.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CanonicalCypherValueTest {
    @Test
    void canonicalizesMapsDeterministically() {
        var left = CanonicalCypherValue.fromObject(Map.of("b", 2, "a", List.of("x", "y")));
        var right = CanonicalCypherValue.fromObject(Map.of("a", List.of("x", "y"), "b", 2));

        assertEquals(left.canonical(), right.canonical());
        assertEquals("{a: [x, y], b: 2}", left.display());
    }

    @Test
    void canonicalizationKeepsScalarTypesDistinct() {
        var number = CanonicalCypherValue.fromObject(1L);
        var string = CanonicalCypherValue.fromObject("1");

        assertEquals("1", number.display());
        assertEquals("1", string.display());
        assertNotEquals(number.canonical(), string.canonical());
    }
}
