package com.placify.controller;

import com.placify.model.*;
import com.placify.repository.*;
import com.placify.service.AtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatbotController {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private InterviewQuestionRepository interviewQuestionRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ParsedJobRepository parsedJobRepository;
    @Autowired private SalaryBreakdownRepository salaryBreakdownRepository;
    @Autowired private AtsService atsService;

    @PostMapping("/message")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String message = body.getOrDefault("message", "").trim();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        Map<String, Object> response = processMessage(message, user);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> processMessage(String msg, User user) {
        String lower = msg.toLowerCase();
        Map<String, Object> res = new HashMap<>();

        // ── Greetings ─────────────────────────────────────────────
        if (matches(lower, "hi", "hello", "hey", "good morning", "good evening", "namaste")) {
            return reply("👋 Hello! I'm Placify AI — your placement assistant.\n\nI can help you with:\n• 💰 Salary breakdown (e.g. *decode salary 1500000*)\n• 🏢 Company insights (e.g. *tell me about Google*)\n• 🎓 Interview questions (e.g. *interview questions for software engineer*)\n• 💼 Job listings (e.g. *show me jobs*)\n• 📊 Your parsed jobs (e.g. *my jobs*)\n• 📄 Resume tips (e.g. *how to improve my resume*)\n\nWhat would you like to know?",
                    quickReplies("Decode salary 1500000", "Tell me about Google", "Interview questions for SDE", "Show me jobs", "Resume tips"));
        }

        // ── Help ──────────────────────────────────────────────────
        if (matches(lower, "help", "what can you do", "commands", "options")) {
            return reply("Here's what I can do:\n\n💰 **Salary** — *decode salary 2500000*\n🏢 **Company** — *tell me about Amazon* or *company insights flipkart*\n🎓 **Interview** — *interview questions for data analyst* or *hr round questions*\n💼 **Jobs** — *show jobs* or *latest jobs*\n📊 **My Jobs** — *my parsed jobs* or *jobs from email*\n📄 **Resume** — *resume tips* or *how to improve ats score*\n🎯 **Skills** — *skills for frontend developer*",
                    quickReplies("Decode salary 1200000", "About Google", "Interview SDE", "Show jobs"));
        }

        // ── Salary ────────────────────────────────────────────────
        if (matches(lower, "salary", "ctc", "decode", "lpa", "lakh", "package", "in hand", "take home")) {
            double ctc = extractNumber(msg);
            if (ctc > 0) {
                return handleSalary(ctc);
            }
            return reply("💰 Please tell me the CTC amount.\n\nExamples:\n• *decode salary 1500000*\n• *what is in-hand for 25 LPA*\n• *salary breakdown 800000*",
                    quickReplies("Decode salary 500000", "Decode salary 1200000", "Decode salary 2500000", "Decode salary 4000000"));
        }

        // ── Company ───────────────────────────────────────────────
        if (matches(lower, "company", "about", "insights", "hiring", "interview process", "culture")) {
            String companyName = extractCompanyName(msg);
            if (companyName != null) {
                return handleCompany(companyName);
            }
            // List all companies
            List<Company> companies = companyRepository.findAll();
            if (companies.isEmpty()) {
                return reply("No companies in the database yet. Ask your admin to add some!");
            }
            String list = companies.stream()
                    .map(c -> "• " + c.getName() + (c.getIndustry() != null ? " (" + c.getIndustry() + ")" : ""))
                    .collect(Collectors.joining("\n"));
            return reply("🏢 **Available Companies:**\n\n" + list + "\n\nAsk me about any of them! e.g. *tell me about Google*",
                    companies.stream().limit(5).map(c -> "About " + c.getName()).collect(Collectors.toList()));
        }

        // ── Interview Questions ───────────────────────────────────
        if (matches(lower, "interview", "question", "round", "hr round", "technical round", "system design", "behavioral", "machine coding")) {
            return handleInterview(msg, lower);
        }

        // ── Jobs ──────────────────────────────────────────────────
        if (matches(lower, "job", "jobs", "opening", "vacancy", "hiring", "position")) {
            if (matches(lower, "my", "parsed", "email", "from email", "naukri", "linkedin")) {
                return handleParsedJobs(user);
            }
            return handleJobs();
        }

        // ── Resume ────────────────────────────────────────────────
        if (matches(lower, "resume", "ats", "cv", "improve", "score", "keyword")) {
            return handleResumeTips();
        }

        // ── Skills ────────────────────────────────────────────────
        if (matches(lower, "skill", "skills", "learn", "technology", "tech stack")) {
            String role = extractRole(msg);
            return handleSkills(role);
        }

        // ── Placement tips ────────────────────────────────────────
        if (matches(lower, "tip", "tips", "advice", "placement", "crack", "prepare", "preparation")) {
            return handlePlacementTips();
        }

        // ── Fallback ──────────────────────────────────────────────
        return reply("🤔 I didn't quite understand that. Here are some things I can help with:",
                quickReplies("Decode salary 1500000", "Tell me about Google", "Interview questions for SDE", "Resume tips", "Show jobs"));
    }

    // ── Handlers ─────────────────────────────────────────────────

    private Map<String, Object> handleSalary(double ctc) {
        double basic       = round(ctc * 0.40);
        double hra         = round(basic * 0.50);
        double variable    = round(ctc * 0.10);
        double pf          = round(Math.min(basic * 0.12, 21600));
        double gratuity    = round(basic * 0.0481);
        double insurance   = round(Math.min(ctc * 0.005, 15000));
        double grossAnnual = round(basic + hra + variable + (ctc - basic - hra - variable - pf - gratuity - insurance));
        double taxableIncome = Math.max(0, grossAnnual - 75000); // new regime
        double tax         = calculateNewRegimeTax(taxableIncome);
        double cess        = round(tax * 0.04);
        double totalTax    = round(tax + cess);
        double monthlyInHand = round((grossAnnual - totalTax - pf - insurance) / 12);

        String text = String.format(
            "💰 **Salary Breakdown for ₹%s CTC**\n\n" +
            "📊 **Annual Components:**\n" +
            "• Basic Salary: ₹%s\n" +
            "• HRA: ₹%s\n" +
            "• Variable Pay: ₹%s\n" +
            "• PF (Employee): ₹%s\n" +
            "• Gratuity: ₹%s\n\n" +
            "🧾 **Tax (New Regime):**\n" +
            "• Taxable Income: ₹%s\n" +
            "• Total Tax + Cess: ₹%s\n" +
            "• Effective Rate: %.1f%%\n\n" +
            "✅ **Monthly In-Hand: ₹%s**\n" +
            "✅ **Annual In-Hand: ₹%s**",
            fmt(ctc), fmt(basic), fmt(hra), fmt(variable), fmt(pf), fmt(gratuity),
            fmt(taxableIncome), fmt(totalTax),
            (totalTax / grossAnnual) * 100,
            fmt(monthlyInHand), fmt(monthlyInHand * 12)
        );

        Map<String, Object> res = reply(text, quickReplies("Decode salary 500000", "Decode salary 2500000", "Decode salary 5000000", "Resume tips"));
        res.put("type", "salary");
        res.put("data", Map.of("ctc", ctc, "monthlyInHand", monthlyInHand, "totalTax", totalTax));
        return res;
    }

    private Map<String, Object> handleCompany(String name) {
        Optional<Company> opt = companyRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(name.toLowerCase()))
                .findFirst();

        if (opt.isEmpty()) {
            List<Company> all = companyRepository.findAll();
            String names = all.stream().map(Company::getName).collect(Collectors.joining(", "));
            return reply("🏢 I couldn't find **" + name + "**.\n\nAvailable companies: " + names,
                    all.stream().limit(4).map(c -> "About " + c.getName()).collect(Collectors.toList()));
        }

        Company c = opt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("🏢 **").append(c.getName()).append("**\n");
        if (c.getIndustry() != null) sb.append("📌 ").append(c.getIndustry());
        if (c.getLocation() != null) sb.append(" · ").append(c.getLocation());
        sb.append("\n\n");
        if (c.getDescription() != null) sb.append(c.getDescription()).append("\n\n");
        if (c.getSalaryRange() != null) sb.append("💰 Salary: ").append(c.getSalaryRange()).append("\n");
        if (c.getDifficulty() != null) sb.append("🎯 Interview Difficulty: ").append(c.getDifficulty()).append("\n");
        if (c.getWorkCulture() != null) sb.append("🏢 Work Culture: ").append(c.getWorkCulture()).append("\n");
        if (c.getRating() != null) sb.append("⭐ Rating: ").append(c.getRating()).append("/5\n");
        if (c.getHiringStrategy() != null) sb.append("\n📋 **Hiring Strategy:**\n").append(c.getHiringStrategy());

        Map<String, Object> res = reply(sb.toString(),
                quickReplies("Interview process at " + c.getName(), "Tips for " + c.getName(), "Skills for " + c.getName()));
        res.put("type", "company");
        res.put("company", Map.of("name", c.getName(), "difficulty", c.getDifficulty() != null ? c.getDifficulty() : "MEDIUM"));
        return res;
    }

    private Map<String, Object> handleInterview(String msg, String lower) {
        // Detect round type
        String roundType = null;
        if (lower.contains("hr")) roundType = "HR";
        else if (lower.contains("system design")) roundType = "SYSTEM_DESIGN";
        else if (lower.contains("machine coding") || lower.contains("machine round")) roundType = "MACHINE_CODING";
        else if (lower.contains("behavioral") || lower.contains("behaviour")) roundType = "BEHAVIORAL";
        else if (lower.contains("managerial")) roundType = "MANAGERIAL";
        else if (lower.contains("technical")) roundType = "TECHNICAL";

        String role = extractRole(msg);

        List<InterviewQuestion> questions;
        if (role != null && roundType != null) {
            questions = interviewQuestionRepository.findByJobRoleAndRoundType(role, roundType);
            questions.addAll(interviewQuestionRepository.findByJobRoleAndRoundType("All Roles", roundType));
        } else if (role != null) {
            questions = interviewQuestionRepository.findByJobRole(role);
            questions.addAll(interviewQuestionRepository.findByJobRole("All Roles"));
        } else if (roundType != null) {
            questions = interviewQuestionRepository.findByRoundType(roundType);
        } else {
            questions = interviewQuestionRepository.findAll();
        }

        if (questions.isEmpty()) {
            return reply("🎓 No interview questions found for that criteria.\n\nTry:\n• *HR round questions*\n• *Technical interview for software engineer*\n• *System design questions*",
                    quickReplies("HR round questions", "Technical interview SDE", "System design questions", "Behavioral questions"));
        }

        // Pick 5 random questions
        Collections.shuffle(questions);
        List<InterviewQuestion> sample = questions.stream().limit(5).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        String label = (role != null ? role + " " : "") + (roundType != null ? roundType.replace("_", " ") : "Interview");
        sb.append("🎓 **").append(label).append(" Questions:**\n\n");
        for (int i = 0; i < sample.size(); i++) {
            InterviewQuestion q = sample.get(i);
            sb.append(i + 1).append(". **").append(q.getQuestion()).append("**\n");
            if (q.getDifficulty() != null) sb.append("   _").append(q.getDifficulty()).append("_\n");
            sb.append("\n");
        }
        sb.append("_Showing ").append(sample.size()).append(" of ").append(questions.size()).append(" questions_");

        List<String> qr = new ArrayList<>();
        qr.add("Show answers");
        if (roundType == null) qr.add("HR round questions");
        if (!"TECHNICAL".equals(roundType)) qr.add("Technical questions");
        if (!"SYSTEM_DESIGN".equals(roundType)) qr.add("System design questions");

        Map<String, Object> res = reply(sb.toString(), qr);
        res.put("type", "questions");
        res.put("questions", sample.stream().map(q -> Map.of(
                "question", q.getQuestion(),
                "answer", q.getAnswer() != null ? q.getAnswer() : "",
                "difficulty", q.getDifficulty() != null ? q.getDifficulty() : "MEDIUM",
                "roundType", q.getRoundType() != null ? q.getRoundType() : "TECHNICAL"
        )).collect(Collectors.toList()));
        return res;
    }

    private Map<String, Object> handleJobs() {
        List<Job> jobs = jobRepository.findAll();
        if (jobs.isEmpty()) {
            return reply("💼 No jobs in the database yet. Check back later or ask your admin to add some!",
                    quickReplies("My parsed jobs", "Resume tips", "Interview questions"));
        }
        StringBuilder sb = new StringBuilder("💼 **Available Jobs:**\n\n");
        jobs.stream().limit(8).forEach(j -> {
            sb.append("• **").append(j.getTitle()).append("** at ").append(j.getCompany());
            if (j.getSalary() != null) sb.append(" — ₹").append(fmt(j.getSalary()));
            sb.append("\n");
        });
        if (jobs.size() > 8) sb.append("\n_...and ").append(jobs.size() - 8).append(" more_");
        return reply(sb.toString(), quickReplies("My parsed jobs", "Interview questions", "Resume tips"));
    }

    private Map<String, Object> handleParsedJobs(User user) {
        if (user == null) return reply("Please log in to see your parsed jobs.");
        List<ParsedJob> jobs = parsedJobRepository.findTop5ByUserOrderByReceivedAtDesc(user);
        if (jobs.isEmpty()) {
            return reply("📭 No parsed jobs found. Go to the Dashboard and click **Parse Emails** to extract jobs from your inbox!",
                    quickReplies("How to parse emails", "Show jobs", "Interview questions"));
        }
        StringBuilder sb = new StringBuilder("📊 **Your Recent Parsed Jobs:**\n\n");
        jobs.forEach(j -> {
            sb.append("• **").append(j.getJobTitle() != null ? j.getJobTitle() : "Job").append("**");
            if (j.getCompanyName() != null) sb.append(" at ").append(j.getCompanyName());
            if (j.getSalaryRange() != null) sb.append(" — ").append(j.getSalaryRange());
            if (j.getSource() != null) sb.append(" [").append(j.getSource()).append("]");
            sb.append("\n");
        });
        return reply(sb.toString(), quickReplies("Interview questions", "Resume tips", "Decode salary 1500000"));
    }

    private Map<String, Object> handleResumeTips() {
        String text = "📄 **Resume Tips to Boost Your ATS Score:**\n\n" +
            "1. **Use keywords** from the job description — ATS scans for exact matches\n" +
            "2. **Quantify achievements** — '40% performance improvement' beats 'improved performance'\n" +
            "3. **Action verbs** — Start bullets with: Developed, Led, Optimized, Reduced, Built\n" +
            "4. **Standard sections** — Summary, Experience, Education, Skills, Projects\n" +
            "5. **Avoid tables/graphics** — ATS can't parse them\n" +
            "6. **File format** — Submit as PDF unless specified otherwise\n" +
            "7. **Certifications** — AWS, Azure, PMP, Scrum Master boost your score\n" +
            "8. **Optimal length** — 1-2 pages, 300-800 words\n\n" +
            "💡 Use the **Resume Optimizer** to get your ATS score!";
        return reply(text, quickReplies("Skills for software engineer", "Interview questions", "Decode salary 1500000"));
    }

    private Map<String, Object> handleSkills(String role) {
        if (role == null) {
            return reply("🎯 Which role are you asking about?\n\nExamples:\n• *skills for software engineer*\n• *skills for data analyst*\n• *skills for frontend developer*",
                    quickReplies("Skills for software engineer", "Skills for data analyst", "Skills for frontend developer", "Skills for backend developer"));
        }

        // Try to get from DB
        List<InterviewQuestion> questions = interviewQuestionRepository.findByJobRole(role);
        List<Company> companies = companyRepository.findAll().stream()
                .filter(c -> c.getTechStack() != null)
                .limit(3)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("💻 **Skills for " + role + ":**\n\n");

        // Hardcoded skill maps for common roles
        Map<String, String> skillMap = new HashMap<>();
        skillMap.put("software engineer", "**Core:** Java, Python, C++, JavaScript\n**Frameworks:** Spring Boot, React, Node.js\n**Databases:** MySQL, PostgreSQL, MongoDB, Redis\n**DevOps:** Docker, Kubernetes, Git, CI/CD\n**Concepts:** DSA, System Design, REST APIs, Microservices");
        skillMap.put("frontend developer", "**Core:** JavaScript, TypeScript, HTML5, CSS3\n**Frameworks:** React, Vue.js, Angular\n**Styling:** Tailwind CSS, SASS, Material-UI\n**Tools:** Webpack, npm, Git, Figma\n**Concepts:** Responsive Design, Performance, Accessibility");
        skillMap.put("backend developer", "**Core:** Java, Python, Go, Node.js\n**Frameworks:** Spring Boot, Django, Express, FastAPI\n**Databases:** MySQL, PostgreSQL, MongoDB, Redis\n**APIs:** REST, GraphQL, gRPC\n**Tools:** Docker, Kubernetes, Postman, Git");
        skillMap.put("data analyst", "**Core:** SQL, Python, Excel, R\n**Visualization:** Tableau, Power BI, Matplotlib\n**Analysis:** Statistics, Pandas, NumPy\n**Concepts:** Data Warehousing, ETL, Business Intelligence");
        skillMap.put("data scientist", "**Core:** Python, R, SQL, Scala\n**ML:** TensorFlow, PyTorch, Scikit-learn\n**Data:** Pandas, NumPy, Spark\n**Concepts:** Machine Learning, Deep Learning, NLP, Statistics");
        skillMap.put("devops engineer", "**Containers:** Docker, Kubernetes, Helm\n**CI/CD:** Jenkins, GitHub Actions, GitLab CI\n**Cloud:** AWS, Azure, GCP, Terraform\n**Monitoring:** Prometheus, Grafana, ELK Stack\n**Scripting:** Bash, Python, Ansible");

        String lowerRole = role.toLowerCase();
        String skills = skillMap.entrySet().stream()
                .filter(e -> lowerRole.contains(e.getKey()) || e.getKey().contains(lowerRole))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("**General Skills:** Problem Solving, Communication, Teamwork, Adaptability, Continuous Learning");

        sb.append(skills);
        sb.append("\n\n💡 Use the **Resume Optimizer** to check how well your resume matches these skills!");

        return reply(sb.toString(), quickReplies("Interview questions for " + role, "Decode salary 1500000", "Resume tips"));
    }

    private Map<String, Object> handlePlacementTips() {
        String text = "🎯 **Placement Preparation Tips:**\n\n" +
            "**📚 Technical Preparation:**\n" +
            "• Practice 150+ LeetCode problems (Easy→Medium→Hard)\n" +
            "• Master System Design (HLD + LLD)\n" +
            "• Know your tech stack deeply\n\n" +
            "**📝 Resume:**\n" +
            "• Tailor resume for each company\n" +
            "• Quantify every achievement\n" +
            "• Keep it to 1-2 pages\n\n" +
            "**🎤 Interviews:**\n" +
            "• Practice STAR method for behavioral questions\n" +
            "• Think out loud during coding rounds\n" +
            "• Ask clarifying questions before solving\n\n" +
            "**🏢 Company Research:**\n" +
            "• Know the company's products and tech stack\n" +
            "• Understand their business model\n" +
            "• Prepare questions to ask the interviewer\n\n" +
            "**⏰ Timeline:**\n" +
            "• 3 months before: DSA + System Design\n" +
            "• 1 month before: Mock interviews + Resume polish\n" +
            "• 1 week before: Company-specific research";
        return reply(text, quickReplies("Interview questions", "Resume tips", "Skills for software engineer"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private double extractNumber(String text) {
        // Handle "25 LPA", "25L", "2500000"
        text = text.toLowerCase().replaceAll(",", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:lpa|lakh|lac|l\\b|crore|cr)?").matcher(text);
        while (m.find()) {
            double val = Double.parseDouble(m.group(1));
            String suffix = m.group(0).toLowerCase();
            if (suffix.contains("crore") || suffix.contains("cr")) val *= 10000000;
            else if (suffix.contains("lpa") || suffix.contains("lakh") || suffix.contains("lac") || (suffix.endsWith("l") && val < 1000)) val *= 100000;
            if (val >= 100000) return val;
        }
        return 0;
    }

    private String extractCompanyName(String msg) {
        List<Company> companies = companyRepository.findAll();
        String lower = msg.toLowerCase();
        return companies.stream()
                .filter(c -> lower.contains(c.getName().toLowerCase()))
                .map(Company::getName)
                .findFirst()
                .orElse(null);
    }

    private String extractRole(String msg) {
        String lower = msg.toLowerCase();
        String[] roles = {"software engineer", "frontend developer", "backend developer", "full stack developer",
                "data scientist", "data analyst", "devops engineer", "machine learning engineer",
                "product manager", "qa engineer", "mobile developer", "cloud engineer",
                "sde", "sde2", "sde1", "senior engineer", "junior developer"};
        for (String role : roles) {
            if (lower.contains(role)) return role.substring(0, 1).toUpperCase() + role.substring(1);
        }
        return null;
    }

    private double calculateNewRegimeTax(double income) {
        if (income <= 300000) return 0;
        if (income <= 700000) return (income - 300000) * 0.05;
        if (income <= 1000000) return 20000 + (income - 700000) * 0.10;
        if (income <= 1200000) return 50000 + (income - 1000000) * 0.15;
        if (income <= 1500000) return 80000 + (income - 1200000) * 0.20;
        return 140000 + (income - 1500000) * 0.30;
    }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private String fmt(double v) {
        if (v >= 10000000) return String.format("%.1f Cr", v / 10000000);
        if (v >= 100000) return String.format("%.1f L", v / 100000);
        return String.format("%.0f", v);
    }

    private Map<String, Object> reply(String text, List<String> quickReplies) {
        Map<String, Object> r = new HashMap<>();
        r.put("text", text);
        r.put("quickReplies", quickReplies);
        r.put("type", "text");
        return r;
    }

    private Map<String, Object> reply(String text) {
        return reply(text, Collections.emptyList());
    }

    private List<String> quickReplies(String... items) {
        return Arrays.asList(items);
    }
}
