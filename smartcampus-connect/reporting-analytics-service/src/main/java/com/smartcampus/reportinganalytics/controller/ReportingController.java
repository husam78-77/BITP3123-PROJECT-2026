
package com.smartcampus.reportinganalytics.controller;

import com.smartcampus.reportinganalytics.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

 private final ReportingService reportingService;

 public ReportingController(ReportingService reportingService) {
     this.reportingService = reportingService;
 }

 // GET student summary
 @GetMapping("/students")
 public ResponseEntity<Map<String, Object>> getStudentSummary() {
     return ResponseEntity.ok(reportingService.getStudentSummary());
 }

 // GET enrolment summary
 @GetMapping("/enrolments")
 public ResponseEntity<Map<String, Object>> getEnrolmentSummary() {
     return ResponseEntity.ok(reportingService.getEnrolmentSummary());
 }

 // GET library summary
 @GetMapping("/library")
 public ResponseEntity<Map<String, Object>> getLibrarySummary() {
     return ResponseEntity.ok(reportingService.getLibrarySummary());
 }

 // GET full campus overview
 @GetMapping("/overview")
 public ResponseEntity<Map<String, Object>> getCampusOverview() {
     return ResponseEntity.ok(reportingService.getCampusOverview());
 }
}