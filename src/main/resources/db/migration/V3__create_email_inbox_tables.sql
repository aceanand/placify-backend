-- Create email_accounts table
CREATE TABLE IF NOT EXISTS email_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    token_expires_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_email (user_id, email_address)
);

-- Create email_messages table
CREATE TABLE IF NOT EXISTS email_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    thread_id VARCHAR(255),
    sender_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255),
    subject TEXT,
    preview TEXT,
    body_html LONGTEXT,
    body_text LONGTEXT,
    received_date TIMESTAMP NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    has_attachments BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES email_accounts(id) ON DELETE CASCADE,
    UNIQUE KEY unique_account_message (account_id, message_id),
    INDEX idx_account_date (account_id, received_date DESC),
    INDEX idx_account_read (account_id, is_read)
);
