package com.placify.controller;

import com.placify.dto.MessageResponse;
import com.placify.dto.ParsedJobDTO;
import com.placify.model.EmailAccount;
import com.placify.model.EmailMessage;
import com.placify.model.ParsedJob;
import com.placify.model.User;
import com.placify.repository.EmailAccountRepository;
import com.placify.repository.EmailMessageRepository;
import com.placify.repository.ParsedJobRepository;
import com.placify.repository.UserRepository;
import com.placify.service.JobParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parsed-jobs")
@CrossOrigin(origins = "http://localhost:5173")
public class ParsedJobController {

    private static final Logger logger = LoggerFactory.getLogger(ParsedJobController.class);

    @Autowired private ParsedJobRepository parsedJobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailAccountRepository emailAccountRepository;
    @Autowired private EmailMessageRepository emailMessageRepository;
    @Autowired private JobParserService jobParserService;

    @GetMapping
    public ResponseEntity<?> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String source,
            Authentication authentication
    ) {
        try {
            User user = getUser(authentication);
            Pageable pageable = PageRequest.of(page, size);
            Page<ParsedJob> jobPage = (source != null && !source.isBlank())
                ? parsedJobRepository.findByUserAndSourceOrderByReceivedAtDesc(user, source, pageable)
                : parsedJobRepository.findByUserOrderByReceivedAtDesc(user, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("jobs", jobPage.getContent().stream().map(this::toDTO).collect(Collectors.toList()));
            response.put("totalPages", jobPage.getTotalPages());
            response.put("totalItems", jobPage.getTotalElements());
            response.put("currentPage", jobPage.getNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentJobs(Authentication authentication) {
        try {
            User user = getUser(authentication);
            List<ParsedJobDTO> jobs = parsedJobRepository
                .findTop5ByUserOrderByReceivedAtDesc(user)
                .stream().map(this::toDTO).collect(Collectors.toList());
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseEmails(Authentication authentication) {
        try {
            User user = getUser(authentication);
            List<EmailAccount> accounts = emailAccountRepository.findByUser(user);
            int parsed = 0;
            for (EmailAccount account : accounts) {
                List<EmailMessage> messages = emailMessageRepository
                    .findByAccount(account, PageRequest.of(0, 200)).getContent();
                for (EmailMessage msg : messages) {
                    ParsedJob job = jobParserService.parseAndSave(msg, user);
                    if (job != null) parsed++;
                }
            }
            return ResponseEntity.ok(new MessageResponse("Parsed " + parsed + " new job(s) from your emails"));
        } catch (Exception e) {
            logger.error("Error parsing emails", e);
            return ResponseEntity.internalServerError().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reparse")
    public ResponseEntity<?> reparseEmails(Authentication authentication) {
        try {
            User user = getUser(authentication);
            List<ParsedJob> existing = parsedJobRepository
                .findByUserOrderByReceivedAtDesc(user, PageRequest.of(0, 10000)).getContent();
            parsedJobRepository.deleteAll(existing);

            List<EmailAccount> accounts = emailAccountRepository.findByUser(user);
            int parsed = 0;
            for (EmailAccount account : accounts) {
                List<EmailMessage> messages = emailMessageRepository
                    .findByAccount(account, PageRequest.of(0, 200)).getContent();
                for (EmailMessage msg : messages) {
                    ParsedJob job = jobParserService.parseAndSave(msg, user);
                    if (job != null) parsed++;
                }
            }
            return ResponseEntity.ok(new MessageResponse("Re-parsed " + parsed + " job(s) from your emails"));
        } catch (Exception e) {
            logger.error("Error re-parsing emails", e);
            return ResponseEntity.internalServerError().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication authentication) {
        try {
            User user = getUser(authentication);
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", parsedJobRepository.countByUser(user));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Authentication authentication) {
        try {
            User user = getUser(authentication);
            ParsedJob job = parsedJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
            if (!job.getUser().getId().equals(user.getId()))
                return ResponseEntity.status(403).body(new MessageResponse("Access denied"));
            parsedJobRepository.delete(job);
            return ResponseEntity.ok(new MessageResponse("Job deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse(e.getMessage()));
        }
    }

    private User getUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ParsedJobDTO toDTO(ParsedJob j) {
        ParsedJobDTO dto = new ParsedJobDTO();
        dto.setId(j.getId());
        dto.setSource(j.getSource());
        dto.setSourceColor(j.getSourceColor());
        dto.setSourceIcon(j.getSourceIcon());
        dto.setJobTitle(j.getJobTitle());
        dto.setCompanyName(j.getCompanyName());
        dto.setCompanyRating(j.getCompanyRating());
        dto.setLocation(j.getLocation());
        dto.setExperienceRequired(j.getExperienceRequired());
        dto.setSalaryRange(j.getSalaryRange());
        dto.setWorkMode(j.getWorkMode());
        dto.setSkills(j.getSkills());
        dto.setApplyLink(j.getApplyLink());
        dto.setJobDescription(j.getJobDescription());
        dto.setSenderEmail(j.getSenderEmail());
        dto.setReceivedAt(j.getReceivedAt());
        dto.setCreatedAt(j.getCreatedAt());
        return dto;
    }
}
