package com.placify.controller;

import com.placify.model.Company;
import com.placify.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/company")
@CrossOrigin(origins = "http://localhost:5173")
public class CompanyController {

    @Autowired
    private CompanyRepository companyRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Company> getCompanyById(@PathVariable Long id) {
        return companyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public List<Company> searchCompanies(@RequestParam String q) {
        String lower = q.toLowerCase();
        return companyRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(lower)
                        || (c.getIndustry() != null && c.getIndustry().toLowerCase().contains(lower))
                        || (c.getTechStack() != null && c.getTechStack().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    @GetMapping("/industry/{industry}")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public List<Company> getByIndustry(@PathVariable String industry) {
        return companyRepository.findAll().stream()
                .filter(c -> industry.equalsIgnoreCase(c.getIndustry()))
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Company createCompany(@RequestBody Company company) {
        return companyRepository.save(company);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Company> updateCompany(@PathVariable Long id, @RequestBody Company d) {
        return companyRepository.findById(id).map(c -> {
            c.setName(d.getName()); c.setIndustry(d.getIndustry()); c.setLocation(d.getLocation());
            c.setWebsite(d.getWebsite()); c.setLogo(d.getLogo()); c.setColor(d.getColor());
            c.setSize(d.getSize()); c.setFounded(d.getFounded()); c.setRating(d.getRating());
            c.setDescription(d.getDescription()); c.setHiringStrategy(d.getHiringStrategy());
            c.setInterviewProcess(d.getInterviewProcess()); c.setRequiredSkills(d.getRequiredSkills());
            c.setTechStack(d.getTechStack()); c.setInterviewTips(d.getInterviewTips());
            c.setSalaryRange(d.getSalaryRange()); c.setBenefits(d.getBenefits());
            c.setDifficulty(d.getDifficulty()); c.setWorkCulture(d.getWorkCulture());
            return ResponseEntity.ok(companyRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        return companyRepository.findById(id).map(c -> {
            companyRepository.delete(c);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
