package at.jku.dke.task_app.cypher.evaluation;

import at.jku.dke.task_app.cypher.config.Neo4jProperties;
import at.jku.dke.task_app.cypher.evaluation.model.CanonicalValue;
import at.jku.dke.task_app.cypher.evaluation.model.CypherComparison;
import at.jku.dke.task_app.cypher.evaluation.model.CypherQueryResult;
import at.jku.dke.task_app.cypher.exception.CypherResultLimitExceededException;
import at.jku.dke.task_app.cypher.graph.GraphSnapshot;
import at.jku.dke.task_app.cypher.syntax.CypherStatementSplitter;
import at.jku.dke.task_app.cypher.syntax.CypherStatementValidator;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CypherAnalyzer {
    public static final int MAX_GRAPH_NODES = 50;
    public static final int MAX_GRAPH_EDGES = 100;

    private final Driver driver;
    private final Neo4jProperties properties;
    private final CypherStatementSplitter splitter;
    private final CypherStatementValidator validator;

    public CypherAnalyzer(Driver driver, Neo4jProperties properties, CypherStatementSplitter splitter, CypherStatementValidator validator) {
        this.driver = driver;
        this.properties = properties;
        this.splitter = splitter;
        this.validator = validator;
    }

    public CypherQueryResult executeRead(String setupStatements, String query) {
        this.validator.validateSetupScript(setupStatements);
        this.validator.validateReadQuery(query);

        try (Session session = this.driver.session(this.sessionConfig());
             Transaction tx = session.beginTransaction(this.transactionConfig())) {
            this.runSetup(tx, setupStatements);
            CypherQueryResult result = this.runQuery(tx, query);
            tx.rollback();
            return result;
        }
    }

    public CypherComparison compare(String setupStatements, String solution, String submission) {
        this.validator.validateSetupScript(setupStatements);
        this.validator.validateReadQuery(solution);
        this.validator.validateReadQuery(submission);

        try (Session session = this.driver.session(this.sessionConfig());
             Transaction tx = session.beginTransaction(this.transactionConfig())) {
            this.runSetup(tx, setupStatements);
            CypherQueryResult solutionResult = this.runQuery(tx, solution);
            CypherQueryResult submissionResult = this.runQuery(tx, submission);
            tx.rollback();
            return new CypherComparison(solutionResult, submissionResult, this.validator.containsOrderBy(solution));
        }
    }

    public GraphSnapshot extractGraph(String setupStatements) {
        this.validator.validateSetupScript(setupStatements);

        try (Session session = this.driver.session(this.sessionConfig());
             Transaction tx = session.beginTransaction(this.transactionConfig())) {
            this.runSetup(tx, setupStatements);

            List<GraphSnapshot.Node> nodes = new ArrayList<>();
            boolean nodesTruncated = false;
            Result nodeResult = tx.run("MATCH (n) RETURN elementId(n) AS id, labels(n) AS labels, properties(n) AS props");
            while (nodeResult.hasNext()) {
                if (nodes.size() >= MAX_GRAPH_NODES) {
                    nodesTruncated = true;
                    break;
                }
                Record record = nodeResult.next();
                nodes.add(new GraphSnapshot.Node(
                    record.get("id").asString(),
                    record.get("labels").asList(org.neo4j.driver.Value::asString),
                    record.get("props").asMap()));
            }
            nodeResult.consume();

            List<GraphSnapshot.Edge> edges = new ArrayList<>();
            boolean edgesTruncated = false;
            if (!nodesTruncated) {
                Result edgeResult = tx.run("MATCH (a)-[r]->(b) RETURN elementId(a) AS src, elementId(b) AS dst, type(r) AS type, properties(r) AS props");
                while (edgeResult.hasNext()) {
                    if (edges.size() >= MAX_GRAPH_EDGES) {
                        edgesTruncated = true;
                        break;
                    }
                    Record record = edgeResult.next();
                    edges.add(new GraphSnapshot.Edge(
                        record.get("src").asString(),
                        record.get("dst").asString(),
                        record.get("type").asString(),
                        record.get("props").asMap()));
                }
                edgeResult.consume();
            }

            tx.rollback();
            return new GraphSnapshot(List.copyOf(nodes), List.copyOf(edges), nodesTruncated || edgesTruncated);
        }
    }

    private SessionConfig sessionConfig() {
        return SessionConfig.builder()
            .withDatabase(this.properties.getDatabase())
            .withDefaultAccessMode(AccessMode.WRITE)
            .build();
    }

    private TransactionConfig transactionConfig() {
        return TransactionConfig.builder()
            .withTimeout(this.properties.getQueryTimeout())
            .build();
    }

    private void runSetup(Transaction tx, String setupStatements) {
        for (String statement : this.splitter.split(setupStatements)) {
            tx.run(statement).consume();
        }
    }

    private CypherQueryResult runQuery(Transaction tx, String query) {
        Result result = tx.run(this.splitter.split(query).getFirst());
        List<String> keys = result.keys();
        List<List<CanonicalValue>> rows = new ArrayList<>();
        while (result.hasNext()) {
            if (rows.size() >= this.properties.getMaxRows())
                throw new CypherResultLimitExceededException(this.properties.getMaxRows());

            Record record = result.next();
            rows.add(keys.stream()
                .map(key -> CanonicalCypherValue.from(record.get(key)))
                .toList());
        }
        return new CypherQueryResult(List.copyOf(keys), List.copyOf(rows));
    }
}
