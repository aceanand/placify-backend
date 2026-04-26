-- Create email_entries table for storing parsed email data
CREATE TABLE IF NOT EXISTS email_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DOUBLE NOT NULL,
    department VARCHAR(255) NOT NULL,
    received_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_received_at (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
