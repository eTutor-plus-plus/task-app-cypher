package at.jku.dke.task_app.cypher.graph;

import java.util.List;
import java.util.Map;

public record GraphSnapshot(List<Node> nodes, List<Edge> edges, boolean truncated) {

    public record Node(String id, List<String> labels, Map<String, Object> properties) {
    }

    public record Edge(String sourceId, String targetId, String type, Map<String, Object> properties) {
    }
}
