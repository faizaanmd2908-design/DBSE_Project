-- HESTIA full schema matching existing hestia_db design
CREATE DATABASE IF NOT EXISTS hestia_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hestia_db;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL,
  phone VARCHAR(15) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('BUYER','ADMIN') NOT NULL DEFAULT 'BUYER',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY email (email),
  UNIQUE KEY phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS properties (
  property_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  property_type ENUM('APARTMENT','HOUSE','VILLA','PLOT','OTHER') NOT NULL,
  location VARCHAR(150) NOT NULL,
  city VARCHAR(100) NOT NULL,
  state VARCHAR(100) DEFAULT NULL,
  pincode VARCHAR(10) DEFAULT NULL,
  price DECIMAL(15,2) NOT NULL,
  area_sqft DECIMAL(10,2) NOT NULL,
  bedrooms TINYINT UNSIGNED DEFAULT NULL,
  bathrooms TINYINT UNSIGNED DEFAULT NULL,
  status ENUM('AVAILABLE','SOLD','RESERVED','INACTIVE') NOT NULL DEFAULT 'AVAILABLE',
  listed_by BIGINT UNSIGNED DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (property_id),
  KEY fk_property_user (listed_by),
  CONSTRAINT fk_property_user FOREIGN KEY (listed_by) REFERENCES users (user_id) ON DELETE SET NULL,
  CONSTRAINT chk_property_area CHECK (area_sqft > 0),
  CONSTRAINT chk_property_price CHECK (price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS property_images (
  image_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  property_id BIGINT UNSIGNED NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  is_primary TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (image_id),
  KEY fk_image_property (property_id),
  CONSTRAINT fk_image_property FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS favourites (
  favourite_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  property_id BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (favourite_id),
  UNIQUE KEY uq_user_property_favourite (user_id, property_id),
  KEY fk_favourite_property (property_id),
  CONSTRAINT fk_favourite_property FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE,
  CONSTRAINT fk_favourite_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interests (
  interest_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  property_id BIGINT UNSIGNED NOT NULL,
  status ENUM('EXPRESSED','CONTACTED','CLOSED','WITHDRAWN') NOT NULL DEFAULT 'EXPRESSED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (interest_id),
  UNIQUE KEY uq_user_property_interest (user_id, property_id),
  KEY fk_interest_property (property_id),
  CONSTRAINT fk_interest_property FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE CASCADE,
  CONSTRAINT fk_interest_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_history (
  activity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  property_id BIGINT UNSIGNED DEFAULT NULL,
  activity_type ENUM('VIEWED','SAVED','UNSAVED','INTERESTED','EMI_CALCULATED') NOT NULL,
  activity_details VARCHAR(255) DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (activity_id),
  KEY fk_activity_user (user_id),
  KEY idx_activity_property (property_id),
  CONSTRAINT fk_activity_property FOREIGN KEY (property_id) REFERENCES properties (property_id) ON DELETE SET NULL,
  CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE OR REPLACE VIEW available_properties AS
SELECT p.property_id, p.title, p.description, p.property_type, p.location, p.city, p.state,
       p.price, p.area_sqft, p.bedrooms, p.bathrooms, p.status, p.created_at,
       (SELECT image_url FROM property_images pi WHERE pi.property_id = p.property_id AND pi.is_primary = 1 LIMIT 1) AS primary_image
FROM properties p
WHERE p.status = 'AVAILABLE';
