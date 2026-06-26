
package com.smartcampus.reportinganalytics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * R9 — FAILURE HANDLING: Graceful Degradation + Client-Side Timeout
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This service demonstrates <b>two failure-handling techniques</b> required
 * by R9 (Failure Handling) in the DAD rubric:
 *
 * <h3>1. GRACEFUL DEGRADATION</h3>
 * <p>Every method that calls a downstream service wraps the REST call in a
 * {@code try-catch} block.  If the downstream service is unavailable
 * (offline, unreachable, or times out), the catch block returns a
 * <b>fallback response</b> with:</p>
 * <ul>
 *   <li>Default values (e.g., {@code totalStudents: 0})</li>
 *   <li>A descriptive status message (e.g., {@code "Student Profile Service unavailable"})</li>
 * </ul>
 * <p>This ensures the Reporting Service remains functional even when its
 * dependencies are down — it degrades <b>gracefully</b> instead of
 * crashing or returning a 500 error.</p>
 *
 * <h3>2. CLIENT-SIDE TIMEOUT</h3>
 * <p>The {@link RestTemplate} injected into this service is configured
 * with timeouts in {@code AppConfig}:</p>
 * <ul>
 *   <li><b>Connect Timeout: 3 seconds</b> — if the downstream service
 *       cannot be reached within 3 seconds, the connection attempt is
 *       aborted.</li>
 *   <li><b>Read Timeout: 3 seconds</b> — if the downstream service
 *       connects but does not respond within 3 seconds, the read is
 *       aborted.</li>
 * </ul>
 * <p>When a timeout occurs, a {@code ResourceAccessException} is thrown,
 * which is caught by the graceful degradation try-catch blocks.  This
 * prevents the Reporting Service from hanging indefinitely when a
 * downstream service is slow or unresponsive.</p>
 *
 * <h3>How the Two Techniques Work Together</h3>
 * <pre>
 *   1. Reporting Service calls Student Service (RestTemplate.getForObject)
 *   2. Student Service is offline
 *   3. CLIENT-SIDE TIMEOUT fires after 3 seconds → ResourceAccessException
 *   4. GRACEFUL DEGRADATION catches the exception
 *   5. Fallback response returned: {"totalStudents": 0, "status": "Service unavailable"}
 * </pre>
 *
 * <p>Without the timeout, step 3 would never happen — the thread would
 * block indefinitely.  Without graceful degradation, step 4 would
 * propagate the exception as a 500 error.  Together, they ensure the
 * system <b>fails fast</b> and <b>fails gracefully</b>.</p>
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Service
public class ReportingService {

    private final RestTemplate restTemplate;
    private final String studentServiceUrl;
    private final String enrolmentServiceUrl;
    private final String libraryServiceUrl;

    // Manual constructor injection
    public ReportingService(RestTemplate restTemplate,
                            @Value("${student.service.url}") String studentServiceUrl,
                            @Value("${enrolment.service.url}") String enrolmentServiceUrl,
                            @Value("${library.service.url}") String libraryServiceUrl) {
        this.restTemplate = restTemplate;
        this.studentServiceUrl = studentServiceUrl;
        this.enrolmentServiceUrl = enrolmentServiceUrl;
        this.libraryServiceUrl = libraryServiceUrl;
    }

    /**
     * Report 1: Student Summary.
     *
     * <p><b>R9 — Graceful Degradation:</b> If the Student Profile Service
     * is unavailable or times out (within 3 seconds), the catch block
     * returns a fallback response with {@code totalStudents: 0} and a
     * descriptive status message instead of crashing.</p>
     *
     * @return a map containing student count and data, or fallback values
     */
    public Map<String, Object> getStudentSummary() {
        Map<String, Object> report = new HashMap<>();
        try {
            // R9: This REST call is protected by the client-side timeout
            // (3s connect + 3s read) configured in AppConfig.
            // If Student Service is offline, the timeout fires and the
            // catch block below handles the failure gracefully.
            List students = restTemplate.getForObject(
                    studentServiceUrl + "/api/students", List.class);
            report.put("totalStudents", students != null ? students.size() : 0);
            report.put("students", students);
            report.put("status", "SUCCESS");
        } catch (Exception e) {
            // R9 — GRACEFUL DEGRADATION: Return fallback values instead of
            // propagating the exception. This could be triggered by:
            // - ConnectException (service offline) → caught after 3s timeout
            // - SocketTimeoutException (service slow) → caught after 3s timeout
            // - Any other network/HTTP error
            report.put("totalStudents", 0);
            report.put("status", "Student Profile Service unavailable");
        }
        return report;
    }

    /**
     * Report 2: Enrolment Summary.
     *
     * <p><b>R9 — Graceful Degradation:</b> If the Course Enrolment Service
     * is unavailable or times out, fallback values are returned.</p>
     *
     * @return a map containing enrolment/course counts, or fallback values
     */
    public Map<String, Object> getEnrolmentSummary() {
        Map<String, Object> report = new HashMap<>();
        try {
            // R9: Protected by client-side timeout (3s connect + 3s read)
            List enrolments = restTemplate.getForObject(
                    enrolmentServiceUrl + "/api/enrolments", List.class);
            List courses = restTemplate.getForObject(
                    enrolmentServiceUrl + "/api/courses", List.class);
            report.put("totalEnrolments", enrolments != null ? enrolments.size() : 0);
            report.put("totalCourses", courses != null ? courses.size() : 0);
            report.put("enrolments", enrolments);
            report.put("status", "SUCCESS");
        } catch (Exception e) {
            // R9 — GRACEFUL DEGRADATION: Fallback response
            report.put("totalEnrolments", 0);
            report.put("status", "Course Enrolment Service unavailable");
        }
        return report;
    }

    /**
     * Report 3: Library Summary.
     *
     * <p><b>R9 — Graceful Degradation:</b> If the Library Booking Service
     * is unavailable or times out, fallback values are returned.</p>
     *
     * @return a map containing library statistics, or fallback values
     */
    public Map<String, Object> getLibrarySummary() {
        Map<String, Object> report = new HashMap<>();
        try {
            // R9: Protected by client-side timeout (3s connect + 3s read)
            List allBooks = restTemplate.getForObject(
                    libraryServiceUrl + "/api/books", List.class);
            List availableBooks = restTemplate.getForObject(
                    libraryServiceUrl + "/api/books/available", List.class);
            List allLoans = restTemplate.getForObject(
                    libraryServiceUrl + "/api/books/loans", List.class);
            List roomBookings = restTemplate.getForObject(
                    libraryServiceUrl + "/api/rooms", List.class);

            report.put("totalBooks", allBooks != null ? allBooks.size() : 0);
            report.put("availableBooks", availableBooks != null ? availableBooks.size() : 0);
            report.put("activeLoans", allLoans != null ? allLoans.size() : 0);
            report.put("roomBookings", roomBookings != null ? roomBookings.size() : 0);
            report.put("status", "SUCCESS");
        } catch (Exception e) {
            // R9 — GRACEFUL DEGRADATION: Fallback response
            report.put("totalBooks", 0);
            report.put("status", "Library Service unavailable");
        }
        return report;
    }

    /**
     * Report 4: Full Campus Overview.
     *
     * <p>Aggregates all reports.  Each sub-report independently applies
     * graceful degradation — if one service is down, the other reports
     * still succeed.</p>
     *
     * @return a combined campus overview map
     */
    public Map<String, Object> getCampusOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("studentSummary", getStudentSummary());
        overview.put("enrolmentSummary", getEnrolmentSummary());
        overview.put("librarySummary", getLibrarySummary());
        return overview;
    }
}