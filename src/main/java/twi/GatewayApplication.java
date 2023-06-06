package twi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * this is a proxy to both the backend bookmark-api and the static HTML
 * site that provides the experience for this system.
 *
 * @author Josh Long
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    RouteLocator gateway(GatewayProperties gp, RouteLocatorBuilder rlb)  {
        var api = determineUri(gp, GatewayProperties::bookmarksApiUri, () -> URI.create("http://localhost:8081"));
        var html = determineUri(gp, GatewayProperties::htmlUri, () -> URI.create("http://localhost:5173"));
        return rlb
                .routes()
                .route(rs -> rs
                        .path("/api/**")
                        .filters(f -> f.tokenRelay().rewritePath("/api/(?<segment>.*)", "/$\\{segment}"))
                        .uri(api))
                .route(rs -> rs.path("/**").uri(html))
                .build();
    }

    private static URI determineUri(GatewayProperties g, Function<GatewayProperties, URI> u, Supplier<URI> defaultUri) {
        var result = u.apply(g);
        return result != null ? result : defaultUri.get();
    }
}

@ConfigurationProperties(prefix = "twi.gateway")
record GatewayProperties(
        URI bookmarksApiUri,
        URI htmlUri) {
}