package twi;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.net.URI;
import java.util.Map;

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

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Bean
    ApplicationRunner debugEnv() {
        return args -> System.getenv().forEach((k, v) -> log.info('\t' + k + '=' + v));
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize
                        .matchers(EndpointRequest.toAnyEndpoint()).permitAll()
                        .anyExchange().authenticated()
                )
               // .redirectToHttps(Customizer.withDefaults())
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    HttpClientCustomizer httpClientCustomizer() {
        log.info("overriding the DNS resolver for the Reactor Netty HTTP Client");
        return hc -> hc.resolver(DefaultAddressResolverGroup.INSTANCE);
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
                        .filters(f -> f.tokenRelay().rewritePath("/api/(?<segment>.*)", "/$\\{segment}"))
                        .uri(api)
                )
                .route(rs -> rs.path("/**").uri(html))
                .build();
    }
}

@ConfigurationProperties(prefix = "twi.gateway")
record GatewayProperties(
        URI bookmarksApiUri,
        URI studioClientUri) {
}