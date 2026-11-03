-- Lab 9: account model for Spring Security.
-- Login by e-mail, BCrypt password, role REGISTERED/USER/ADMIN, e-mail confirmation token.

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255);

-- password and role exist since V1: widen them and allow the new role REGISTERED.
ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users ALTER COLUMN password SET DATA TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN role SET DATA TYPE VARCHAR(32);
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'REGISTERED';
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('REGISTERED', 'USER', 'ADMIN'));

-- Data migration from the old login column.
UPDATE users
SET email = username
WHERE email IS NULL AND username LIKE '%_@_%._%';

-- Test accounts of lab 8 had plain logins (admin, mykhailo, ...): give them test addresses.
UPDATE users
SET email = LOWER(username) || '@bookshelf.test'
WHERE email IS NULL;

UPDATE users SET nickname = username WHERE nickname IS NULL;

-- Accounts created before lab 9 were trusted already: no confirmation needed.
UPDATE users SET verified = TRUE;

-- BCryptPasswordEncoder stores the bare hash, without the {bcrypt} id of DelegatingPasswordEncoder.
UPDATE users
SET password = SUBSTRING(password, 9)
WHERE password LIKE '{bcrypt}%';

ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE (email);
ALTER TABLE users ADD CONSTRAINT users_verification_token_unique UNIQUE (verification_token);
