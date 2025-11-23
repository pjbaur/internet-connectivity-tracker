package me.paulbaur.ict.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.paulbaur.ict.common.logging.LoggingContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Ensures every HTTP request has a request identifier available in MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "reqId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String incomingReqId = request.getHeader(REQUEST_ID_HEADER);
        String reqId = StringUtils.hasText(incomingReqId) ? incomingReqId : UUID.randomUUID().toString();

        try (LoggingContext ignored = LoggingContext.withValue(MDC_KEY, reqId)) {
            response.setHeader(REQUEST_ID_HEADER, reqId);
            filterChain.doFilter(request, response);
        }
    }
}
