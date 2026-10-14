CREATE DATABASE IF NOT EXISTS elearning_db;
USE elearning_db;

CREATE TABLE IF NOT EXISTS students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS courses (
    course_id     INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(150) NOT NULL,
    total_lessons INT NOT NULL CHECK (total_lessons > 0)
);

CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id     INT AUTO_INCREMENT PRIMARY KEY,
    student_id        INT NOT NULL,
    course_id         INT NOT NULL,
    lessons_completed INT NOT NULL DEFAULT 0,
    status            ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
    enrolled_on       DATE NOT NULL DEFAULT (CURRENT_DATE),
    UNIQUE (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES courses(course_id)  ON DELETE CASCADE
);

-- Sample data
INSERT IGNORE INTO courses (course_id, title, total_lessons) VALUES
    (1, 'Java Fundamentals', 20),
    (2, 'Database Management with SQL', 15),
    (3, 'Cyber Security Basics', 12);
