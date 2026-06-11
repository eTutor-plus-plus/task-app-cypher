package at.jku.dke.task_app.cypher.evaluation.model;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record CypherQueryResult(List<String> keys, List<List<CanonicalValue>> rows) {

    public List<List<String>> canonicalRows() {
        return this.rows.stream()
            .map(row -> row.stream().map(CanonicalValue::canonical).toList())
            .toList();
    }

    public List<List<String>> displayRows() {
        return this.rows.stream()
            .map(row -> row.stream().map(CanonicalValue::display).toList())
            .toList();
    }

    public Map<List<String>, Long> rowMultiset() {
        return this.canonicalRows().stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
