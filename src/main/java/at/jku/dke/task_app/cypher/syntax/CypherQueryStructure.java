package at.jku.dke.task_app.cypher.syntax;

import java.util.List;

public record CypherQueryStructure(List<NodePattern> nodes,
                                   List<RelationshipPattern> relationships,
                                   List<String> filters,
                                   boolean distinct,
                                   boolean aggregated,
                                   List<OrderItem> orderBy,
                                   String skip,
                                   String limit,
                                   boolean union) {

    public record NodePattern(List<String> labels) {
    }

    public record RelationshipPattern(List<String> type, List<String> sourceLabels, List<String> targetLabels, boolean directed) {
    }

    public record OrderItem(String expression, boolean descending) {
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && relationships.isEmpty() && filters.isEmpty()
            && orderBy.isEmpty() && skip == null && limit == null && !distinct && !aggregated;
    }
}
