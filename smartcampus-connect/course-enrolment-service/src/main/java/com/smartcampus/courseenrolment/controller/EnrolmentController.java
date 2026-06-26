// First Commit - Abdalla Hashim Ahmed Abdalla - B032320119
// git commit -m "Add Enrollment REST controller - Abdalla B032320119"
package com.smartcampus.courseenrolment.controller;

import com.smartcampus.courseenrolment.entity.Enrolment;
import com.smartcampus.courseenrolment.service.EnrolmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrolments")
public class EnrolmentController {

    private final EnrolmentService enrolmentService;

    public EnrolmentController(EnrolmentService enrolmentService) {
        this.enrolmentService = enrolmentService;
    }

    // GET all enrollments
    @GetMapping
    public ResponseEntity<List<Enrolment>> getAllEnrolments() {
        return ResponseEntity.ok(enrolmentService.getAllEnrolments());
    }

    // GET enrollments by student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrolment>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrolmentService.getEnrolmentsByStudent(studentId));
    }

    // GET enrollments by course
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Enrolment>> getByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrolmentService.getEnrolmentsByCourse(courseId));
    }

    // POST enroll student
    @PostMapping("/enrol")
    public ResponseEntity<Enrolment> enrolStudent(@RequestParam Long studentId,
                                                   @RequestParam Long courseId,
                                                   @RequestParam String semester) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrolmentService.enrolStudent(studentId, courseId, semester));
    }

    // PUT drop student
    @PutMapping("/drop")
    public ResponseEntity<Enrolment> dropStudent(@RequestParam Long studentId,
                                                  @RequestParam Long courseId,
                                                  @RequestParam String semester) {
        return ResponseEntity.ok(enrolmentService.dropStudent(studentId, courseId, semester));
    }
}