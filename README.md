# Placify Backend

A comprehensive Spring Boot-based recruitment and career development platform backend that integrates email services, resume analysis, job parsing, and AI-powered features to help job seekers optimize their career journey.

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Key Features](#key-features)
- [Database Schema](#database-schema)
- [Services](#services)
- [Security](#security)
- [Development](#development)

## Overview

Placify Backend is a full-featured REST API that powers the Placify platform. It provides:

- **User Authentication & Authorization**: JWT-based authentication with role-based access control
- **Email Integration**: OAuth-based Gmail and Outlook integration with scheduled email syncing
- **Job Parsing**: Intelligent extraction of job details from emails using regex patterns
- **Resume Analysis**: PDF parsing and ATS score calculation
- **Company Insights**: Company ratings, interview processes, and salary data
- **Interview Preparation**: Interview questions and preparation materials
- **AI Chatbot**: Context-aware chatbot for career guidance
- **Admin Dashboard**: User and system management

## Technology Stack

- **Framework**: Spring Boot 2.7.18
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: MySQL
- **Authentication**: JWT (JJWT 0.11.5) + Spring Security
- **Email APIs**: 
  - Google Gmail API
  - Microsoft Graph API
  - SendGrid
- **PDF Processing**: Apache PDFBox 2.0.29
- **Encryption**: Jasypt Spring Boot
- **ORM**: Spring Data JPA / Hibernate
- **Logging**: SLF4J

## Project Structure

```
backend/
├── src/main/java/com/placify/
│   ├── controller/              # REST API endpoints (18 controllers)
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── ResumeController.java
│   │   ├── JobController.java
│   │   ├── ParsedJobController.java
│   │   ├── CompanyController.java
│   │   ├── SalaryController.java
│   │   ├── InterviewController.java
│   │   ├── ChatbotController.java
│   │   ├── EmailOAuthController.java
│   │   ├── EmailMessageController.java
│   │   ├── EmailSourceController.java
│   │   ├── EmailDashboardController.java
│   │   ├── EmailWebhookController.java
│   │   ├── AdminController.java
│   │   ├── AdminUserController.java
│   │   ├── AtsAdminController.java
│   │   └── TestController.java
│   │
│   ├── service/                 # Business logic (16 services)
│   │   ├── AtsService.java
│   │   ├── EmailService.java
│   │   ├── EmailSyncService.java
│   │   ├── EmailProcessingService.java
│   │   ├── EmailParserService.java
│   │   ├── EmailVerificationService.java
│   │   ├── GmailService.java
│   │   ├── GoogleOAuthService.java
│   │   ├── OutlookService.java
│   │   ├── MicrosoftOAuthService.java
│   │   ├── JobParserService.java
│   │   ├── PasswordResetService.java
│   │   ├── PdfService.java
│   │   ├── FileStorageService.java
│   │   ├── EncryptionService.java
│   │   └── UserMatcherService.java
│   │
│   ├── model/                   # JPA entities (18 models)
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Resume.java
│   │   ├── Job.java
│   │   ├── ParsedJob.java
│   │   ├── EmailAccount.java
│   │   ├── EmailMessage.java
│   │   ├── EmailEntry.java
│   │   ├── EmailSource.java
│   │   ├── EmailVerificationToken.java
│   │   ├── PasswordResetToken.java
│   │   ├── Company.java
│   │   ├── InterviewQuestion.java
│   │   ├── AtsConfiguration.java
│   │   ├── SalaryBreakdown.java
│   │   ├── ActionVerb.java
│   │   ├── SkillKeyword.java
│   │   └── JobRole.java
│   │
│   ├── repository/              # Data access layer (18 repositories)
│   │   ├── UserRepository.java
│   │   ├── RoleRepository.java
│   │   ├── ResumeRepository.java
│   │   ├── JobRepository.java
│   │   ├── ParsedJobRepository.java
│   │   ├── EmailAccountRepository.java
│   │   ├── EmailMessageRepository.java
│   │   ├── EmailEntryRepository.java
│   │   ├── EmailSourceRepository.java
│   │   ├── EmailVerificationTokenRepository.java
│   │   ├── PasswordResetTokenRepository.java
│   │   ├── CompanyRepository.java
│   │   ├── InterviewQuestionRepository.java
│   │   ├── AtsConfigurationRepository.java
│   │   ├── SalaryBreakdownRepository.java
│   │   ├── ActionVerbRepository.java
│   │   ├── SkillKeywordRepository.java
│   │   └── JobRoleRepository.java
│   │
│   ├── dto/                     # Data Transfer Objects (18 DTOs)
│   │   ├── LoginRequest.java
│   │   ├── SignupRequest.java
│   │   ├── JwtResponse.java
│   │   ├── MessageResponse.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   ├── ResetPasswordRequest.java
│   │   ├── ResetPasswordWithOtpRequest.java
│   │   ├── VerifyOtpRequest.java
│   │   ├── EmailAccountDTO.java
│   │   ├── EmailMessageDTO.java
│   │   ├── EmailDetailDTO.java
│   │   ├── EmailEntryDTO.java
│   │   ├── EmailSourceDTO.java
│   │   ├── EmailWebhookRequest.java
│   │   ├── OAuthUrlResponse.java
│   │   ├── ParsedJobDTO.java
│   │   └── ProcessingResult.java
│   │
│   ├── config/                  # Configuration classes
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   └── DataInitializer.java
│   │
│   ├── security/                # Security utilities
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   └── CustomUserDetailsService.java
│   │
│   └── PlacifyApplication.java  # Main application class
│
├── src/main/resources/
│   ├── application.properties   # Application configuration
│   └── application-dev.properties
│
├── pom.xml                      # Maven dependencies
└── mvnw                         # Maven wrapper
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd backend
   ```

2. **Configure environment variables**
   Create `application.properties` in `src/main/resources/`:
   ```properties
   # Server
   server.port=8081
   server.servlet.context-path=/api
   
   # Database
   spring.datasource.url=jdbc:mysql://localhost:3306/placify
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=false
   
   # JWT
   app.jwtSecret=your_jwt_secret_key_min_32_chars
   app.jwtExpirationMs=86400000
   
   # Email (SendGrid)
   sendgrid.api.key=your_sendgrid_api_key
   sendgrid.from.email=noreply@placify.com
   
   # Google OAuth
   google.client.id=your_google_client_id
   google.client.secret=your_google_client_secret
   google.redirect.uri=http://localhost:8081/api/email/oauth/callback/google
   
   # Microsoft OAuth
   microsoft.client.id=your_microsoft_client_id
   microsoft.client.secret=your_microsoft_client_secret
   microsoft.redirect.uri=http://localhost:8081/api/email/oauth/callback/microsoft
   
   # Encryption
   jasypt.encryptor.password=your_encryption_password
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The API will be available at `http://localhost:8081/api`

## Configuration

### Database Setup

Create MySQL database:
```sql
CREATE DATABASE placify;
USE placify;
```

### Security Configuration

- **JWT Secret**: Generate a strong secret key (minimum 32 characters)
- **CORS**: Configured for `http://localhost:5173` and `http://localhost:5174` (frontend dev servers)
- **Password Encoding**: BCrypt with strength 10
- **Session Management**: Stateless (JWT-based)

### Email Configuration

#### SendGrid Setup
1. Create SendGrid account and get API key
2. Add to `application.properties`:
   ```properties
   sendgrid.api.key=SG.xxxxx
   sendgrid.from.email=noreply@placify.com
   ```

#### Google OAuth Setup
1. Create Google Cloud project
2. Enable Gmail API
3. Create OAuth 2.0 credentials (Web application)
4. Add authorized redirect URI: `http://localhost:8081/api/email/oauth/callback/google`
5. Add credentials to `application.properties`

#### Microsoft OAuth Setup
1. Create Azure AD application
2. Enable Microsoft Graph API
3. Create client secret
4. Add authorized redirect URI: `http://localhost:8081/api/email/oauth/callback/microsoft`
5. Add credentials to `application.properties`

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | User login |
| POST | `/auth/signup` | User registration |
| POST | `/auth/verify-email` | Verify email with OTP |
| POST | `/auth/resend-verification` | Resend verification email |
| POST | `/auth/forgot-password` | Request password reset |
| POST | `/auth/reset-password` | Reset password with OTP |

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/user/profile` | Get user profile |
| PUT | `/user/profile` | Update user profile |
| GET | `/user/{id}` | Get user by ID |

### Resume Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/resume/upload` | Upload resume PDF |
| GET | `/resume/{id}` | Get resume details |
| GET | `/resume/user/{userId}` | Get user's resumes |
| DELETE | `/resume/{id}` | Delete resume |

### Job Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/jobs` | Get all jobs |
| GET | `/jobs/{id}` | Get job details |
| POST | `/jobs` | Create job |
| PUT | `/jobs/{id}` | Update job |
| DELETE | `/jobs/{id}` | Delete job |

### Parsed Jobs (from emails)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/parsed-jobs` | Get parsed jobs |
| GET | `/parsed-jobs/{id}` | Get parsed job details |
| DELETE | `/parsed-jobs/{id}` | Delete parsed job |

### Email Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/email/oauth/url` | Get OAuth authorization URL |
| GET | `/email/oauth/callback` | OAuth callback handler |
| GET | `/email/oauth/accounts` | List connected email accounts |
| DELETE | `/email/oauth/accounts/{accountId}` | Disconnect email account |
| GET | `/email/messages` | Get emails |
| GET | `/email/messages/{messageId}` | Get email details |
| PUT | `/email/messages/{messageId}/read` | Mark email as read |
| POST | `/email/messages/sync/{accountId}` | Sync emails |
| POST | `/email/webhook` | Email webhook endpoint |

### Company Insights

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/company` | Get all companies |
| GET | `/company/{id}` | Get company details |
| POST | `/company` | Create company |
| PUT | `/company/{id}` | Update company |

### Salary Analysis

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/salary` | Get salary data |
| GET | `/salary/{id}` | Get salary details |
| POST | `/salary/analyze` | Analyze salary |

### Interview Preparation

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/interview/questions` | Get interview questions |
| GET | `/interview/questions/{id}` | Get question details |
| POST | `/interview/questions` | Create question |

### Chatbot

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/chatbot/message` | Send message to chatbot |
| GET | `/chatbot/history` | Get chat history |

### Admin Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/users` | List all users |
| PUT | `/admin/users/{id}/roles` | Update user roles |
| PUT | `/admin/users/{userId}/toggle-status` | Toggle user status |
| GET | `/admin/ats-config` | Get ATS configuration |
| PUT | `/admin/ats-config` | Update ATS configuration |

## Key Features

### 1. Email Integration

**Scheduled Email Sync**
- Runs every 15 minutes
- Fetches emails from Gmail and Outlook
- Stores emails in database
- Automatically parses job-related emails

**OAuth Support**
- Google Gmail API integration
- Microsoft Graph API integration
- Secure token storage with encryption
- Automatic token refresh

**Job Parsing**
- Regex-based extraction of job details
- Identifies job title, company, salary, location, skills
- Extracts apply links
- Detects work mode (Remote, Hybrid, On-site)

### 2. Resume Analysis

**PDF Processing**
- Extracts text from PDF resumes
- Preserves formatting information
- Handles multi-page documents

**ATS Score Calculation**
- Analyzes technical skills
- Evaluates experience level
- Checks keyword matching
- Provides optimization recommendations

### 3. Authentication & Security

**JWT-based Authentication**
- Stateless token-based auth
- 24-hour token expiration
- Automatic token refresh capability

**Email Verification**
- OTP-based email verification
- 10-minute OTP expiration
- Resend verification email option

**Password Reset**
- OTP-based password reset
- Secure token generation
- Email notification

**Role-based Access Control**
- ADMIN: Full system access
- USER: Standard user features
- EMPLOYEE: Limited features

### 4. Company Insights

- Company ratings and reviews
- Interview process details
- Required skills and tech stack
- Salary ranges by role
- Work culture information
- Difficulty levels (EASY, MEDIUM, HARD)

### 5. AI Chatbot

- Context-aware responses
- Salary query handling
- Company information queries
- Career guidance

## Database Schema

### Core Entities

**User**
- id (PK)
- username (unique)
- email (unique)
- password (hashed)
- fullName
- phone
- profilePicture
- roles (M2M with Role)
- enabled
- emailVerified
- createdAt
- updatedAt

**EmailAccount**
- id (PK)
- user_id (FK)
- provider (GMAIL, OUTLOOK)
- emailAddress
- accessToken (encrypted)
- refreshToken (encrypted)
- tokenExpiresAt
- isActive
- lastSyncAt
- createdAt
- updatedAt

**EmailMessage**
- id (PK)
- emailAccount_id (FK)
- messageId (provider-specific)
- subject
- senderEmail
- senderName
- bodyText
- bodyHtml
- receivedDate
- isRead
- createdAt

**ParsedJob**
- id (PK)
- user_id (FK)
- emailMessage_id (FK)
- source (LinkedIn, Naukri, Indeed, etc.)
- jobTitle
- companyName
- companyRating
- location
- experienceRequired
- salaryRange
- workMode
- skills
- applyLink
- jobDescription
- receivedAt

**Resume**
- id (PK)
- user_id (FK)
- fileName
- filePath
- fileSize
- fileType
- atsScore
- uploadedAt
- analyzedAt

**Company**
- id (PK)
- name
- rating
- interviewProcess
- requiredSkills
- techStack
- salaryRanges
- workCulture
- difficulty

### Indexes

- `idx_user_id` on EmailMessage, ParsedJob, Resume
- `idx_source` on ParsedJob
- `idx_received_at` on ParsedJob, EmailMessage
- `idx_is_read` on EmailMessage
- `idx_email_account_id` on EmailMessage

## Services

### AtsService
Calculates ATS (Applicant Tracking System) scores for resumes based on:
- Technical skills matching
- Experience level
- Keyword density
- Format compliance

### EmailSyncService
Scheduled service that:
- Runs every 15 minutes
- Fetches emails from connected accounts
- Processes and stores emails
- Triggers job parsing

### JobParserService
Extracts job information from emails using:
- Regex patterns for job titles
- Company name extraction
- Salary range parsing
- Location identification
- Skill extraction

### GmailService & OutlookService
Handle email API interactions:
- Fetch emails
- Parse email content
- Extract attachments
- Manage labels/folders

### PasswordResetService
Manages password reset flow:
- Generate OTP
- Send reset email
- Validate OTP
- Update password

### PdfService
Processes PDF resumes:
- Extract text content
- Preserve formatting
- Handle multi-page documents

## Security

### Authentication Flow

1. User signs up with email/password
2. Password hashed with BCrypt
3. Verification email sent with OTP
4. User verifies email
5. User logs in with credentials
6. JWT token issued (24-hour expiration)
7. Token stored in localStorage (frontend)
8. Token included in Authorization header for API requests

### Token Security

- JWT signed with HS512 algorithm
- Secret key minimum 32 characters
- Token includes user ID and roles
- Automatic logout on token expiration

### Data Encryption

- OAuth tokens encrypted with Jasypt
- Sensitive data encrypted at rest
- HTTPS recommended for production

### CORS Configuration

```properties
# Allowed origins
http://localhost:5173
http://localhost:5174
```

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package -DskipTests
```

### Docker Build

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/placify-backend-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Logging

Configure logging in `application.properties`:
```properties
logging.level.root=INFO
logging.level.com.placify=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Common Issues

**Issue**: Database connection failed
- **Solution**: Verify MySQL is running and credentials are correct

**Issue**: Gmail API not working
- **Solution**: Ensure Gmail API is enabled in Google Cloud Console and credentials are valid

**Issue**: Email sync not running
- **Solution**: Check if `@EnableScheduling` is present in main application class

**Issue**: JWT token validation fails
- **Solution**: Verify JWT secret key is consistent across application restarts

## Contributing

1. Create a feature branch
2. Make changes
3. Test thoroughly
4. Submit pull request

## License

Proprietary - Placify Platform

## Support

For issues and questions, contact the development team.
