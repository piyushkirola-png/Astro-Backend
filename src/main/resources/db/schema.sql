CREATE DATABASE IF NOT EXISTS astro CHARACTER
SET
    utf8mb4 COLLATE utf8mb4_unicode_ci;

USE astro;

CREATE TABLE
    IF NOT EXISTS users (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(100) NOT NULL,
        email VARCHAR(100) UNIQUE NOT NULL,
        password VARCHAR(255) NOT NULL,
        role ENUM ('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
        gender ENUM ('MALE', 'FEMALE') NULL,
        phone VARCHAR(20) NULL,
        date_of_birth DATE NULL,
        time_of_birth TIME NULL,
        place_of_birth VARCHAR(255) NULL,
        birth_lat DOUBLE NULL,
        birth_lng DOUBLE NULL,
        birth_timezone VARCHAR(20) NULL,
        current_address VARCHAR(255) NULL,
        city VARCHAR(100) NULL,
        state VARCHAR(100) NULL,
        country VARCHAR(100) NULL,
        pincode VARCHAR(10) NULL,
        avatar_url VARCHAR(255) NULL,
        zodiac_sign VARCHAR(20) NULL,
        chat_seconds_balance INT NOT NULL DEFAULT 120,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS horoscopes (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        zodiac_sign VARCHAR(20) NOT NULL,
        horoscope_date DATE NOT NULL,
        type VARCHAR(20) NOT NULL,
        content TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS revoked_tokens (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        token VARCHAR(512) NOT NULL UNIQUE,
        revoked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        expires_at TIMESTAMP NOT NULL
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS chat_sessions (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        title VARCHAR(255) NOT NULL DEFAULT 'New Chat',
        is_active BOOLEAN DEFAULT TRUE,
        is_pinned BOOLEAN DEFAULT FALSE,
        pinned_at TIMESTAMP NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
        INDEX idx_chat_sessions_user (user_id),
        INDEX idx_chat_sessions_pinned (user_id, is_pinned, pinned_at)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS chat_messages (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        session_id BIGINT NOT NULL,
        role ENUM ('USER', 'ASSISTANT', 'SYSTEM') NOT NULL,
        content TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (session_id) REFERENCES chat_sessions (id) ON DELETE CASCADE,
        INDEX idx_chat_messages_session (session_id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS content_library (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        signature VARCHAR(64) NOT NULL,
        category VARCHAR(32) NOT NULL,
        variant INT NOT NULL,
        language VARCHAR(16) NOT NULL,
        tone VARCHAR(16) NOT NULL DEFAULT 'NEUTRAL',
        text TEXT NOT NULL,
        UNIQUE KEY uq_content (signature, category, variant, language),
        INDEX idx_content_lookup (signature, category, language)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS daily_readings (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        reading_date DATE NOT NULL,
        signature VARCHAR(64) NOT NULL,
        lucky_number INT NOT NULL,
        lucky_color VARCHAR(32) NOT NULL,
        energy_level INT NOT NULL,
        mood_trend VARCHAR(16) NOT NULL,
        forecast_text TEXT NOT NULL,
        love_text TEXT,
        career_text TEXT,
        wellness_text TEXT,
        health_text TEXT,
        finance_text TEXT,
        language VARCHAR(16) NOT NULL DEFAULT 'HINGLISH',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uq_user_date (user_id, reading_date),
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
        INDEX idx_daily_user_date (user_id, reading_date)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS kundali_basic (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        zodiac VARCHAR(20) NOT NULL UNIQUE,
        panchang_tithi VARCHAR(64),
        karana VARCHAR(32),
        yoga VARCHAR(32),
        nakshatra VARCHAR(64),
        nakshatra_lord VARCHAR(32),
        ascendant VARCHAR(32),
        ascendant_lord VARCHAR(32),
        sunrise VARCHAR(16),
        sunset VARCHAR(16),
        varna VARCHAR(32),
        vashya VARCHAR(32),
        yoni VARCHAR(32),
        gan VARCHAR(32),
        nadi VARCHAR(32),
        sign VARCHAR(32),
        sign_lord VARCHAR(32),
        charan VARCHAR(8),
        tatva VARCHAR(16),
        name_alphabet VARCHAR(32),
        paya VARCHAR(32),
        yunja VARCHAR(32)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS kundali_chart (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        zodiac VARCHAR(20) NOT NULL,
        chart_type VARCHAR(4) NOT NULL,
        house1 VARCHAR(64),
        house2 VARCHAR(64),
        house3 VARCHAR(64),
        house4 VARCHAR(64),
        house5 VARCHAR(64),
        house6 VARCHAR(64),
        house7 VARCHAR(64),
        house8 VARCHAR(64),
        house9 VARCHAR(64),
        house10 VARCHAR(64),
        house11 VARCHAR(64),
        house12 VARCHAR(64),
        UNIQUE KEY uq_zodiac_chart (zodiac, chart_type)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS planetary_positions (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        zodiac VARCHAR(20) NOT NULL,
        planet VARCHAR(32) NOT NULL,
        sign VARCHAR(32),
        sign_lord VARCHAR(32),
        nakshatra VARCHAR(64),
        nakshatra_lord VARCHAR(32),
        degree VARCHAR(32),
        retro VARCHAR(8),
        house INT,
        state VARCHAR(16),
        status VARCHAR(32),
        INDEX idx_planetary_zodiac (zodiac)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS dasha_periods (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        zodiac VARCHAR(20) NOT NULL,
        planet VARCHAR(32) NOT NULL,
        start_offset_years INT NOT NULL,
        end_offset_years INT NOT NULL,
        house INT,
        sign VARCHAR(32),
        paragraph1 TEXT,
        paragraph2 TEXT,
        INDEX idx_dasha_zodiac (zodiac)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS payment_categories (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        code VARCHAR(32) NOT NULL UNIQUE,
        name VARCHAR(100) NOT NULL,
        amount DECIMAL(10, 2) NOT NULL,
        description VARCHAR(255),
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS wallet_packages (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        amount DECIMAL(10, 2) NOT NULL,
        seconds_credited INT NOT NULL,
        label VARCHAR(50) NOT NULL,
        display_order INT NOT NULL DEFAULT 0,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_wallet_active_order (is_active, display_order)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS payments (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        category_code VARCHAR(32) NULL,
        package_id BIGINT NULL,
        amount DECIMAL(10, 2) NOT NULL,
        base_amount DECIMAL(10, 2) NULL,
        gst_amount DECIMAL(10, 2) NULL,
        gst_rate DECIMAL(5, 2) NULL,
        invoice_number VARCHAR(32) NULL UNIQUE,
,
        currency VARCHAR(8) NOT NULL DEFAULT 'INR',
        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        gateway VARCHAR(32) NOT NULL,
        gateway_order_id VARCHAR(100) NOT NULL UNIQUE,
        gateway_payment_id VARCHAR(100) NULL,
        payment_link VARCHAR(500) NULL,
        utr VARCHAR(100) NULL,
        signature_verified BOOLEAN DEFAULT FALSE,
        customer_name VARCHAR(100) NULL,
        customer_email VARCHAR(100) NULL,
        customer_phone VARCHAR(20) NULL,
        payment_mode VARCHAR(32) NULL,
        seconds_credited INT NULL,
        failure_reason VARCHAR(500) NULL,
        failure_code VARCHAR(64) NULL,
        notes VARCHAR(500) NULL,
        metadata TEXT NULL,
        ip_address VARCHAR(45) NULL,
        user_agent VARCHAR(255) NULL,
        processed_at TIMESTAMP NULL,
        completed_at TIMESTAMP NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id),
        INDEX idx_payments_user (user_id),
        INDEX idx_payments_status (status),
        INDEX idx_payments_user_status (user_id, status),
        INDEX idx_payments_category (category_code),
        INDEX idx_payments_created (created_at)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS webhook_inbound_logs (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        gateway VARCHAR(32) NOT NULL,
        event_type VARCHAR(64) NULL,
        order_id VARCHAR(100) NULL,
        payment_id VARCHAR(100) NULL,
        amount DECIMAL(10, 2) NULL,
        status VARCHAR(20) NULL,
        signature_verified BOOLEAN DEFAULT FALSE,
        request_body TEXT NULL,
        request_headers TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_webhook_gateway (gateway),
        INDEX idx_webhook_order (order_id),
        INDEX idx_webhook_created (created_at)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS report_entries (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        type VARCHAR(20) NOT NULL,
        zodiac VARCHAR(20) NULL,
        category VARCHAR(64) NULL,
        title VARCHAR(255) NULL,
        content TEXT NULL,
        field1 VARCHAR(255) NULL,
        field2 VARCHAR(255) NULL,
        field3 VARCHAR(255) NULL,
        field4 VARCHAR(255) NULL,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_report_type (type),
        INDEX idx_report_zodiac (zodiac)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS horoscope_entries (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        zodiac VARCHAR(20) NOT NULL,
        period VARCHAR(20) NOT NULL,
        variant INT NOT NULL,
        content TEXT NULL,
        love_text TEXT NULL,
        career_text TEXT NULL,
        health_text TEXT NULL,
        money_text TEXT NULL,
        lucky_number INT NULL,
        lucky_color VARCHAR(32) NULL,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uq_horoscope_variant (zodiac, period, variant),
        INDEX idx_horoscope_lookup (zodiac, period, is_active)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS otp_codes (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        email VARCHAR(100) NOT NULL,
        code VARCHAR(6) NOT NULL,
        purpose VARCHAR(20) NOT NULL,
        expires_at TIMESTAMP NOT NULL,
        used BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_otp_email_purpose (email, purpose, used),
        INDEX idx_otp_expires (expires_at)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS password_reset_tokens (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        token VARCHAR(64) NOT NULL UNIQUE,
        expires_at TIMESTAMP NOT NULL,
        used BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id),
        INDEX idx_prt_token (token),
        INDEX idx_prt_user (user_id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS ai_usage_logs (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        session_id BIGINT NOT NULL,
        usage_date DATE NOT NULL,
        message_count INT NOT NULL DEFAULT 0,
        seconds_used INT NOT NULL DEFAULT 0,
        rupees_deducted DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uq_usage_user_session_date (user_id, session_id, usage_date),
        FOREIGN KEY (user_id) REFERENCES users (id),
        FOREIGN KEY (session_id) REFERENCES chat_sessions (id) ON DELETE CASCADE,
        INDEX idx_usage_user_date (user_id, usage_date)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;