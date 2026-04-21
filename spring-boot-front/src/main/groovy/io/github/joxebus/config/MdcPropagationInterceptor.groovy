package io.github.joxebus.config

import groovy.util.logging.Slf4j
import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

/**
 * RestTemplate interceptor that propagates MDC (Mapped Diagnostic Context) values
 * from the Frontend to Backend services via HTTP headers.
 *
 * This enables distributed tracing where a single request_id can be tracked across
 * multiple microservices:
 * Browser → Frontend (generates request_id) → Backend (receives same request_id)
 *
 * MDC Fields Propagated:
 * - request_id → X-Request-ID
 * - user_ip → X-Person-User-IP
 * - http_method → X-Person-HTTP-Method
 * - request_uri → X-Person-Request-URI
 *
 * Usage:
 * This interceptor is automatically registered with the RestTemplate bean in InitConfiguration.
 * No manual intervention needed - propagation happens automatically for all RestTemplate calls.
 *
 * Example Flow:
 * 1. Browser makes GET request to Frontend: /people
 * 2. RequestLoggingFilter sets MDC: request_id=abc-123, user_ip=192.168.1.1, http_method=GET, request_uri=/people
 * 3. Frontend calls Backend via RestTemplate: /people
 * 4. This interceptor adds headers: X-Request-ID=abc-123, X-Person-User-IP=192.168.1.1, etc.
 * 5. Backend receives request with headers
 * 6. Backend's RequestLoggingFilter reads headers and sets same MDC values
 * 7. Kibana query for request_id=abc-123 shows logs from BOTH Frontend and Backend
 */
@Component
@Slf4j
class MdcPropagationInterceptor implements ClientHttpRequestInterceptor {

    // Header constants (must match RequestLoggingFilter)
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
    ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // Read MDC values set by RequestLoggingFilter
        String requestId = MDC.get(MDC_REQUEST_ID)
        String userIp = MDC.get(MDC_USER_IP)
        String httpMethod = MDC.get(MDC_HTTP_METHOD)
        String requestUri = MDC.get(MDC_REQUEST_URI)

        // Propagate MDC values as HTTP headers to backend service
        if (requestId) {
            request.headers.set(HEADER_REQUEST_ID, requestId)
            log.debug("Propagating request_id: {} to backend service", requestId)
        }

        if (userIp) {
            request.headers.set(HEADER_USER_IP, userIp)
        }

        if (httpMethod) {
            request.headers.set(HEADER_HTTP_METHOD, httpMethod)
        }

        if (requestUri) {
            request.headers.set(HEADER_REQUEST_URI, requestUri)
        }

        // Continue with the request
        return execution.execute(request, body)
    }
}
