package com.placify.controller;

import com.placify.model.Resume;
import com.placify.model.User;
import com.placify.repository.ResumeRepository;
import com.placify.repository.UserRepository;
import com.placify.security.UserPrincipal;
import com.placify.service.PdfService;
import com.placify.service.AtsService;
import com.placify.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeController {
    
    @Autowired
    private ResumeRepository resumeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private AtsService atsService;
    
    @Autowired(required = false)
    private FileStorageService fileStorageService;
    
    @GetMapping("/job-roles")
    public List<Map<String, String>> getJobRoles() {
        return atsService.getAvailableJobRoles();
    }
    
    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public Map<String, Object> analyzeResume(@RequestBody Resume resume, 
                                @AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get detailed ATS analysis
        Map<String, Object> atsAnalysis = atsService.calculateDetailedAtsScore(
            resume.getResumeText(), 
            resume.getTargetRole()
        );
        
        // Save resume with analysis info
        double atsScore = (Double) atsAnalysis.get("totalScore");
        @SuppressWarnings("unchecked")
        List<String> matchedKeywords = (List<String>) atsAnalysis.get("matchedKeywords");
        
        resume.setAtsScore(atsScore);
        resume.setKeywords(String.join(", ", matchedKeywords));
        resume.setUser(user);
        resume.setIsAnalyzed(true);
        resume.setAnalysisDate(LocalDateTime.now());
        
        Resume savedResume = resumeRepository.save(resume);
        
        // Return detailed analysis
        Map<String, Object> response = new HashMap<>();
        response.put("resume", savedResume);
        response.put("analysis", atsAnalysis);
        response.put("message", "Resume analyzed successfully!");
        
        return response;
    }
    
    @PostMapping("/upload-pdf")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public Map<String, Object> uploadPdfResume(@RequestParam("file") MultipartFile file,
                                  @RequestParam("targetRole") String targetRole,
                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Extract text from PDF
            String resumeText = pdfService.extractTextFromPdf(file);
            
            // Get detailed ATS analysis
            Map<String, Object> atsAnalysis = atsService.calculateDetailedAtsScore(resumeText, targetRole);
            
            // Create resume object
            Resume resume = new Resume();
            resume.setTargetRole(targetRole);
            resume.setResumeText(resumeText);
            resume.setUser(user);
            
            // Set file information if FileStorageService is available
            if (fileStorageService != null) {
                try {
                    FileStorageService.FileStorageResult fileResult = fileStorageService.storeResumeFile(file, user.getId());
                    resume.setFileName(fileResult.getFileName());
                    resume.setFilePath(fileResult.getFilePath());
                    resume.setFileSize(fileResult.getFileSize());
                    resume.setFileType(fileResult.getFileType());
                } catch (Exception e) {
                    // Log error but continue without file storage
                    System.err.println("Failed to store file: " + e.getMessage());
                }
            }
            
            resume.setIsAnalyzed(true);
            resume.setAnalysisDate(LocalDateTime.now());
            
            // Save resume with ATS analysis
            double atsScore = (Double) atsAnalysis.get("totalScore");
            @SuppressWarnings("unchecked")
            List<String> matchedKeywords = (List<String>) atsAnalysis.get("matchedKeywords");
            
            resume.setAtsScore(atsScore);
            resume.setKeywords(String.join(", ", matchedKeywords));
            
            Resume savedResume = resumeRepository.save(resume);
            
            // Return detailed analysis
            Map<String, Object> response = new HashMap<>();
            response.put("resume", savedResume);
            response.put("analysis", atsAnalysis);
            response.put("message", "Resume uploaded and analyzed successfully!");
            
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process PDF: " + e.getMessage());
        }
    }
    
    @GetMapping("/my-resumes")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public List<Resume> getMyResumes(@AuthenticationPrincipal UserPrincipal currentUser) {
        return resumeRepository.findByUserId(currentUser.getId());
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllResumes() {
        try {
            List<Resume> resumes = resumeRepository.findAll();
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch resumes: " + e.getMessage());
        }
    }
    
    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> downloadResume(@PathVariable Long id, 
                                           @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Resume not found"));
            
            // Check if user owns the resume or is admin
            boolean isOwner = resume.getUser().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            if (resume.getFilePath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Use FileStorageService to get the file
            if (fileStorageService != null) {
                try {
                    org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(resume.getFilePath());
                    
                    return ResponseEntity.ok()
                            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                                    "inline; filename=\"" + resume.getFileName() + "\"")
                            .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, 
                                    resume.getFileType() != null ? resume.getFileType() : "application/pdf")
                            .body(resource);
                } catch (Exception e) {
                    return ResponseEntity.status(500).body("Failed to load file: " + e.getMessage());
                }
            }
            
            return ResponseEntity.status(500).body("File storage service not available");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to prepare download: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteResume(@PathVariable Long id) {
        try {
            // Get resume to delete associated file
            Optional<Resume> resumeOpt = resumeRepository.findById(id);
            if (resumeOpt.isPresent()) {
                Resume resume = resumeOpt.get();
                // Delete file from filesystem if exists and service is available
                if (resume.getFilePath() != null && fileStorageService != null) {
                    fileStorageService.deleteFile(resume.getFilePath());
                }
            }
            
            resumeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete resume: " + e.getMessage());
        }
    }
}
