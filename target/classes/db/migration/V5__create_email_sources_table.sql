-- Create email_sources table for managing allowed email sources
CREATE TABLE IF NOT EXISTS email_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255) NOT NULL,
    email_pattern VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    icon VARCHAR(50) NOT NULL,
    color VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_is_active (is_active),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default email sources
INSERT INTO email_sources (name, domain, email_pattern, is_active, icon, color) VALUES
('LinkedIn', 'linkedin.com', '@linkedin.com', TRUE, '💼', '#0A66C2'),
('Naukri', 'naukri.com', '@naukri.com', TRUE, '💼', '#4A90E2'),
('Superset', 'superset.com', '@superset.com', TRUE, '📊', '#20A7C9');
