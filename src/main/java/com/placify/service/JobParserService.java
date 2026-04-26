package com.placify.service;

import com.placify.model.EmailMessage;
import com.placify.model.EmailSource;
import com.placify.model.ParsedJob;
import com.placify.model.User;
import com.placify.repository.EmailSourceRepository;
import com.placify.repository.ParsedJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobParserService {

    private static final Logger log = LoggerFactory.getLogger(JobParserService.class);

    @Autowired private ParsedJobRepository parsedJobRepository;
    @Autowired private EmailSourceRepository emailSourceRepository;

    public ParsedJob parseAndSave(EmailMessage message, User user) {
        try {
            if (parsedJobRepository.existsByUserAndEmailMessageId(user, message.getId())) {
                return null;
            }

            EmailSource source = identifySource(message.getSenderEmail());
            if (source == null) return null;

            String text = message.getBodyText() != null ? message.getBodyText() : "";
            if (text.isBlank() && message.getBodyHtml() != null) {
                text = message.getBodyHtml()
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("\\s+", " ")
                    .trim();
            }

            if (!isJobEmail(text, message.getSubject())) return null;

            ParsedJob job = new ParsedJob();
            job.setUser(user);
            job.setSource(source.getName());
            job.setSourceColor(source.getColor());
            job.setSourceIcon(source.getIcon());
            job.setSenderEmail(message.getSenderEmail());
            job.setEmailMessageId(message.getId());
            job.setReceivedAt(message.getReceivedDate());

            job.setJobTitle(extractJobTitle(text, message.getSubject()));
            job.setCompanyName(extractCompanyName(text));
            job.setCompanyRating(extractRating(text));
            job.setLocation(extractLocation(text));
            job.setExperienceRequired(extractExperience(text));
            job.setSalaryRange(extractSalary(text));
            job.setWorkMode(extractWorkMode(text));
            job.setSkills(extractSkills(text));
            job.setApplyLink(extractApplyLink(text, message.getBodyHtml()));
            job.setJobDescription(extractDescription(text));

            log.info("Parsed job: {} @ {}", job.getJobTitle(), job.getCompanyName());
            return parsedJobRepository.save(job);

        } catch (Exception e) {
            log.error("Error parsing job from email {}", message.getId(), e);
            return null;
        }
    }

    private EmailSource identifySource(String senderEmail) {
        if (senderEmail == null) return null;
        String lower = senderEmail.toLowerCase();
        return emailSourceRepository.findByIsActiveTrue().stream()
            .filter(s -> lower.contains(s.getDomain().toLowerCase()))
            .findFirst().orElse(null);
    }

    private boolean isJobEmail(String text, String subject) {
        String combined = (text + " " + (subject != null ? subject : "")).toLowerCase();
        return combined.contains("apply") || combined.contains("job") ||
               combined.contains("position") || combined.contains("role") ||
               combined.contains("hiring") || combined.contains("opportunity") ||
               combined.contains("vacancy") || combined.contains("opening");
    }

    private String extractJobTitle(String text, String subject) {
        if (subject != null) {
            Pattern p = Pattern.compile(
                "(?:apply(?:ing)? (?:for|to)(?: this)? (?:job|role|position)[:\\s]+)([A-Z][\\w\\s,&/-]{3,60})",
                Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(subject);
            if (m.find()) return m.group(1).trim();
        }
        for (String pat : new String[]{
            "(?:position|role|job title|opening)[:\\s]+([A-Z][\\w\\s,&/-]{3,60})",
            "(?:apply for|applying for)[:\\s]+([A-Z][\\w\\s,&/-]{3,60})"
        }) {
            Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) return m.group(1).trim();
        }
        Matcher m = Pattern.compile(
            "(?:\\d\\.\\d\\s*\\(\\d+ Reviews?\\)\\s*)([A-Z][\\w\\s,&/-]{3,60})(?:\\n|\\r|Mumbai|Delhi|Bangalore|Pune|Hyderabad|Chennai)",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).trim();
        return subject != null ? subject.replaceAll("(?i)(re:|fwd:|fw:)\\s*", "").trim() : "Job Opportunity";
    }

    private String extractCompanyName(String text) {
        Pattern p = Pattern.compile(
            "(?:at|from|by|with)\\s+([A-Z][\\w\\s&.,'-]{2,50})(?:\\s+(?:is|are|has|have|Ltd|Inc|Pvt|Solutions|Technologies|Services))",
            Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1).trim();

        p = Pattern.compile("^([A-Z][\\w\\s&.,'-]{2,50})\\s*\\d\\.\\d\\s*\\(", Pattern.MULTILINE);
        m = p.matcher(text);
        if (m.find()) return m.group(1).trim();

        p = Pattern.compile("(?:company|employer|organization)[:\\s]+([A-Z][\\w\\s&.,'-]{2,50})", Pattern.CASE_INSENSITIVE);
        m = p.matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private String extractRating(String text) {
        Matcher m = Pattern.compile("(\\d\\.\\d)\\s*(?:stars?|/5|\\(\\d+\\s*Reviews?\\))", Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private String extractLocation(String text) {
        Matcher m = Pattern.compile(
            "\\b(Mumbai|Delhi|Bangalore|Bengaluru|Pune|Hyderabad|Chennai|Kolkata|Noida|Gurgaon|Gurugram|Ahmedabad|Jaipur|Surat|Lucknow|Chandigarh|Indore|Bhopal|Nagpur|Kochi|Coimbatore|Vizag|Visakhapatnam|Remote|Pan India)\\b",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1);
        m = Pattern.compile("(?:location|city|place)[:\\s]+([A-Z][\\w\\s,/-]{2,40})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private String extractExperience(String text) {
        Matcher m = Pattern.compile(
            "(\\d+\\s*[-–]\\s*\\d+\\s*(?:years?|yrs?)|fresher|0\\s*[-–]\\s*\\d+\\s*(?:years?|yrs?))",
            Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractSalary(String text) {
        Matcher m = Pattern.compile(
            "(\\d+(?:\\.\\d+)?\\s*(?:lacs?|lakhs?|LPA|lpa|CTC|ctc|k|K)\\s*[-–]\\s*\\d+(?:\\.\\d+)?\\s*(?:lacs?|lakhs?|LPA|lpa|CTC|ctc|k|K))",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).trim();
        m = Pattern.compile("(?:salary|ctc|package|compensation)[:\\s]+(\\d[\\d,.\\s\\-–lacs LPA lakhs CTC kK]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private String extractWorkMode(String text) {
        Matcher m = Pattern.compile("\\b(In[\\s-]?office|Work from home|WFH|Remote|Hybrid|On[\\s-]?site)\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractSkills(String text) {
        Matcher m = Pattern.compile("(?:skills?|technologies?|tech stack|requirements?)[:\\s]+([A-Za-z0-9,\\s.#+/-]{10,200})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).replaceAll("\\s+", " ").trim();
        m = Pattern.compile("([A-Za-z][A-Za-z0-9\\s]+(?:,\\s*[A-Za-z][A-Za-z0-9\\s]+){3,})(?=\\s*Apply)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private String extractApplyLink(String rawText, String htmlText) {
        if (htmlText != null && !htmlText.isBlank()) {
            // href near "Apply" text
            Matcher m = Pattern.compile(
                "<a[^>]+href=[\"']([^\"']{10,400})[\"'][^>]*>[^<]{0,50}(?:Apply|apply)[^<]*</a>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(htmlText);
            if (m.find()) return cleanUrl(m.group(1));

            // Any job-related href
            m = Pattern.compile("href=[\"'](https?://[^\"']{10,400})[\"']", Pattern.CASE_INSENSITIVE).matcher(htmlText);
            while (m.find()) {
                String url = cleanUrl(m.group(1));
                if (url.contains("naukri.com") || url.contains("linkedin.com/jobs") ||
                    url.contains("indeed.com") || url.contains("apply") || url.contains("/job")) {
                    return url;
                }
            }
        }

        Matcher m = Pattern.compile("https?://[\\w./?=&%+#@:~-]{10,400}").matcher(rawText);
        String first = null;
        while (m.find()) {
            String url = cleanUrl(m.group());
            if (first == null) first = url;
            if (url.contains("naukri.com") || url.contains("linkedin.com/jobs") ||
                url.contains("indeed.com") || url.contains("apply") || url.contains("/job")) {
                return url;
            }
        }
        return first;
    }

    private String cleanUrl(String url) {
        return url.replaceAll("[\"'>)\\]]+$", "")
                  .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                  .trim();
    }

    private String extractDescription(String text) {
        Matcher m = Pattern.compile(
            "(?:Job description|About the role|Role|Responsibilities)[:\\s]+(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
        if (m.find()) {
            String desc = m.group(1).trim();
            return desc.length() > 2000 ? desc.substring(0, 2000) + "..." : desc;
        }
        return text.length() > 2000 ? text.substring(0, 2000) + "..." : text;
    }
}
