// First Commit - Abdalla Hashim Ahmed Abdalla - B032320119
// git commit -m "Add Enrolment service logic - Abdalla B032320119"
package com.smartcampus.courseenrolment.service;

import com.smartcampus.courseenrolment.entity.Course;
import com.smartcampus.courseenrolment.entity.Enrolment;
import com.smartcampus.courseenrolment.repository.CourseRepository;
import com.smartcampus.courseenrolment.repository.EnrolmentRepository;
import com.smartcampus.courseenrolment.socket.NotificationProducerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * R6 — DISTRIBUTED MESSAGING: Producer (Course Enrolment Service)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This service acts as a <b>Producer</b> in the Producer–Consumer
 * architecture.  After a student enrolment is successfully persisted,
 * an asynchronous TCP notification is sent to the Notification Service
 * (Consumer) via the {@link NotificationProducerClient}.
 *
 * <h3>Asynchronous Workflow</h3>
 * <pre>
 *   1. Client sends POST /api/enrolments/enrol
 *   2. EnrolmentService.enrolStudent() validates and saves the enrolment
 *   3. EnrolmentService calls notificationProducer.sendAsync("ENROLMENT", ...)
 *   4. The TCP message is sent on a BACKGROUND THREAD (CompletableFuture)
 *   5. The HTTP response is returned IMMEDIATELY (non-blocking)
 *   6. Meanwhile, NotificationSocketServer receives and persists the notification
 * </pre>
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Service
public class EnrolmentService {

    private final EnrolmentRepository enrolmentRepository;
    private final CourseRepository courseRepository;
    private final RestTemplate restTemplate;
    private final String studentServiceUrl;

    /**
     * R6 — TCP Socket Producer Client.
     * Injected via constructor injection.  Used to send asynchronous
     * notification messages over TCP after enrolment events.
     */
    private final NotificationProducerClient notificationProducer;

    // Manual constructor injection
    public EnrolmentService(
            EnrolmentRepository enrolmentRepository,
            CourseRepository courseRepository,
            RestTemplate restTemplate,
            @Value("${student.service.url}") String studentServiceUrl,
            NotificationProducerClient notificationProducer) {

        this.enrolmentRepository = enrolmentRepository;
        this.courseRepository = courseRepository;
        this.restTemplate = restTemplate;
        this.studentServiceUrl = studentServiceUrl;
        this.notificationProducer = notificationProducer;
    }

    // Validate student exists via REST call to Student Profile Service
    private void validateStudent(Long studentId) {
        try {
            restTemplate.getForObject(studentServiceUrl + "/api/students/" + studentId, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Student with ID " + studentId + " not found in Student Profile Service");
        }
    }

    /**
     * Enrols a student in a course and sends an asynchronous TCP
     * notification to the Notification Service.
     *
     * <p><b>R6 — Producer action:</b> After the enrolment is saved,
     * {@code notificationProducer.sendAsync()} sends a TCP message
     * using the custom protocol format:</p>
     * <pre>
     *   ENROLMENT:2026-06-10T10:30:00 Student 1 enrolled in DAD3123
     * </pre>
     * <p>The send is asynchronous (CompletableFuture) — the HTTP
     * response is not delayed.</p>
     */
    public Enrolment enrolStudent(Long studentId, Long courseId, String semester) {
        validateStudent(studentId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

        // Capacity check
        if (course.getCurrentEnrolled() >= course.getCapacity()) {
            throw new RuntimeException("Course " + course.getCourseCode() + " is full");
        }

        // Duplicate check
        enrolmentRepository.findByStudentIdAndCourseIdAndSemester(studentId, courseId, semester)
                .ifPresent(e -> { throw new RuntimeException("Student is already enrolled in this course for this semester"); });

        // Increment enrolment count
        course.setCurrentEnrolled(course.getCurrentEnrolled() + 1);
        courseRepository.save(course);

        Enrolment enrolment = new Enrolment(studentId, courseId, semester, "ENROLLED");
        Enrolment saved = enrolmentRepository.save(enrolment);

        // ── R6: ASYNCHRONOUS TCP NOTIFICATION (Producer → Consumer) ──────
        // Send the notification message to the Notification Service via TCP.
        // This call is NON-BLOCKING — it returns immediately while the
        // actual TCP send happens on a background thread.
        notificationProducer.sendAsync(
                "ENROLMENT",
                "Student " + studentId + " enrolled in " + course.getCourseCode()
        );

        return saved;
    }

    // Drop a student from a course
    public Enrolment dropStudent(Long studentId, Long courseId, String semester) {
        Enrolment enrolment = enrolmentRepository
                .findByStudentIdAndCourseIdAndSemester(studentId, courseId, semester)
                .orElseThrow(() -> new RuntimeException("Enrolment not found"));

        if ("DROPPED".equals(enrolment.getStatus())) {
            throw new RuntimeException("Student has already dropped this course");
        }

        enrolment.setStatus("DROPPED");

        // Decrement enrolment count
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setCurrentEnrolled(Math.max(0, course.getCurrentEnrolled() - 1));
        courseRepository.save(course);

        return enrolmentRepository.save(enrolment);
    }

    // Get all enrolments
    public List<Enrolment> getAllEnrolments() {
        return enrolmentRepository.findAll();
    }

    // Get enrolments by student
    public List<Enrolment> getEnrolmentsByStudent(Long studentId) {
        return enrolmentRepository.findByStudentId(studentId);
    }

    // Get enrolments by course
    public List<Enrolment> getEnrolmentsByCourse(Long courseId) {
        return enrolmentRepository.findByCourseId(courseId);
    }

    // Course CRUD
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + id));
    }

    public Course createCourse(Course course) {
        if (courseRepository.existsByCourseCode(course.getCourseCode())) {
            throw new RuntimeException("Course code already exists: " + course.getCourseCode());
        }
        course.setCurrentEnrolled(0);
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, Course updated) {
        Course existing = getCourseById(id);
        existing.setCourseName(updated.getCourseName());
        existing.setProgramme(updated.getProgramme());
        existing.setCapacity(updated.getCapacity());
        return courseRepository.save(existing);
    }

    public void deleteCourse(Long id) {
        courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + id));
        courseRepository.deleteById(id);
    }
}