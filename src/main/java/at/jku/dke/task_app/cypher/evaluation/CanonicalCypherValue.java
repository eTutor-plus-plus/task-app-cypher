package at.jku.dke.task_app.cypher.evaluation;

import at.jku.dke.task_app.cypher.evaluation.model.CanonicalValue;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CanonicalCypherValue {
    private CanonicalCypherValue() {
    }

    public static CanonicalValue from(Value value) {
        if (value == null || value.isNull())
            return new CanonicalValue("null", "null");
        return fromObject(value.asObject());
    }

    public static CanonicalValue fromObject(Object object) {
        if (object == null)
            return new CanonicalValue("null", "null");
        if (object instanceof Node node)
            return node(node);
        if (object instanceof Relationship relationship)
            return relationship(relationship);
        if (object instanceof Path path)
            return path(path);
        if (object instanceof Map<?, ?> map)
            return map(map);
        if (object instanceof Iterable<?> iterable)
            return iterable(iterable);
        if (object.getClass().isArray())
            return array(object);
        if (object instanceof CharSequence sequence)
            return new CanonicalValue("string:" + quote(sequence.toString()), sequence.toString());
        if (object instanceof Number number)
            return new CanonicalValue(object.getClass().getName() + ':' + number, number.toString());
        if (object instanceof Boolean bool)
            return new CanonicalValue("boolean:" + bool, bool.toString());

        return new CanonicalValue(object.getClass().getName() + ':' + quote(object.toString()), object.toString());
    }

    private static CanonicalValue node(Node node) {
        List<String> labels = new ArrayList<>();
        node.labels().forEach(labels::add);
        labels.sort(String::compareTo);
        CanonicalValue properties = map(node.asMap());
        String canonical = "node:{id:" + quote(node.elementId()) + ",labels:" + labels + ",properties:" + properties.canonical() + '}';
        String display = '(' + String.join(":", labels) + ' ' + properties.display() + ')';
        return new CanonicalValue(canonical, display);
    }

    private static CanonicalValue relationship(Relationship relationship) {
        CanonicalValue properties = map(relationship.asMap());
        String canonical = "relationship:{id:" + quote(relationship.elementId()) +
            ",start:" + quote(relationship.startNodeElementId()) +
            ",end:" + quote(relationship.endNodeElementId()) +
            ",type:" + quote(relationship.type()) +
            ",properties:" + properties.canonical() + '}';
        String display = "-[:" + relationship.type() + ' ' + properties.display() + "]->";
        return new CanonicalValue(canonical, display);
    }

    private static CanonicalValue path(Path path) {
        List<CanonicalValue> parts = new ArrayList<>();
        parts.add(node(path.start()));
        for (Path.Segment segment : path) {
            parts.add(relationship(segment.relationship()));
            parts.add(node(segment.end()));
        }
        return sequence("path", parts);
    }

    private static CanonicalValue map(Map<?, ?> map) {
        List<? extends Map.Entry<?, ?>> entries = map.entrySet().stream()
            .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
            .toList();
        StringBuilder canonical = new StringBuilder("map:{");
        StringBuilder display = new StringBuilder("{");
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<?, ?> entry = entries.get(i);
            CanonicalValue value = fromObject(entry.getValue());
            if (i > 0) {
                canonical.append(',');
                display.append(", ");
            }
            canonical.append(quote(String.valueOf(entry.getKey()))).append(':').append(value.canonical());
            display.append(entry.getKey()).append(": ").append(value.display());
        }
        return new CanonicalValue(canonical.append('}').toString(), display.append('}').toString());
    }

    private static CanonicalValue iterable(Iterable<?> iterable) {
        List<CanonicalValue> values = new ArrayList<>();
        iterable.forEach(value -> values.add(fromObject(value)));
        return sequence("list", values);
    }

    private static CanonicalValue array(Object array) {
        List<CanonicalValue> values = new ArrayList<>();
        for (int i = 0; i < Array.getLength(array); i++) {
            values.add(fromObject(Array.get(array, i)));
        }
        return sequence("list", values);
    }

    private static CanonicalValue sequence(String type, List<CanonicalValue> values) {
        StringBuilder canonical = new StringBuilder(type).append(":[");
        StringBuilder display = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                canonical.append(',');
                display.append(", ");
            }
            canonical.append(values.get(i).canonical());
            display.append(values.get(i).display());
        }
        return new CanonicalValue(canonical.append(']').toString(), display.append(']').toString());
    }

    private static String quote(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
