package com.example.springbootwebfluxwebclientnettyleak;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Configuration
public class RouterConfig {

  @Bean
  public RouterFunction<ServerResponse> routes() {
    return RouterFunctions.nest(
            path("/api"),
            route(GET("/test"), this::test));
  }

  public Mono<ServerResponse> test(ServerRequest request) {
    Mono<Void> exchangeA =
        WebClient.create("http://localhost:8008")
            .get()
            .uri(uriBuilder -> uriBuilder.path("/delay/5").build())
            .retrieve()
            .bodyToFlux(DataBuffer.class)
            .map(DataBufferUtils::release)
            .then();

    Mono<Void> exchangeB =
        WebClient.create("http://localhost:8008")
            .get()
            .uri(uriBuilder -> uriBuilder.path("/status/200").build())
            .retrieve()
            .bodyToFlux(DataBuffer.class)
            .map(DataBufferUtils::release)
            .then();

    return Mono.first(exchangeA, exchangeB).flatMap(aVoid -> ServerResponse.ok().build());
  }
}
