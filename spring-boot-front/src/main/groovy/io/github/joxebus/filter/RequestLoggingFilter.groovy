package io.github.joxebus.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter that adds request-scoped context to all logs using MDC (Mapped Diagnostic Context).
 * This allows tracking all logs for a specific request using the request_id field.
 */
@Component
class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        try {
            // Add request ID to all logs for this request
            MDC.put("request_id", UUID.randomUUID().toString())
            MDC.put("user_ip", request.getRemoteAddr())
            MDC.put("http_method", request.getMethod())
            MDC.put("request_uri", request.getRequestURI())

            filterChain.doFilter(request, response)
        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.clear()
        }
    }
}
