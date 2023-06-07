package twi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.time.Instant;

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
    ApplicationRunner health() {
        return args -> {
            var tmp = new File("/tmp/");
            var health = new File(tmp, "health");
            try (var out = new FileWriter(health)) {
                var message = "initialized @ " + Instant.now();
                tmp.mkdirs();
                FileCopyUtils.copy(message, out);
                log.info(message + "::" +  health.exists());
            }
        };
    }

    @Bean
    RouteLocator gateway(GatewayProperties gp, RouteLocatorBuilder rlb) {
        var api = gp.bookmarksApiUri();
        var html = gp.studioClientUri();
        var logger = LoggerFactory.getLogger(getClass());
        logger.info("forwarding /api/* to " + gp.bookmarksApiUri() + " and / to " + gp.studioClientUri());
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