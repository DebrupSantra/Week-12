package com.acme.ecommerce.gateway;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedUserHeaderFilter implements GlobalFilter, Ordered {
    private static final String USER_HEADER = "X-Authenticated-User";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .map(userId -> mutateRequest(exchange, userId))
                .defaultIfEmpty(mutateRequest(exchange, null))
                .flatMap(request -> chain.filter(exchange.mutate().request(request).build()));
    }

    private ServerHttpRequest mutateRequest(ServerWebExchange exchange, String userId) {
        return exchange.getRequest().mutate().headers(headers -> {
            headers.remove(USER_HEADER);
            if (userId != null) headers.set(USER_HEADER, userId);
        }).build();
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
