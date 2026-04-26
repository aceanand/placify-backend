package com.placify.controller;

import com.placify.model.InterviewQuestion;
import com.placify.repository.InterviewQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview")
@CrossOrigin(origins = "http://localhost:5173")
public class InterviewController {

    @Autowired
    private InterviewQuestionRepository questionRepository;

    /** Get all distinct job roles */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<String>> getRoles() {
        return ResponseEntity.ok(questionRepository.findDistinctJobRoles());
    }

    /** Get all distinct round types */
    @GetMapping("/rounds")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<String>> getRounds() {
        return ResponseEntity.ok(questionRepository.findDistinctRoundTypes());
    }

    /** Get questions filtered by role and/or round */
    @GetMapping("/questions")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<InterviewQuestion>> getQuestions(
            @RequestParam(required = false) String jobRole,
            @RequestParam(required = false) String roundType
    ) {
        List<InterviewQuestion> questions;

        if (jobRole != null && roundType != null) {
            // Combine role-specific + "All Roles" for the given round
            questions = questionRepository.findByJobRoleAndRoundType(jobRole, roundType);
            questions.addAll(questionRepository.findByJobRoleAndRoundType("All Roles", roundType));
        } else if (jobRole != null) {
            questions = questionRepository.findByJobRole(jobRole);
            questions.addAll(questionRepository.findByJobRole("All Roles"));
        } else if (roundType != null) {
            questions = questionRepository.findByRoundType(roundType);
        } else {
            questions = questionRepository.findAll();
        }

        return ResponseEntity.ok(questions);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @GetMapping("/questions/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InterviewQuestion> getAllQuestions() {
        return questionRepository.findAll();
    }

    @PostMapping("/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public InterviewQuestion createQuestion(@RequestBody InterviewQuestion question) {
        return questionRepository.save(question);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InterviewQuestion> updateQuestion(
            @PathVariable Long id, @RequestBody InterviewQuestion details) {
        return questionRepository.findById(id).map(q -> {
            q.setJobRole(details.getJobRole());
            q.setRoundType(details.getRoundType());
            q.setQuestion(details.getQuestion());
            q.setAnswer(details.getAnswer());
            q.setDifficulty(details.getDifficulty());
            return ResponseEntity.ok(questionRepository.save(q));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        return questionRepository.findById(id).map(q -> {
            questionRepository.delete(q);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
