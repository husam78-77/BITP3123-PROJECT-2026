// First Commit - Abdalla Hashim Ahmed Abdalla - B032320119
// git commit -m "Add Course REST controller - Abdalla B032320119"
package com.smartcampus.courseenrolment.controller;

import com.smartcampus.courseenrolment.entity.Course;
import com.smartcampus.courseenrolment.service.EnrolmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final EnrolmentService enrolmentService;

    public CourseController(EnrolmentService enrolmentService) {
        this.enrolmentService = enrolmentService;
    }

    // GET all courses
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(enrolmentService.getAllCourses());
    }

    // GET course by ID
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(enrolmentService.getCourseById(id));
    }

    // POST create course
    @PostMapping
    public ResponseEntity<Course> createCourse(@Valid @RequestBody Course course) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrolmentService.createCourse(course));
    }

    // PUT update course
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @Valid @RequestBody Course course) {
        return ResponseEntity.ok(enrolmentService.updateCourse(id, course));
    }

    // DELETE course
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCourse(@PathVariable Long id) {
        enrolmentService.deleteCourse(id);
        return ResponseEntity.ok("Course deleted successfully");
    }
}