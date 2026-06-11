package at.jku.dke.task_app.cypher.config;

import at.jku.dke.etutor.task_app.config.BaseOpenApiConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig extends BaseOpenApiConfig {
    public OpenApiConfig() {
        super("eTutor - cypher API", "API for tasks of type <code>cypher</code>", OpenApiConfig.class.getPackage().getImplementationVersion());
    }
}
