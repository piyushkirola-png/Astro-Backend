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
        free_messages_used INT NOT NULL DEFAULT 0,
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
    IF NOT EXISTS bookings (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        booking_type VARCHAR(50) NOT NULL,
        booking_date TIMESTAMP NOT NULL,
        status VARCHAR(20) DEFAULT 'PENDING',
        amount DECIMAL(10, 2),
        notes TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE
    IF NOT EXISTS transactions (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        booking_id BIGINT,
        amount DECIMAL(10, 2) NOT NULL,
        payment_method VARCHAR(50),
        transaction_id VARCHAR(100) UNIQUE,
        status VARCHAR(20) DEFAULT 'PENDING',
        gateway_response TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id),
        FOREIGN KEY (booking_id) REFERENCES bookings (id)
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
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
        INDEX idx_chat_sessions_user (user_id)
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