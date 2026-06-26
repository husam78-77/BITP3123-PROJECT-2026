
package com.smartcampus.reportinganalytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * R9 — FAILURE HANDLING: Client-Side Timeout Configuration
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This configuration class implements <b>Client-Side Timeout</b> for all
 * outgoing HTTP calls made by the Reporting Service via {@link RestTemplate}.
 *
 * <h3>What is Client-Side Timeout?</h3>
 * <p>A client-side timeout sets a maximum duration the calling service
 * (the <i>client</i>) will wait for a response from a downstream service
 * (the <i>server</i>).  If the downstream service does not respond within
 * the configured time limit, the request is aborted and a
 * {@link org.springframework.web.client.ResourceAccessException} is thrown,
 * which is then handled by the Reporting Service's graceful degradation
 * logic (try-catch blocks in {@code ReportingService}).</p>
 *
 * <h3>Why Distributed Systems Require Timeouts</h3>
 * <p>In a microservices architecture, services communicate over the
 * network.  Networks are inherently unreliable — a downstream service
 * may be slow, overloaded, or completely offline.  Without timeouts:</p>
 * <ul>
 *   <li>The calling thread <b>blocks indefinitely</b>, waiting for a
 *       response that may never arrive.</li>
 *   <li>Thread pool resources are <b>exhausted</b> as more threads pile
 *       up waiting for unresponsive services.</li>
 *   <li>The calling service itself <b>becomes unresponsive</b> (cascading
 *       failure), even though its own logic is healthy.</li>
 * </ul>
 * <p>Timeouts act as a <b>circuit breaker</b> for network calls — they
 * guarantee that no single slow dependency can cripple the entire system.</p>
 *
 * <h3>Two Types of Timeout</h3>
 * <table>
 *   <tr><th>Timeout</th><th>What It Controls</th><th>Configured Value</th></tr>
 *   <tr>
 *     <td><b>Connect Timeout</b></td>
 *     <td>Maximum time to establish a TCP connection with the downstream
 *         service.  If the service is offline or the network is unreachable,
 *         this timeout fires.</td>
 *     <td>3000 ms (3 seconds)</td>
 *   </tr>
 *   <tr>
 *     <td><b>Read Timeout</b></td>
 *     <td>Maximum time to wait for data after the connection is established.
 *         If the downstream service accepts the connection but takes too
 *         long to respond (e.g., slow database query), this timeout fires.</td>
 *     <td>3000 ms (3 seconds)</td>
 *   </tr>
 * </table>
 *
 * <h3>Difference: Timeout vs Graceful Degradation</h3>
 * <ul>
 *   <li><b>Client-Side Timeout</b> — a preventive mechanism that
 *       <b>limits how long</b> the system waits before giving up.
 *       It ensures the system fails <b>fast</b> rather than hanging.</li>
 *   <li><b>Graceful Degradation</b> — a reactive mechanism that
 *       <b>handles the failure</b> once it occurs (whether caused by a
 *       timeout, connection refusal, or any other exception).  It returns
 *       a meaningful fallback response instead of propagating the error.</li>
 * </ul>
 * <p>Together, they form a complete failure-handling strategy:
 * timeouts ensure fast failure, and graceful degradation ensures
 * the failure is handled gracefully.</p>
 *
 * <h3>How This Satisfies R9</h3>
 * <p>R9 requires demonstrating at least two failure-handling techniques:
 * </p>
 * <ol>
 *   <li><b>Graceful Degradation</b> — implemented in
 *       {@code ReportingService} via try-catch blocks that return
 *       fallback responses when downstream services are unavailable.</li>
 *   <li><b>Client-Side Timeout</b> — implemented here via
 *       {@link RestTemplateBuilder#setConnectTimeout(Duration)} and
 *       {@link RestTemplateBuilder#setReadTimeout(Duration)}, ensuring
 *       requests fail fast (within 3 seconds) rather than blocking
 *       indefinitely.</li>
 * </ol>
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Configuration
public class AppConfig {

    /**
     * R9 — CLIENT-SIDE TIMEOUT: Configures RestTemplate with timeouts.
     *
     * <p>Uses Spring Boot's {@link RestTemplateBuilder} to configure
     * both connection and read timeouts.  This is the recommended
     * approach in Spring Boot 3 (avoids deprecated APIs).</p>
     *
     * <ul>
     *   <li><b>Connect Timeout (3s)</b> — if the downstream service
     *       cannot be reached within 3 seconds, the request is aborted
     *       with a {@code ResourceAccessException}.</li>
     *   <li><b>Read Timeout (3s)</b> — if the downstream service
     *       accepts the connection but does not send a response within
     *       3 seconds, the request is aborted.</li>
     * </ul>
     *
     * <p>Timeout values are read from {@code application.properties}:</p>
     * <ul>
     *   <li>{@code reporting.timeout.connect=3000}</li>
     *   <li>{@code reporting.timeout.read=3000}</li>
     * </ul>
     *
     * @param builder        the Spring Boot auto-configured RestTemplateBuilder
     * @param connectTimeout connect timeout in milliseconds from properties
     * @param readTimeout    read timeout in milliseconds from properties
     * @return a RestTemplate instance with timeout protection
     */
    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${reporting.timeout.connect}") int connectTimeout,
            @Value("${reporting.timeout.read}") int readTimeout) {
        return builder
                // R9: Connect Timeout — max time to establish TCP connection
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                // R9: Read Timeout — max time to wait for response data
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}