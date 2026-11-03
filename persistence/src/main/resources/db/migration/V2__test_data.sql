-- Lab 8: initial test data. Passwords are BCrypt hashes (test accounts only).

INSERT INTO books (id, title, author, pub_year) VALUES
    (1, 'Чистий код', 'Роберт Мартін', 2008),
    (2, 'Ефективна Java', 'Джошуа Блох', 2018),
    (3, 'Рефакторинг', 'Мартін Фаулер', 2018),
    (4, 'Предметно орієнтоване проєктування', 'Ерік Еванс', 2003),
    (5, 'Java Concurrency in Practice', 'Браян Гетц', 2006),
    (6, 'Чиста архітектура', 'Роберт Мартін', 2017),
    (7, 'Java Persistence with Hibernate', 'Крістіан Бауер, Гевін Кінг', 2016);

INSERT INTO users (id, username, password, role) VALUES
    (1, 'admin',    '{bcrypt}$2a$10$VvzHlJ8ZWRr6KOO5ZJ3odujsjMBNIQG39OAKsC.fCHJ6cSRRYapMG', 'ADMIN'),
    (2, 'mykhailo', '{bcrypt}$2a$10$d52CojSZHxQbDQiKlcdFqOjFoDB5xo45Gb6suTdGTQ2doxdism1PO', 'USER'),
    (3, 'olena',    '{bcrypt}$2a$10$d52CojSZHxQbDQiKlcdFqOjFoDB5xo45Gb6suTdGTQ2doxdism1PO', 'USER'),
    (4, 'andrii',   '{bcrypt}$2a$10$d52CojSZHxQbDQiKlcdFqOjFoDB5xo45Gb6suTdGTQ2doxdism1PO', 'USER');

INSERT INTO comments (book_id, user_id, author, text, created_at) VALUES
    (1, 2, 'mykhailo', 'Після цієї книги по-іншому називаю змінні та методи.', TIMESTAMP WITH TIME ZONE '2026-10-28 18:20:00+02:00'),
    (7, 2, 'mykhailo', 'Найкраще пояснення каскадів і LAZY-завантаження, яке я читав.', TIMESTAMP WITH TIME ZONE '2026-10-30 10:05:00+02:00'),
    (3, 2, 'mykhailo', 'Каталог рефакторингів зручно тримати під рукою.', TIMESTAMP WITH TIME ZONE '2026-11-01 21:40:00+02:00'),
    (2, 3, 'olena', 'Розділ про record та незмінні об''єкти особливо корисний.', TIMESTAMP WITH TIME ZONE '2026-10-29 12:15:00+02:00'),
    (7, 3, 'olena', 'Зрозуміла різницю між mappedBy та @JoinColumn.', TIMESTAMP WITH TIME ZONE '2026-10-31 09:30:00+02:00'),
    (4, 4, 'andrii', 'Агрегати та обмежені контексти описано дуже доступно.', TIMESTAMP WITH TIME ZONE '2026-10-27 16:00:00+02:00'),
    (6, 1, 'admin', 'Рекомендовано для курсу «Сучасні фреймворки програмування».', TIMESTAMP WITH TIME ZONE '2026-10-26 08:45:00+02:00');

-- Explicit ids were used above; new rows continue from 100.
ALTER TABLE books ALTER COLUMN id RESTART WITH 100;
ALTER TABLE users ALTER COLUMN id RESTART WITH 100;
ALTER TABLE comments ALTER COLUMN id RESTART WITH 100;
