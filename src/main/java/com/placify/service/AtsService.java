package com.placify.service;

import com.placify.model.*;
import com.placify.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AtsService {
    
    @Autowired
    private JobRoleRepository jobRoleRepository;
    
    @Autowired
    private SkillKeywordRepository skillKeywordRepository;
    
    @Autowired
    private ActionVerbRepository actionVerbRepository;
    
    @Autowired
    private AtsConfigurationRepository atsConfigurationRepository;
    
    // Detailed ATS Score Breakdown
    public Map<String, Object> calculateDetailedAtsScore(String resumeText, String targetRole) {
        Map<String, Object> result = new HashMap<>();
        
        if (resumeText == null || resumeText.isEmpty()) {
            result.put("totalScore", 0.0);
            result.put("breakdown", new HashMap<>());
            result.put("suggestions", Arrays.asList("Please provide resume text"));
            return result;
        }
        
        String lowerResume = resumeText.toLowerCase();
        String originalResume = resumeText; // Keep original for case-sensitive checks
        String lowerRole = targetRole.toLowerCase();
        
        // Score breakdown
        Map<String, Double> breakdown = new HashMap<>();
        List<String> suggestions = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();
        Map<String, Object> detailedAnalysis = new HashMap<>();
        
        // 1. Technical Skills Score (30 points)
        double technicalScore = calculateTechnicalSkillsScore(lowerResume, lowerRole, matchedKeywords, missingKeywords, detailedAnalysis);
        breakdown.put("technicalSkills", technicalScore);
        
        // 2. Experience Score (25 points)
        double experienceScore = calculateExperienceScore(lowerResume, originalResume, detailedAnalysis);
        breakdown.put("experience", experienceScore);
        
        // 3. Education Score (15 points)
        double educationScore = calculateEducationScore(lowerResume, detailedAnalysis);
        breakdown.put("education", educationScore);
        
        // 4. Keywords Density Score (15 points)
        double keywordDensity = calculateKeywordDensity(lowerResume, lowerRole, detailedAnalysis);
        breakdown.put("keywordDensity", keywordDensity);
        
        // 5. Formatting & Structure Score (10 points)
        double formattingScore = calculateFormattingScore(originalResume, detailedAnalysis);
        breakdown.put("formatting", formattingScore);
        
        // 6. Action Verbs Score (5 points)
        double actionVerbsScore = calculateActionVerbsScore(lowerResume, detailedAnalysis);
        breakdown.put("actionVerbs", actionVerbsScore);
        
        // Calculate total score
        double totalScore = technicalScore + experienceScore + educationScore + 
                           keywordDensity + formattingScore + actionVerbsScore;
        
        // Generate suggestions
        suggestions.addAll(generateSuggestions(breakdown, matchedKeywords, missingKeywords, detailedAnalysis));
        
        // Build result
        result.put("totalScore", Math.round(totalScore * 100.0) / 100.0);
        result.put("breakdown", breakdown);
        result.put("matchedKeywords", matchedKeywords);
        result.put("missingKeywords", missingKeywords);
        result.put("suggestions", suggestions);
        result.put("grade", getGrade(totalScore));
        result.put("detailedAnalysis", detailedAnalysis);
        
        return result;
    }
    
    // 1. Technical Skills Score (30 points) - Enhanced
    private double calculateTechnicalSkillsScore(String resumeText, String targetRole, 
                                                  List<String> matched, List<String> missing,
                                                  Map<String, Object> detailedAnalysis) {
        Map<String, List<String>> skillsByCategory = getRequiredSkillsByCategory(targetRole);
        
        if (skillsByCategory.isEmpty()) {
            detailedAnalysis.put("skillsAnalysis", "No specific role requirements");
            return 15.0; // Default score if no specific role
        }
        
        int totalSkills = 0;
        int matchedCount = 0;
        int criticalMatched = 0;
        int criticalTotal = 0;
        
        Map<String, Object> categoryScores = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : skillsByCategory.entrySet()) {
            String category = entry.getKey();
            List<String> skills = entry.getValue();
            int categoryMatched = 0;
            
            for (String skill : skills) {
                totalSkills++;
                boolean found = isSkillPresent(resumeText, skill);
                
                if (found) {
                    matchedCount++;
                    categoryMatched++;
                    matched.add(skill);
                    
                    // Track critical skills (first 5 in each category)
                    if (skills.indexOf(skill) < 5) {
                        criticalMatched++;
                    }
                } else {
                    missing.add(skill);
                }
                
                // Count critical skills
                if (skills.indexOf(skill) < 5) {
                    criticalTotal++;
                }
            }
            
            double categoryScore = skills.isEmpty() ? 0 : (categoryMatched * 100.0) / skills.size();
            categoryScores.put(category, Math.round(categoryScore * 10.0) / 10.0);
        }
        
        // Calculate weighted score
        double baseScore = totalSkills == 0 ? 0 : (matchedCount * 20.0) / totalSkills;
        
        // Bonus for critical skills (up to 10 points)
        double criticalBonus = criticalTotal == 0 ? 0 : (criticalMatched * 10.0) / criticalTotal;
        
        double finalScore = baseScore + criticalBonus;
        
        // Store detailed analysis
        detailedAnalysis.put("totalSkillsRequired", totalSkills);
        detailedAnalysis.put("skillsMatched", matchedCount);
        detailedAnalysis.put("skillsMatchPercentage", Math.round((matchedCount * 100.0) / totalSkills));
        detailedAnalysis.put("criticalSkillsMatched", criticalMatched + "/" + criticalTotal);
        detailedAnalysis.put("categoryScores", categoryScores);
        
        return Math.min(Math.round(finalScore * 100.0) / 100.0, 30.0);
    }
    
    // Check if skill is present with word boundary matching
    private boolean isSkillPresent(String resumeText, String skill) {
        String lowerSkill = skill.toLowerCase();
        
        // For multi-word skills (e.g., "Spring Boot", "Machine Learning")
        if (lowerSkill.contains(" ")) {
            return resumeText.contains(lowerSkill);
        }
        
        // For single-word skills, check with word boundaries
        String pattern = "(?i)(?<![a-z])" + Pattern.quote(skill) + "(?![a-z])";
        return Pattern.compile(pattern).matcher(resumeText).find();
    }
    
    // 2. Experience Score (25 points) - Enhanced
    private double calculateExperienceScore(String resumeText, String originalResume,
                                           Map<String, Object> detailedAnalysis) {
        double score = 0.0;
        Map<String, Object> expAnalysis = new HashMap<>();
        
        // 1. Years of experience (max 8 points)
        Pattern yearsPattern = Pattern.compile("(\\d+)\\+?\\s*(years?|yrs?)");
        Matcher yearsMatcher = yearsPattern.matcher(resumeText);
        int maxYears = 0;
        while (yearsMatcher.find()) {
            int years = Integer.parseInt(yearsMatcher.group(1));
            maxYears = Math.max(maxYears, years);
        }
        double yearsScore = Math.min(maxYears * 1.5, 8);
        score += yearsScore;
        expAnalysis.put("yearsOfExperience", maxYears);
        expAnalysis.put("yearsScore", yearsScore);
        
        // 2. Quantifiable achievements (max 8 points)
        int achievementCount = countQuantifiableAchievements(originalResume);
        double achievementScore = Math.min(achievementCount * 1.0, 8);
        score += achievementScore;
        expAnalysis.put("quantifiableAchievements", achievementCount);
        expAnalysis.put("achievementScore", achievementScore);
        
        // 3. Experience keywords (max 6 points)
        String[] experienceKeywords = {"experience", "worked", "developed", "built", "created", 
                                       "implemented", "designed", "managed", "led", "delivered",
                                       "architected", "optimized", "improved", "increased", "reduced"};
        int expKeywordCount = 0;
        for (String keyword : experienceKeywords) {
            if (resumeText.contains(keyword)) {
                expKeywordCount++;
            }
        }
        double keywordScore = Math.min(expKeywordCount * 0.5, 6);
        score += keywordScore;
        expAnalysis.put("experienceKeywords", expKeywordCount);
        expAnalysis.put("keywordScore", keywordScore);
        
        // 4. Job titles/positions (max 3 points)
        int positionCount = countPositions(resumeText);
        double positionScore = Math.min(positionCount * 1.0, 3);
        score += positionScore;
        expAnalysis.put("positionsListed", positionCount);
        expAnalysis.put("positionScore", positionScore);
        
        detailedAnalysis.put("experienceAnalysis", expAnalysis);
        return Math.min(Math.round(score * 100.0) / 100.0, 25.0);
    }
    
    // Count quantifiable achievements (numbers, percentages, metrics)
    private int countQuantifiableAchievements(String resumeText) {
        int count = 0;
        
        // Pattern for percentages: 50%, 25%, etc.
        Pattern percentPattern = Pattern.compile("\\d+%");
        Matcher percentMatcher = percentPattern.matcher(resumeText);
        while (percentMatcher.find()) count++;
        
        // Pattern for numbers with context: "increased by 40", "reduced 30", etc.
        Pattern numberPattern = Pattern.compile("(increased|reduced|improved|grew|saved|generated|achieved)\\s+(?:by\\s+)?\\d+");
        Matcher numberMatcher = numberPattern.matcher(resumeText.toLowerCase());
        while (numberMatcher.find()) count++;
        
        // Pattern for dollar amounts: $1M, $500K, etc.
        Pattern dollarPattern = Pattern.compile("\\$\\d+[KMB]?");
        Matcher dollarMatcher = dollarPattern.matcher(resumeText);
        while (dollarMatcher.find()) count++;
        
        // Pattern for time savings: "reduced time by 2 hours", etc.
        Pattern timePattern = Pattern.compile("\\d+\\s+(hours?|days?|weeks?|months?)");
        Matcher timeMatcher = timePattern.matcher(resumeText.toLowerCase());
        while (timeMatcher.find()) count++;
        
        return count;
    }
    
    // Count job positions/titles
    private int countPositions(String resumeText) {
        int count = 0;
        String[] positionKeywords = {"engineer", "developer", "manager", "analyst", "architect",
                                    "lead", "senior", "junior", "specialist", "consultant",
                                    "director", "coordinator", "administrator", "designer"};
        
        for (String keyword : positionKeywords) {
            if (resumeText.contains(keyword)) {
                count++;
            }
        }
        
        return Math.min(count, 5); // Cap at 5 positions
    }
    
    // 3. Education Score (15 points) - Enhanced
    private double calculateEducationScore(String resumeText, Map<String, Object> detailedAnalysis) {
        double score = 0.0;
        Map<String, Object> eduAnalysis = new HashMap<>();
        
        // 1. Degrees (max 8 points)
        Map<String, Integer> degreePoints = new HashMap<>();
        degreePoints.put("phd", 8);
        degreePoints.put("ph.d", 8);
        degreePoints.put("doctorate", 8);
        degreePoints.put("master", 6);
        degreePoints.put("mba", 6);
        degreePoints.put("m.tech", 6);
        degreePoints.put("m.e", 6);
        degreePoints.put("m.sc", 6);
        degreePoints.put("bachelor", 5);
        degreePoints.put("b.tech", 5);
        degreePoints.put("b.e", 5);
        degreePoints.put("b.sc", 5);
        degreePoints.put("associate", 3);
        
        int highestDegreePoints = 0;
        String highestDegree = "None";
        for (Map.Entry<String, Integer> entry : degreePoints.entrySet()) {
            if (resumeText.contains(entry.getKey())) {
                if (entry.getValue() > highestDegreePoints) {
                    highestDegreePoints = entry.getValue();
                    highestDegree = entry.getKey();
                }
            }
        }
        score += highestDegreePoints;
        eduAnalysis.put("highestDegree", highestDegree);
        eduAnalysis.put("degreeScore", highestDegreePoints);
        
        // 2. GPA bonus (max 2 points)
        Pattern gpaPattern = Pattern.compile("gpa[:\\s]*(\\d\\.\\d+)");
        Matcher gpaMatcher = gpaPattern.matcher(resumeText);
        if (gpaMatcher.find()) {
            double gpa = Double.parseDouble(gpaMatcher.group(1));
            if (gpa >= 3.5) {
                score += 2;
                eduAnalysis.put("gpa", gpa);
                eduAnalysis.put("gpaBonus", 2);
            } else if (gpa >= 3.0) {
                score += 1;
                eduAnalysis.put("gpa", gpa);
                eduAnalysis.put("gpaBonus", 1);
            }
        }
        
        // 3. Certifications (max 5 points)
        String[] certifications = {"certified", "certification", "certificate", 
                                  "aws certified", "azure certified", "google cloud certified",
                                  "oracle certified", "microsoft certified",
                                  "pmp", "scrum master", "csm", "cissp", "ceh",
                                  "comptia", "ccna", "ccnp", "cka", "ckad"};
        int certCount = 0;
        List<String> foundCerts = new ArrayList<>();
        for (String cert : certifications) {
            if (resumeText.contains(cert)) {
                certCount++;
                foundCerts.add(cert);
            }
        }
        double certScore = Math.min(certCount * 1.0, 5);
        score += certScore;
        eduAnalysis.put("certificationsFound", certCount);
        eduAnalysis.put("certificationScore", certScore);
        
        detailedAnalysis.put("educationAnalysis", eduAnalysis);
        return Math.min(Math.round(score * 100.0) / 100.0, 15.0);
    }
    
    // 4. Keyword Density Score (15 points) - Enhanced with TF-IDF-like scoring
    private double calculateKeywordDensity(String resumeText, String targetRole,
                                          Map<String, Object> detailedAnalysis) {
        List<String> roleKeywords = getRoleSpecificKeywords(targetRole);
        
        if (roleKeywords.isEmpty()) {
            return 7.5; // Default score
        }
        
        Map<String, Integer> keywordFrequency = new HashMap<>();
        int totalOccurrences = 0;
        int keywordsWithOptimalDensity = 0;
        
        for (String keyword : roleKeywords) {
            int count = countOccurrences(resumeText, keyword.toLowerCase());
            keywordFrequency.put(keyword, count);
            totalOccurrences += count;
            
            // Optimal density: 2-4 occurrences per keyword
            if (count >= 2 && count <= 4) {
                keywordsWithOptimalDensity++;
            }
        }
        
        // Calculate score based on optimal density
        double optimalDensityScore = (keywordsWithOptimalDensity * 10.0) / roleKeywords.size();
        
        // Bonus for overall keyword presence (max 5 points)
        double presenceBonus = Math.min((totalOccurrences * 5.0) / (roleKeywords.size() * 3), 5);
        
        double finalScore = optimalDensityScore + presenceBonus;
        
        // Penalty for keyword stuffing (more than 6 occurrences)
        int stuffedKeywords = 0;
        for (int count : keywordFrequency.values()) {
            if (count > 6) stuffedKeywords++;
        }
        if (stuffedKeywords > 0) {
            finalScore -= (stuffedKeywords * 0.5);
        }
        
        Map<String, Object> densityAnalysis = new HashMap<>();
        densityAnalysis.put("totalKeywordOccurrences", totalOccurrences);
        densityAnalysis.put("keywordsWithOptimalDensity", keywordsWithOptimalDensity);
        densityAnalysis.put("averageOccurrencesPerKeyword", 
                           roleKeywords.isEmpty() ? 0 : Math.round((totalOccurrences * 10.0) / roleKeywords.size()) / 10.0);
        densityAnalysis.put("keywordStuffingDetected", stuffedKeywords > 0);
        
        detailedAnalysis.put("keywordDensityAnalysis", densityAnalysis);
        
        return Math.max(0, Math.min(Math.round(finalScore * 100.0) / 100.0, 15.0));
    }
    
    // 5. Formatting & Structure Score (10 points) - Enhanced
    private double calculateFormattingScore(String resumeText, Map<String, Object> detailedAnalysis) {
        double score = 0.0;
        Map<String, Object> formatAnalysis = new HashMap<>();
        
        // 1. Standard sections (max 4 points)
        String[] sections = {"experience", "education", "skills", "projects", "summary", "objective", "certifications"};
        int sectionCount = 0;
        List<String> foundSections = new ArrayList<>();
        for (String section : sections) {
            if (resumeText.toLowerCase().contains(section)) {
                sectionCount++;
                foundSections.add(section);
            }
        }
        double sectionScore = Math.min(sectionCount * 0.8, 4);
        score += sectionScore;
        formatAnalysis.put("sectionsFound", foundSections);
        formatAnalysis.put("sectionScore", sectionScore);
        
        // 2. Bullet points or structured content (2 points)
        boolean hasBullets = resumeText.contains("•") || resumeText.contains("-") || 
                            resumeText.contains("*") || resumeText.contains("►");
        if (hasBullets) {
            score += 2;
            formatAnalysis.put("hasBulletPoints", true);
        }
        
        // 3. Contact information (2 points)
        boolean hasEmail = resumeText.contains("@");
        boolean hasPhone = resumeText.matches(".*\\d{10}.*") || resumeText.matches(".*\\(\\d{3}\\).*");
        if (hasEmail && hasPhone) {
            score += 2;
            formatAnalysis.put("hasCompleteContact", true);
        } else if (hasEmail || hasPhone) {
            score += 1;
            formatAnalysis.put("hasPartialContact", true);
        }
        
        // 4. Resume length check (1 point)
        int wordCount = resumeText.split("\\s+").length;
        formatAnalysis.put("wordCount", wordCount);
        if (wordCount >= 300 && wordCount <= 1000) {
            score += 1;
            formatAnalysis.put("lengthOptimal", true);
        } else if (wordCount < 300) {
            formatAnalysis.put("lengthTooShort", true);
        } else {
            formatAnalysis.put("lengthTooLong", true);
        }
        
        // 5. Professional formatting (1 point)
        boolean hasCapitalization = !resumeText.equals(resumeText.toLowerCase());
        boolean hasProperSpacing = resumeText.contains("\n") || resumeText.contains("  ");
        if (hasCapitalization && hasProperSpacing) {
            score += 1;
            formatAnalysis.put("hasProfessionalFormatting", true);
        }
        
        detailedAnalysis.put("formattingAnalysis", formatAnalysis);
        return Math.min(Math.round(score * 100.0) / 100.0, 10.0);
    }
    
    // 6. Action Verbs Score (5 points) - Enhanced with impact levels (from database)
    private double calculateActionVerbsScore(String resumeText, Map<String, Object> detailedAnalysis) {
        // Get action verbs from database
        List<ActionVerb> actionVerbs = actionVerbRepository.findByIsActiveTrue();
        
        if (actionVerbs.isEmpty()) {
            // Fallback to hardcoded if database is empty
            return calculateActionVerbsScoreHardcoded(resumeText, detailedAnalysis);
        }
        
        Map<String, Integer> impactLevels = new HashMap<>();
        double score = 0.0;
        
        int highImpactCount = 0;
        int mediumImpactCount = 0;
        int lowImpactCount = 0;
        
        for (ActionVerb verb : actionVerbs) {
            if (resumeText.contains(verb.getVerb().toLowerCase())) {
                score += verb.getImpactScore();
                
                switch (verb.getImpactLevel()) {
                    case "HIGH":
                        highImpactCount++;
                        break;
                    case "MEDIUM":
                        mediumImpactCount++;
                        break;
                    case "LOW":
                        lowImpactCount++;
                        break;
                }
            }
        }
        
        impactLevels.put("highImpactVerbs", highImpactCount);
        impactLevels.put("mediumImpactVerbs", mediumImpactCount);
        impactLevels.put("lowImpactVerbs", lowImpactCount);
        impactLevels.put("totalActionVerbs", highImpactCount + mediumImpactCount + lowImpactCount);
        
        detailedAnalysis.put("actionVerbsAnalysis", impactLevels);
        
        // Get max score from configuration or use default
        double maxScore = getConfigValue("action_verbs_max_score", 5.0);
        return Math.min(Math.round(score * 100.0) / 100.0, maxScore);
    }
    
    // Fallback hardcoded action verbs scoring
    private double calculateActionVerbsScoreHardcoded(String resumeText, Map<String, Object> detailedAnalysis) {
        Map<String, Integer> impactLevels = new HashMap<>();
        
        // High-impact verbs (1.0 point each)
        String[] highImpactVerbs = {"achieved", "improved", "increased", "reduced", "optimized", 
                                   "transformed", "revolutionized", "pioneered", "spearheaded"};
        int highImpactCount = 0;
        for (String verb : highImpactVerbs) {
            if (resumeText.contains(verb)) {
                highImpactCount++;
            }
        }
        
        // Medium-impact verbs (0.5 points each)
        String[] mediumImpactVerbs = {"developed", "created", "designed", "implemented", "managed", 
                                     "led", "coordinated", "executed", "delivered", "built"};
        int mediumImpactCount = 0;
        for (String verb : mediumImpactVerbs) {
            if (resumeText.contains(verb)) {
                mediumImpactCount++;
            }
        }
        
        // Low-impact verbs (0.2 points each)
        String[] lowImpactVerbs = {"worked", "helped", "assisted", "participated", "contributed"};
        int lowImpactCount = 0;
        for (String verb : lowImpactVerbs) {
            if (resumeText.contains(verb)) {
                lowImpactCount++;
            }
        }
        
        double score = Math.min(highImpactCount * 1.0, 3.0) + 
                      Math.min(mediumImpactCount * 0.5, 1.5) + 
                      Math.min(lowImpactCount * 0.2, 0.5);
        
        impactLevels.put("highImpactVerbs", highImpactCount);
        impactLevels.put("mediumImpactVerbs", mediumImpactCount);
        impactLevels.put("lowImpactVerbs", lowImpactCount);
        impactLevels.put("totalActionVerbs", highImpactCount + mediumImpactCount + lowImpactCount);
        
        detailedAnalysis.put("actionVerbsAnalysis", impactLevels);
        
        return Math.min(Math.round(score * 100.0) / 100.0, 5.0);
    }
    
    // Generate suggestions based on score breakdown and detailed analysis
    private List<String> generateSuggestions(Map<String, Double> breakdown,
                                             List<String> matched, List<String> missing,
                                             Map<String, Object> detailedAnalysis) {
        List<String> suggestions = new ArrayList<>();
        try {
            generateSuggestionsInternal(breakdown, matched, missing, detailedAnalysis, suggestions);
        } catch (Exception e) {
            // Never let suggestion generation crash the whole analysis
            suggestions.add("💡 Resume analyzed successfully. Review the score breakdown for improvement areas.");
        }
        return suggestions;
    }

    private void generateSuggestionsInternal(Map<String, Double> breakdown,
                                             List<String> matched, List<String> missing,
                                             Map<String, Object> detailedAnalysis,
                                             List<String> suggestions) {
        if (breakdown.get("technicalSkills") < 20) {
            if (!missing.isEmpty()) {
                suggestions.add("⚠️ CRITICAL: Add essential skills for this role: " +
                    String.join(", ", missing.subList(0, Math.min(5, missing.size()))));
            }
            suggestions.add("💡 Add more technical skills relevant to the role (Target: 20+ points)");
        }
        
        // Experience suggestions
        if (breakdown.get("experience") < 18) {
            @SuppressWarnings("unchecked")
            Map<String, Object> expAnalysis = (Map<String, Object>) detailedAnalysis.get("experienceAnalysis");
            if (expAnalysis != null) {
                int achievements = (Integer) expAnalysis.get("quantifiableAchievements");
                if (achievements < 3) {
                    suggestions.add("📊 Add quantifiable achievements with numbers (e.g., 'Increased performance by 40%', 'Reduced costs by $50K')");
                }
                
                int years = (Integer) expAnalysis.get("yearsOfExperience");
                if (years == 0) {
                    suggestions.add("📅 Clearly mention your years of experience (e.g., '5+ years of experience in...')");
                }
            }
        }
        
        // Education suggestions
        if (breakdown.get("education") < 10) {
            @SuppressWarnings("unchecked")
            Map<String, Object> eduAnalysis = (Map<String, Object>) detailedAnalysis.get("educationAnalysis");
            if (eduAnalysis != null) {
                String degree = (String) eduAnalysis.get("highestDegree");
                if ("None".equals(degree)) {
                    suggestions.add("🎓 Add your educational qualifications (degree, university, year)");
                }
                
                int certs = (Integer) eduAnalysis.get("certificationsFound");
                if (certs == 0) {
                    suggestions.add("📜 Include relevant certifications (AWS, Azure, PMP, Scrum Master, etc.)");
                }
            }
        }
        
        // Keyword Density suggestions
        if (breakdown.get("keywordDensity") < 10) {
            @SuppressWarnings("unchecked")
            Map<String, Object> densityAnalysis = (Map<String, Object>) detailedAnalysis.get("keywordDensityAnalysis");
            if (densityAnalysis != null) {
                boolean stuffing = (Boolean) densityAnalysis.get("keywordStuffingDetected");
                if (stuffing) {
                    suggestions.add("⚠️ Reduce keyword repetition - some keywords appear too frequently (looks like keyword stuffing)");
                } else {
                    suggestions.add("🔑 Increase keyword density by naturally incorporating role-specific terms 2-3 times throughout your resume");
                }
            }
        }
        
        // Formatting suggestions
        if (breakdown.get("formatting") < 7) {
            @SuppressWarnings("unchecked")
            Map<String, Object> formatAnalysis = (Map<String, Object>) detailedAnalysis.get("formattingAnalysis");
            if (formatAnalysis != null) {
                @SuppressWarnings("unchecked")
                List<String> sections = (List<String>) formatAnalysis.get("sectionsFound");
                if (sections.size() < 4) {
                    suggestions.add("📋 Add standard resume sections: Summary, Experience, Education, Skills, Projects");
                }
                
                Boolean hasBullets = (Boolean) formatAnalysis.get("hasBulletPoints");
                if (hasBullets == null || !hasBullets) {
                    suggestions.add("• Use bullet points to improve readability and structure");
                }
                
                Boolean hasContact = (Boolean) formatAnalysis.get("hasCompleteContact");
                if (hasContact == null || !hasContact) {
                    suggestions.add("📧 Include complete contact information (email and phone number)");
                }
                
                Boolean tooShort = (Boolean) formatAnalysis.get("lengthTooShort");
                Boolean tooLong = (Boolean) formatAnalysis.get("lengthTooLong");
                if (tooShort != null && tooShort) {
                    suggestions.add("📝 Resume is too short - add more details about your experience and projects (aim for 300-1000 words)");
                } else if (tooLong != null && tooLong) {
                    suggestions.add("✂️ Resume is too long - condense to 1-2 pages, focus on most relevant experience");
                }
            }
        }
        
        // Action Verbs suggestions
        if (breakdown.get("actionVerbs") < 3) {
            @SuppressWarnings("unchecked")
            Map<String, Object> verbAnalysis = (Map<String, Object>) detailedAnalysis.get("actionVerbsAnalysis");
            if (verbAnalysis != null) {
                int highImpact = (Integer) verbAnalysis.get("highImpactVerbs");
                if (highImpact < 3) {
                    suggestions.add("💪 Use more high-impact action verbs: achieved, improved, increased, reduced, optimized, transformed");
                }
            }
        }
        
        // Overall suggestions
        if (suggestions.isEmpty()) {
            suggestions.add("🎉 Excellent resume! Consider tailoring it further for specific job descriptions");
            suggestions.add("💡 Keep updating with new skills, projects, and achievements");
        } else if (breakdown.get("technicalSkills") + breakdown.get("experience") > 40) {
            suggestions.add("👍 Strong technical background! Focus on improving formatting and presentation");
        }
    } // end generateSuggestionsInternal
    
    // Get grade based on score
    private String getGrade(double score) {
        if (score >= 90) return "Excellent";
        if (score >= 80) return "Very Good";
        if (score >= 70) return "Good";
        if (score >= 60) return "Fair";
        if (score >= 50) return "Needs Improvement";
        return "Poor";
    }
    
    // Get required skills organized by category with priority (from database)
    private Map<String, List<String>> getRequiredSkillsByCategory(String targetRole) {
        // Try to find job role in database
        Optional<JobRole> jobRoleOpt = jobRoleRepository.findByTitle(targetRole);
        
        if (jobRoleOpt.isPresent()) {
            JobRole jobRole = jobRoleOpt.get();
            List<SkillKeyword> keywords = skillKeywordRepository.findByJobRoleAndIsActiveTrue(jobRole);
            
            // Group by category and sort by priority
            Map<String, List<String>> skillsByCategory = new LinkedHashMap<>();
            Map<String, List<SkillKeyword>> groupedKeywords = keywords.stream()
                    .collect(Collectors.groupingBy(SkillKeyword::getCategory));
            
            for (Map.Entry<String, List<SkillKeyword>> entry : groupedKeywords.entrySet()) {
                List<String> skills = entry.getValue().stream()
                        .sorted(Comparator.comparing(SkillKeyword::getPriorityLevel))
                        .map(SkillKeyword::getKeyword)
                        .collect(Collectors.toList());
                skillsByCategory.put(entry.getKey(), skills);
            }
            
            return skillsByCategory;
        }
        
        // Fallback to hardcoded values if not in database
        return getHardcodedSkillsByCategory(targetRole);
    }
    
    // Fallback hardcoded skills (for backward compatibility)
    private Map<String, List<String>> getHardcodedSkillsByCategory(String targetRole) {
        String lowerRole = targetRole.toLowerCase();
        Map<String, List<String>> skillsByCategory = new LinkedHashMap<>();
        
        if (lowerRole.contains("software") || lowerRole.contains("developer") || lowerRole.contains("engineer")) {
            skillsByCategory.put("Core Languages", Arrays.asList("Java", "Python", "JavaScript", "TypeScript", "C++"));
            skillsByCategory.put("Frameworks", Arrays.asList("Spring Boot", "React", "Node.js", "Angular", "Django"));
            skillsByCategory.put("Databases", Arrays.asList("MySQL", "PostgreSQL", "MongoDB", "Redis"));
            skillsByCategory.put("DevOps & Tools", Arrays.asList("Git", "Docker", "Kubernetes", "CI/CD", "Jenkins"));
            skillsByCategory.put("Concepts", Arrays.asList("REST API", "Microservices", "Agile", "TDD"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("data scientist") || lowerRole.contains("machine learning")) {
            skillsByCategory.put("Core Languages", Arrays.asList("Python", "R", "SQL", "Scala"));
            skillsByCategory.put("ML Frameworks", Arrays.asList("TensorFlow", "PyTorch", "Scikit-learn", "Keras"));
            skillsByCategory.put("Data Processing", Arrays.asList("Pandas", "NumPy", "Spark", "Hadoop"));
            skillsByCategory.put("Visualization", Arrays.asList("Matplotlib", "Seaborn", "Tableau", "Power BI"));
            skillsByCategory.put("Concepts", Arrays.asList("Machine Learning", "Deep Learning", "NLP", "Statistics"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("data analyst")) {
            skillsByCategory.put("Core Skills", Arrays.asList("SQL", "Excel", "Python", "R"));
            skillsByCategory.put("Visualization", Arrays.asList("Tableau", "Power BI", "Looker", "QlikView"));
            skillsByCategory.put("Analysis", Arrays.asList("Data Analysis", "Statistics", "Business Intelligence"));
            skillsByCategory.put("Tools", Arrays.asList("Google Analytics", "ETL", "Data Warehousing"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("frontend")) {
            skillsByCategory.put("Core Languages", Arrays.asList("JavaScript", "TypeScript", "HTML", "CSS"));
            skillsByCategory.put("Frameworks", Arrays.asList("React", "Vue.js", "Angular", "Svelte"));
            skillsByCategory.put("Styling", Arrays.asList("SASS", "Tailwind CSS", "Bootstrap", "Material-UI"));
            skillsByCategory.put("Tools", Arrays.asList("Webpack", "Babel", "npm", "Git"));
            skillsByCategory.put("Concepts", Arrays.asList("Responsive Design", "UI/UX", "Accessibility", "Performance"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("backend")) {
            skillsByCategory.put("Core Languages", Arrays.asList("Java", "Python", "Node.js", "Go", "PHP"));
            skillsByCategory.put("Frameworks", Arrays.asList("Spring Boot", "Express", "Django", "Flask"));
            skillsByCategory.put("Databases", Arrays.asList("MySQL", "PostgreSQL", "MongoDB", "Redis"));
            skillsByCategory.put("APIs", Arrays.asList("REST API", "GraphQL", "gRPC", "WebSockets"));
            skillsByCategory.put("Tools", Arrays.asList("Docker", "Kubernetes", "Git", "Postman"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("devops") || lowerRole.contains("sre")) {
            skillsByCategory.put("Containers", Arrays.asList("Docker", "Kubernetes", "Helm", "Podman"));
            skillsByCategory.put("CI/CD", Arrays.asList("Jenkins", "GitLab CI", "GitHub Actions", "CircleCI"));
            skillsByCategory.put("Cloud", Arrays.asList("AWS", "Azure", "GCP", "Terraform"));
            skillsByCategory.put("Monitoring", Arrays.asList("Prometheus", "Grafana", "ELK Stack", "Datadog"));
            skillsByCategory.put("Scripting", Arrays.asList("Bash", "Python", "Ansible", "Linux"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("cloud")) {
            skillsByCategory.put("Cloud Platforms", Arrays.asList("AWS", "Azure", "Google Cloud", "Oracle Cloud"));
            skillsByCategory.put("Services", Arrays.asList("EC2", "S3", "Lambda", "RDS", "CloudFront"));
            skillsByCategory.put("IaC", Arrays.asList("Terraform", "CloudFormation", "Ansible", "Pulumi"));
            skillsByCategory.put("Containers", Arrays.asList("Docker", "Kubernetes", "ECS", "EKS"));
            skillsByCategory.put("Concepts", Arrays.asList("Cloud Architecture", "Serverless", "Scalability", "Security"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("mobile")) {
            skillsByCategory.put("Frameworks", Arrays.asList("React Native", "Flutter", "Xamarin", "Ionic"));
            skillsByCategory.put("Native", Arrays.asList("Swift", "Kotlin", "Java", "Objective-C"));
            skillsByCategory.put("Tools", Arrays.asList("Xcode", "Android Studio", "Firebase", "TestFlight"));
            skillsByCategory.put("Concepts", Arrays.asList("Mobile UI/UX", "App Store", "Play Store", "Push Notifications"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("qa") || lowerRole.contains("test")) {
            skillsByCategory.put("Automation", Arrays.asList("Selenium", "Cypress", "Playwright", "Appium"));
            skillsByCategory.put("Frameworks", Arrays.asList("JUnit", "TestNG", "Jest", "Mocha"));
            skillsByCategory.put("Tools", Arrays.asList("JIRA", "TestRail", "Postman", "SoapUI"));
            skillsByCategory.put("Concepts", Arrays.asList("Test Automation", "API Testing", "Performance Testing", "CI/CD"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("product manager")) {
            skillsByCategory.put("Core Skills", Arrays.asList("Product Strategy", "Roadmap", "User Stories", "Prioritization"));
            skillsByCategory.put("Methodologies", Arrays.asList("Agile", "Scrum", "Kanban", "Lean"));
            skillsByCategory.put("Tools", Arrays.asList("JIRA", "Confluence", "Figma", "Analytics"));
            skillsByCategory.put("Analysis", Arrays.asList("Market Research", "Data Analysis", "A/B Testing", "Metrics"));
            return skillsByCategory;
        }
        
        if (lowerRole.contains("ui") || lowerRole.contains("ux") || lowerRole.contains("designer")) {
            skillsByCategory.put("Design Tools", Arrays.asList("Figma", "Adobe XD", "Sketch", "InVision"));
            skillsByCategory.put("Skills", Arrays.asList("UI/UX", "Wireframing", "Prototyping", "User Research"));
            skillsByCategory.put("Design Systems", Arrays.asList("Design Systems", "Component Libraries", "Style Guides"));
            skillsByCategory.put("Concepts", Arrays.asList("Responsive Design", "Accessibility", "Usability Testing", "Information Architecture"));
            return skillsByCategory;
        }
        
        // Default skills for any role
        skillsByCategory.put("Soft Skills", Arrays.asList("Communication", "Teamwork", "Problem Solving", "Leadership", "Time Management"));
        return skillsByCategory;
    }
    
    // Get all required skills (flattened from categories)
    private List<String> getRequiredSkills(String targetRole) {
        Map<String, List<String>> skillsByCategory = getRequiredSkillsByCategory(targetRole);
        List<String> allSkills = new ArrayList<>();
        
        for (List<String> skills : skillsByCategory.values()) {
            allSkills.addAll(skills);
        }
        
        return allSkills;
    }
    
    // Get role-specific keywords for density calculation
    private List<String> getRoleSpecificKeywords(String targetRole) {
        return getRequiredSkills(targetRole);
    }
    
    // Count occurrences of a keyword
    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
    
    // Get all available job roles (from database)
    public List<Map<String, String>> getAvailableJobRoles() {
        List<JobRole> jobRoles = jobRoleRepository.findByIsActiveTrue();
        
        if (jobRoles.isEmpty()) {
            // Fallback to hardcoded if database is empty
            return getHardcodedJobRoles();
        }
        
        List<Map<String, String>> roles = new ArrayList<>();
        for (JobRole jobRole : jobRoles) {
            Map<String, String> role = new HashMap<>();
            role.put("title", jobRole.getTitle());
            role.put("description", jobRole.getDescription());
            roles.add(role);
        }
        
        return roles;
    }
    
    // Fallback hardcoded job roles
    private List<Map<String, String>> getHardcodedJobRoles() {
        List<Map<String, String>> roles = new ArrayList<>();
        
        roles.add(createRole("Software Engineer", "Full-stack development with modern technologies"));
        roles.add(createRole("Frontend Developer", "UI/UX focused web development"));
        roles.add(createRole("Backend Developer", "Server-side and API development"));
        roles.add(createRole("Full Stack Developer", "End-to-end web application development"));
        roles.add(createRole("Data Scientist", "Machine learning and data analysis"));
        roles.add(createRole("Data Analyst", "Business intelligence and data visualization"));
        roles.add(createRole("Machine Learning Engineer", "AI and ML model development"));
        roles.add(createRole("DevOps Engineer", "CI/CD and infrastructure automation"));
        roles.add(createRole("Cloud Engineer", "Cloud infrastructure and services"));
        roles.add(createRole("Mobile Developer", "iOS and Android app development"));
        roles.add(createRole("QA Engineer", "Software testing and quality assurance"));
        roles.add(createRole("Product Manager", "Product strategy and roadmap"));
        roles.add(createRole("Project Manager", "Project planning and execution"));
        roles.add(createRole("UI/UX Designer", "User interface and experience design"));
        roles.add(createRole("Business Analyst", "Business requirements and analysis"));
        roles.add(createRole("Scrum Master", "Agile team facilitation"));
        roles.add(createRole("Database Administrator", "Database management and optimization"));
        roles.add(createRole("Security Engineer", "Cybersecurity and threat prevention"));
        roles.add(createRole("Site Reliability Engineer", "System reliability and performance"));
        roles.add(createRole("Technical Writer", "Documentation and technical content"));
        
        return roles;
    }
    
    private Map<String, String> createRole(String title, String description) {
        Map<String, String> role = new HashMap<>();
        role.put("title", title);
        role.put("description", description);
        return role;
    }
    
    // Helper method to get configuration value
    private double getConfigValue(String key, double defaultValue) {
        try {
            Optional<AtsConfiguration> config = atsConfigurationRepository.findByConfigKey(key);
            if (config.isPresent() && config.get().getIsActive()) {
                return Double.parseDouble(config.get().getConfigValue());
            }
        } catch (Exception e) {
            // Return default if parsing fails
        }
        return defaultValue;
    }
    
    private int getConfigValue(String key, int defaultValue) {
        try {
            Optional<AtsConfiguration> config = atsConfigurationRepository.findByConfigKey(key);
            if (config.isPresent() && config.get().getIsActive()) {
                return Integer.parseInt(config.get().getConfigValue());
            }
        } catch (Exception e) {
            // Return default if parsing fails
        }
        return defaultValue;
    }
}
