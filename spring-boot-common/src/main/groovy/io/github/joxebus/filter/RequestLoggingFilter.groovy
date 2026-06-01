package io.github.joxebus.filter

import groovy.util.logging.Slf4j
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Centralized filter that adds request-scoped context to all logs using MDC (Mapped Diagnostic Context).
 *
 * This filter supports distributed tracing by:
 * 1. Checking for propagated MDC headers from upstream services (e.g., Frontend → Backend)
 * 2. If headers exist, uses them to maintain the same request_id across services
 * 3. If headers don't exist, generates new values (direct service call)
 *
 * MDC Fields:
 * - request_id: Unique identifier for tracking requests across services
 * - user_ip: Original client IP address
 * - http_method: Original HTTP method from client request
 * - request_uri: Original request URI from client
 *
 * HTTP Headers Used:
 * - X-Request-ID: Standard header for request tracking (RFC)
 * - X-Person-User-IP: Custom header for preserving original user IP
 * - X-Person-HTTP-Method: Custom header for preserving original HTTP method
 * - X-Person-Request-URI: Custom header for preserving original request URI
 */
@Component
@Slf4j
class RequestLoggingFilter extends OncePerRequestFilter {

    // Header constants for MDC propagation
    static final String HEADER_REQUEST_ID = "X-Request-ID"
    static final String HEADER_USER_IP = "X-Person-User-IP"
    static final String HEADER_HTTP_METHOD = "X-Person-HTTP-Method"
    static final String HEADER_REQUEST_URI = "X-Person-Request-URI"

    // MDC key constants
    static final String MDC_REQUEST_ID = "request_id"
    static final String MDC_USER_IP = "user_ip"
    static final String MDC_HTTP_METHOD = "http_method"
    static final String MDC_REQUEST_URI = "request_uri"

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        try {
            if(request.getRequestURI().startsWith("/actuator/")) {
                // Skip MDC setup for health checks to reduce log noise
                filterChain.doFilter(request, response)
                return
            }
            // Use Elvis operator to either use propagated headers or generate/extract new values
            String requestId = request.getHeader(HEADER_REQUEST_ID) ?: UUID.randomUUID().toString()

            MDC.put(MDC_REQUEST_ID, requestId)
            MDC.put(MDC_USER_IP, request.getHeader(HEADER_USER_IP) ?: request.remoteAddr)
            MDC.put(MDC_HTTP_METHOD, request.getHeader(HEADER_HTTP_METHOD) ?: request.method)
            MDC.put(MDC_REQUEST_URI, request.getHeader(HEADER_REQUEST_URI) ?: request.requestURI)

            // Single log statement using GString for conditional message
            String source = request.getHeader(HEADER_REQUEST_ID) ? "upstream service" : "direct request"
            log.trace("${request.getHeader(HEADER_REQUEST_ID) ? 'Received propagated' : 'Generated new'} request_id: ${requestId} ${request.getHeader(HEADER_REQUEST_ID) ? 'from' : 'for'} ${source}")

            // Continue filter chain
            filterChain.doFilter(request, response)
        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.clear()
        }
    }
}
