CREATE DATABASE hestia_db;

USE hestia_db;


-- =====================================================
-- 1. USERS
-- Stores buyer and admin information
-- =====================================================

CREATE TABLE Users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,

    name VARCHAR(100) NOT NULL,

    email VARCHAR(100) UNIQUE NOT NULL,

    password VARCHAR(255) NOT NULL,

    role ENUM('BUYER', 'ADMIN') NOT NULL DEFAULT 'BUYER',

    phone VARCHAR(15),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================
-- 2. PROPERTIES
-- Stores real-estate property information
-- =====================================================

CREATE TABLE Properties (
    property_id INT PRIMARY KEY AUTO_INCREMENT,

    title VARCHAR(150) NOT NULL,

    description TEXT,

    property_type ENUM(
        'APARTMENT',
        'HOUSE',
        'VILLA',
        'PLOT',
        'OTHER'
    ) NOT NULL,

    location VARCHAR(150) NOT NULL,

    city VARCHAR(100) NOT NULL,

    state VARCHAR(100),

    price DECIMAL(15,2) NOT NULL,

    area_sqft DECIMAL(10,2) NOT NULL,

    bedrooms INT,

    bathrooms INT,

    status ENUM(
        'AVAILABLE',
        'SOLD',
        'RESERVED',
        'INACTIVE'
    ) DEFAULT 'AVAILABLE',

    listed_by INT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (listed_by)
        REFERENCES Users(user_id)
        ON DELETE SET NULL
);


-- =====================================================
-- 3. PROPERTY IMAGES
-- Stores multiple images for each property
-- =====================================================

CREATE TABLE Property_Images (
    image_id INT PRIMARY KEY AUTO_INCREMENT,

    property_id INT NOT NULL,

    image_url VARCHAR(500) NOT NULL,

    is_primary BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (property_id)
        REFERENCES Properties(property_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 4. FAVOURITES
-- Stores properties saved by buyers
-- =====================================================

CREATE TABLE Favourites (
    favourite_id INT PRIMARY KEY AUTO_INCREMENT,

    user_id INT NOT NULL,

    property_id INT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (user_id, property_id),

    FOREIGN KEY (user_id)
        REFERENCES Users(user_id)
        ON DELETE CASCADE,

    FOREIGN KEY (property_id)
        REFERENCES Properties(property_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 5. INTERESTS
-- Stores buyer interest in properties
-- =====================================================

CREATE TABLE Interests (
    interest_id INT PRIMARY KEY AUTO_INCREMENT,

    user_id INT NOT NULL,

    property_id INT NOT NULL,

    status ENUM(
        'EXPRESSED',
        'CONTACTED',
        'CLOSED',
        'WITHDRAWN'
    ) DEFAULT 'EXPRESSED',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE (user_id, property_id),

    FOREIGN KEY (user_id)
        REFERENCES Users(user_id)
        ON DELETE CASCADE,

    FOREIGN KEY (property_id)
        REFERENCES Properties(property_id)
        ON DELETE CASCADE
);


-- =====================================================
-- 6. ACTIVITY HISTORY
-- Records buyer activities
-- =====================================================

CREATE TABLE Activity_History (
    activity_id INT PRIMARY KEY AUTO_INCREMENT,

    user_id INT NOT NULL,

    property_id INT,

    activity_type ENUM(
        'VIEWED',
        'SAVED',
        'UNSAVED',
        'INTERESTED',
        'EMI_CALCULATED'
    ) NOT NULL,

    activity_details VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES Users(user_id)
        ON DELETE CASCADE,

    FOREIGN KEY (property_id)
        REFERENCES Properties(property_id)
        ON DELETE SET NULL
);


-- =====================================================
-- 7. SAMPLE USERS
-- =====================================================

INSERT INTO Users
(name, email, password, role, phone)
VALUES
('HESTIA Admin', 'admin@hestia.com', 'admin123', 'ADMIN', '9000000001'),

('Faizaan', 'demo@hestia.com', '123456', 'BUYER', '9000000002'),

('Ishaan', 'ishaan@hestia.com', '123456', 'BUYER', '9000000003'),

('Abhiram', 'abhiram@hestia.com', '123456', 'BUYER', '9000000004');


-- =====================================================
-- 8. SAMPLE PROPERTIES
-- =====================================================

INSERT INTO Properties
(title, description, property_type, location, city, state,
 price, area_sqft, bedrooms, bathrooms, status, listed_by)
VALUES

(
    'Modern 3BHK Apartment',
    'Modern apartment with premium interiors and excellent connectivity.',
    'APARTMENT',
    'Kondapur',
    'Hyderabad',
    'Telangana',
    8500000.00,
    1650,
    3,
    3,
    'AVAILABLE',
    1
),

(
    'Luxury Villa',
    'Spacious luxury villa with garden and modern amenities.',
    'VILLA',
    'Gachibowli',
    'Hyderabad',
    'Telangana',
    18500000.00,
    3200,
    4,
    4,
    'AVAILABLE',
    1
),

(
    '2BHK City Apartment',
    'Comfortable apartment close to major facilities.',
    'APARTMENT',
    'Miyapur',
    'Hyderabad',
    'Telangana',
    5200000.00,
    1100,
    2,
    2,
    'AVAILABLE',
    1
),

(
    'Residential Plot',
    'Residential plot suitable for house construction.',
    'PLOT',
    'Kompally',
    'Hyderabad',
    'Telangana',
    4500000.00,
    2400,
    NULL,
    NULL,
    'AVAILABLE',
    1
);


-- =====================================================
-- 9. PROPERTY IMAGES
-- =====================================================

INSERT INTO Property_Images
(property_id, image_url, is_primary)
VALUES

(1, 'property1_main.jpg', TRUE),
(1, 'property1_2.jpg', FALSE),

(2, 'property2_main.jpg', TRUE),
(2, 'property2_2.jpg', FALSE),

(3, 'property3_main.jpg', TRUE),

(4, 'property4_main.jpg', TRUE);


-- =====================================================
-- 10. SAMPLE FAVOURITES
-- =====================================================

INSERT INTO Favourites
(user_id, property_id)
VALUES
(2, 1),
(2, 2),
(3, 3);


-- =====================================================
-- 11. SAMPLE INTERESTS
-- =====================================================

INSERT INTO Interests
(user_id, property_id, status)
VALUES
(2, 1, 'EXPRESSED'),
(3, 2, 'CONTACTED');


-- =====================================================
-- 12. SAMPLE ACTIVITY HISTORY
-- =====================================================

INSERT INTO Activity_History
(user_id, property_id, activity_type, activity_details)
VALUES
(2, 1, 'VIEWED', 'Viewed property details'),
(2, 1, 'SAVED', 'Added property to favourites'),
(2, 1, 'EMI_CALCULATED', 'Calculated home loan EMI'),
(2, 1, 'INTERESTED', 'Expressed interest in property'),
(3, 2, 'VIEWED', 'Viewed property details'),
(3, 2, 'INTERESTED', 'Expressed interest in property');


-- =====================================================
-- 13. VIEW
-- Shows currently available properties
-- =====================================================

CREATE VIEW Available_Properties AS
SELECT
    property_id,
    title,
    property_type,
    location,
    city,
    price,
    area_sqft,
    bedrooms,
    bathrooms
FROM Properties
WHERE status = 'AVAILABLE';
