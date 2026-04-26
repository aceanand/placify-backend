package com.placify.controller;

import com.placify.model.Job;
import com.placify.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:5173")
public class JobController {
    
    @Autowired
    private JobRepository jobRepository;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Job createJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job jobDetails) {
        return jobRepository.findById(id)
                .map(job -> {
                    job.setTitle(jobDetails.getTitle());
                    job.setCompany(jobDetails.getCompany());
                    job.setSalary(jobDetails.getSalary());
                    job.setDeadline(jobDetails.getDeadline());
                    job.setStatus(jobDetails.getStatus());
                    return ResponseEntity.ok(jobRepository.save(job));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(job -> {
                    jobRepository.delete(job);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
