package com.medsys.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long start = System.currentTimeMillis();
            String method = exchange.getRequest().getMethod().name();
            String path   = exchange.getRequest().getURI().getPath();

            return chain.filter(exchange).doFinally(signal -> {
                long duration = System.currentTimeMillis() - start;
                int status = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 0;
                log.info("[GATEWAY] {} {} → {} ({}ms)", method, path, status, duration);
            });
        };
    }

    public static class Config {}
}
