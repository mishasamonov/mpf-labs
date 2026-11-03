-- Lab 9: the login is now the e-mail; legacy columns are no longer used.

ALTER TABLE users DROP CONSTRAINT uk_users_username;
ALTER TABLE users DROP COLUMN username;
ALTER TABLE users DROP COLUMN IF EXISTS display_name;

-- Passwords are always written by the application (BCrypt), never by default.
ALTER TABLE users ALTER COLUMN password DROP NOT NULL; -- Standard SQL: H2 and PostgreSQL.
