# eTutor Task-App: Cypher

This application provides a REST interface for tasks of type `cypher`.

Teachers define Neo4j Cypher setup statements in the task group and a read-only Neo4j Cypher solution query in the task. Students submit read-only Cypher queries. The evaluator builds the graph in a rollback-only Neo4j transaction, executes the teacher and student queries, compares the returned keys and rows, and rolls back the graph state.

## Development

In development environment, the API documentation is available at http://localhost:8081/docs.

The `dev` profile is active by default. Start the local PostgreSQL and Neo4j dependencies with:

```bash
docker compose up -d
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## Docker

Start a new instance of the application using Docker:

```bash
docker run -p 8090:8081 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://postgres:5432/etutor_cypher" \
  -e SPRING_DATASOURCE_USERNAME=etutor_cypher \
  -e SPRING_DATASOURCE_PASSWORD=myPwd \
  -e SPRING_FLYWAY_USER=etutor_cypher_admin \
  -e SPRING_FLYWAY_PASSWORD=adPwd \
  -e CYPHER_NEO4J_URI=bolt://neo4j:7687 \
  -e CYPHER_NEO4J_USERNAME=neo4j \
  -e CYPHER_NEO4J_PASSWORD=secretpassword \
  -e CYPHER_NEO4J_DATABASE=neo4j \
  -e CLIENTS_API_KEYS_0_NAME=task-administration \
  -e CLIENTS_API_KEYS_0_KEY=some-secret-key \
  -e CLIENTS_API_KEYS_0_ROLES_0=CRUD \
  -e CLIENTS_API_KEYS_0_ROLES_1=SUBMIT \
  -e CLIENTS_API_KEYS_1_NAME=moodle \
  -e CLIENTS_API_KEYS_1_KEY=another-secret-key \
  -e CLIENTS_API_KEYS_1_ROLES_0=SUBMIT \
  -e CLIENTS_API_KEYS_2_NAME=plagiarism-checker \
  -e CLIENTS_API_KEYS_2_KEY=key-for-reading-submissions \
  -e CLIENTS_API_KEYS_2_ROLES_0=READ_SUBMISSION \
  etutorplusplus/task-app-cypher
```

or with Docker Compose:

```yaml
version: '3.8'

services:
    task-app-cypher:
        image: etutorplusplus/task-app-cypher
        restart: unless-stopped
        ports:
            -   target: 8081
                published: 8090
        environment:
            SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/etutor_cypher
            SPRING_DATASOURCE_USERNAME: etutor_cypher
            SPRING_DATASOURCE_PASSWORD: myPwd
            SPRING_FLYWAY_USER: etutor_cypher_admin
            SPRING_FLYWAY_PASSWORD: adPwd
            CYPHER_NEO4J_URI: bolt://neo4j:7687
            CYPHER_NEO4J_USERNAME: neo4j
            CYPHER_NEO4J_PASSWORD: secretpassword
            CYPHER_NEO4J_DATABASE: neo4j
            CLIENTS_API_KEYS_0_NAME: task-administration
            CLIENTS_API_KEYS_0_KEY: some-secret-key
            CLIENTS_API_KEYS_0_ROLES_0: CRUD
            CLIENTS_API_KEYS_0_ROLES_1: SUBMIT
            CLIENTS_API_KEYS_1_NAME: moodle
            CLIENTS_API_KEYS_1_KEY: another-secret-key
            CLIENTS_API_KEYS_1_ROLES_0: SUBMIT
            CLIENTS_API_KEYS_2_NAME: plagiarism-checker
            CLIENTS_API_KEYS_2_KEY: key-for-reading-submissions
            CLIENTS_API_KEYS_2_ROLES_0: READ_SUBMISSION
```

### Environment Variables

In production environment, the application requires a PostgreSQL database for task-app metadata and a Neo4j database for query evaluation.

| Variable                     | Description                                              |
|------------------------------|----------------------------------------------------------|
| `SERVER_PORT`                | The server port.                                         |
| `SPRING_DATASOURCE_URL`      | JDBC URL to the PostgreSQL metadata database.            |
| `SPRING_DATASOURCE_USERNAME` | The PostgreSQL JPA username.                             |
| `SPRING_DATASOURCE_PASSWORD` | The PostgreSQL JPA password.                             |
| `SPRING_FLYWAY_USER`         | The PostgreSQL migration administrator username.         |
| `SPRING_FLYWAY_PASSWORD`     | The PostgreSQL migration administrator password.         |
| `CYPHER_NEO4J_URI`           | Neo4j Bolt URI.                                          |
| `CYPHER_NEO4J_USERNAME`      | Neo4j username.                                          |
| `CYPHER_NEO4J_PASSWORD`      | Neo4j password.                                          |
| `CYPHER_NEO4J_DATABASE`      | Neo4j database name.                                     |
| `CYPHER_NEO4J_QUERY_TIMEOUT` | Maximum time for one Neo4j transaction, e.g. `10s`.      |
| `CYPHER_NEO4J_MAX_ROWS`      | Maximum rows collected per solution/submission query.    |
| `CLIENTS_API_KEYS_X_NAME`    | The name of the client.                                  |
| `CLIENTS_API_KEYS_X_KEY`     | The API key of the client.                               |
| `CLIENTS_API_KEYS_X_ROLES_Y` | The role of the client.                                  |
