package at.jku.dke.task_app.cypher.config;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    public WebConfig() {
    }

    @Bean
    public HttpExchangeRepository httpTraceRepository() {
        var repo = new InMemoryHttpExchangeRepository();
        repo.setCapacity(500);
        return repo;
    }
}
