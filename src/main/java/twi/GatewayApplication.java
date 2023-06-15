package twi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.R2dbcReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.net.URI;
import java.util.Map;

/**
 * This is a proxy to both the backend bookmark-api and the static HTML
 * site that provides the experience for this system.
 *
 * @author Josh Long
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

    @Bean
    ApplicationRunner applicationRunner() {
        return e -> System.getenv().forEach((k, v) -> log.info("header: " + k));
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize
                        .matchers(EndpointRequest.toAnyEndpoint()).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    RouteLocator gateway(GatewayProperties gatewayProperties, RouteLocatorBuilder rlb) {
        var api = gatewayProperties.bookmarksApiUri();
        var html = gatewayProperties.studioClientUri();
        Map.of("/api/*", api, "/", html).forEach((k, v) -> log.info("forwarding [" + k + "] to [" + v + "]"));
        return rlb
                .routes()
                .route(rs -> rs
                        .path("/api/**")
                        .filters(f -> f
                                .tokenRelay()
                                .rewritePath("/api/(?<segment>.*)", "/$\\{segment}")
                        )
                        .uri(api)
                )
                .route(rs -> rs
                        .path("/**")
                        .uri(html))
                .build();
    }

    @Bean
    R2dbcReactiveOAuth2AuthorizedClientService r2dbcReactiveOAuth2AuthorizedClientService(
            DatabaseClient dc,
            ReactiveClientRegistrationRepository repository) {
        return new R2dbcReactiveOAuth2AuthorizedClientService(dc, repository);
    }
}

@ConfigurationProperties(prefix = "twi.gateway")
record GatewayProperties(
        URI bookmarksApiUri,
        URI studioClientUri) {
}