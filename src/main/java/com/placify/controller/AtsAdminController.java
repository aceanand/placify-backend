package com.placify.controller;

import com.placify.model.*;
import com.placify.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.*;

@RestController
@RequestMapping("/api/admin/ats")
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class AtsAdminController {
    
    @Autowired
    private JobRoleRepository jobRoleRepository;
    
    @Autowired
    private SkillKeywordRepository skillKeywordRepository;
    
    @Autowired
    private ActionVerbRepository actionVerbRepository;
    
    @Autowired
    private AtsConfigurationRepository atsConfigurationRepository;
    
    // ==================== Job Roles ====================
    
    @GetMapping("/job-roles")
    public List<JobRole> getAllJobRoles() {
        return jobRoleRepository.findAll();
    }
    
    @GetMapping("/job-roles/active")
    public List<JobRole> getActiveJobRoles() {
        return jobRoleRepository.findByIsActiveTrue();
    }
    
    @GetMapping("/job-roles/{id}")
    public ResponseEntity<JobRole> getJobRole(@PathVariable Long id) {
        return jobRoleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/job-roles")
    public ResponseEntity<?> createJobRole(@RequestBody JobRole jobRole) {
        if (jobRoleRepository.existsByTitle(jobRole.getTitle())) {
            return ResponseEntity.badRequest().body("Job role with this title already exists");
        }
        return ResponseEntity.ok(jobRoleRepository.save(jobRole));
    }
    
    @PutMapping("/job-roles/{id}")
    public ResponseEntity<?> updateJobRole(@PathVariable Long id, @RequestBody JobRole jobRoleDetails) {
        return jobRoleRepository.findById(id)
                .map(jobRole -> {
                    jobRole.setTitle(jobRoleDetails.getTitle());
                    jobRole.setDescription(jobRoleDetails.getDescription());
                    jobRole.setIsActive(jobRoleDetails.getIsActive());
                    return ResponseEntity.ok(jobRoleRepository.save(jobRole));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/job-roles/{id}")
    @Transactional
    public ResponseEntity<?> deleteJobRole(@PathVariable Long id) {
        return jobRoleRepository.findById(id)
                .map(jobRole -> {
                    // Delete associated keywords first
                    skillKeywordRepository.deleteByJobRoleId(id);
                    jobRoleRepository.delete(jobRole);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ==================== Skill Keywords ====================
    
    @GetMapping("/job-roles/{roleId}/keywords")
    public List<SkillKeyword> getKeywordsByRole(@PathVariable Long roleId) {
        return skillKeywordRepository.findByJobRoleIdAndIsActiveTrue(roleId);
    }
    
    @GetMapping("/keywords")
    public List<SkillKeyword> getAllKeywords() {
        return skillKeywordRepository.findAll();
    }
    
    @PostMapping("/job-roles/{roleId}/keywords")
    public ResponseEntity<?> addKeyword(@PathVariable Long roleId, @RequestBody SkillKeyword keyword) {
        return jobRoleRepository.findById(roleId)
                .map(jobRole -> {
                    keyword.setJobRole(jobRole);
                    return ResponseEntity.ok(skillKeywordRepository.save(keyword));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/job-roles/{roleId}/keywords/bulk")
    public ResponseEntity<?> addKeywordsBulk(@PathVariable Long roleId, 
                                             @RequestBody List<SkillKeyword> keywords) {
        return jobRoleRepository.findById(roleId)
                .map(jobRole -> {
                    keywords.forEach(keyword -> keyword.setJobRole(jobRole));
                    List<SkillKeyword> saved = skillKeywordRepository.saveAll(keywords);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/keywords/{id}")
    public ResponseEntity<?> updateKeyword(@PathVariable Long id, @RequestBody SkillKeyword keywordDetails) {
        return skillKeywordRepository.findById(id)
                .map(keyword -> {
                    keyword.setKeyword(keywordDetails.getKeyword());
                    keyword.setCategory(keywordDetails.getCategory());
                    keyword.setPriorityLevel(keywordDetails.getPriorityLevel());
                    keyword.setIsActive(keywordDetails.getIsActive());
                    return ResponseEntity.ok(skillKeywordRepository.save(keyword));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/keywords/{id}")
    public ResponseEntity<?> deleteKeyword(@PathVariable Long id) {
        return skillKeywordRepository.findById(id)
                .map(keyword -> {
                    skillKeywordRepository.delete(keyword);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ==================== Action Verbs ====================
    
    @GetMapping("/action-verbs")
    public List<ActionVerb> getAllActionVerbs() {
        return actionVerbRepository.findAll();
    }
    
    @GetMapping("/action-verbs/active")
    public List<ActionVerb> getActiveActionVerbs() {
        return actionVerbRepository.findByIsActiveTrue();
    }
    
    @PostMapping("/action-verbs")
    public ResponseEntity<?> createActionVerb(@RequestBody ActionVerb actionVerb) {
        return ResponseEntity.ok(actionVerbRepository.save(actionVerb));
    }
    
    @PostMapping("/action-verbs/bulk")
    public ResponseEntity<?> createActionVerbsBulk(@RequestBody List<ActionVerb> actionVerbs) {
        List<ActionVerb> saved = actionVerbRepository.saveAll(actionVerbs);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/action-verbs/{id}")
    public ResponseEntity<?> updateActionVerb(@PathVariable Long id, @RequestBody ActionVerb verbDetails) {
        return actionVerbRepository.findById(id)
                .map(verb -> {
                    verb.setVerb(verbDetails.getVerb());
                    verb.setImpactLevel(verbDetails.getImpactLevel());
                    verb.setImpactScore(verbDetails.getImpactScore());
                    verb.setIsActive(verbDetails.getIsActive());
                    return ResponseEntity.ok(actionVerbRepository.save(verb));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/action-verbs/{id}")
    public ResponseEntity<?> deleteActionVerb(@PathVariable Long id) {
        return actionVerbRepository.findById(id)
                .map(verb -> {
                    actionVerbRepository.delete(verb);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ==================== ATS Configuration ====================
    
    @GetMapping("/config")
    public List<AtsConfiguration> getAllConfigurations() {
        return atsConfigurationRepository.findAll();
    }
    
    @GetMapping("/config/{key}")
    public ResponseEntity<AtsConfiguration> getConfiguration(@PathVariable String key) {
        return atsConfigurationRepository.findByConfigKey(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/config")
    public ResponseEntity<?> createConfiguration(@RequestBody AtsConfiguration config) {
        return ResponseEntity.ok(atsConfigurationRepository.save(config));
    }
    
    @PutMapping("/config/{id}")
    public ResponseEntity<?> updateConfiguration(@PathVariable Long id, 
                                                 @RequestBody AtsConfiguration configDetails) {
        return atsConfigurationRepository.findById(id)
                .map(config -> {
                    config.setConfigValue(configDetails.getConfigValue());
                    config.setDescription(configDetails.getDescription());
                    config.setIsActive(configDetails.getIsActive());
                    return ResponseEntity.ok(atsConfigurationRepository.save(config));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/config/{id}")
    public ResponseEntity<?> deleteConfiguration(@PathVariable Long id) {
        return atsConfigurationRepository.findById(id)
                .map(config -> {
                    atsConfigurationRepository.delete(config);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ==================== Utility Endpoints ====================
    
    @GetMapping("/stats")
    public Map<String, Object> getAtsStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobRoles", jobRoleRepository.count());
        stats.put("activeJobRoles", jobRoleRepository.findByIsActiveTrue().size());
        stats.put("totalKeywords", skillKeywordRepository.count());
        stats.put("totalActionVerbs", actionVerbRepository.count());
        stats.put("totalConfigurations", atsConfigurationRepository.count());
        return stats;
    }
}
