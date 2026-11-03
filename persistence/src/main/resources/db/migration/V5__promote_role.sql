-- Lab 9: role of the administrator for the author's account.
-- In the methodical guide this role is called MODERATOR; the task text names it ADMIN.

UPDATE users
SET role = 'ADMIN'
WHERE email = 'mykhailo@bookshelf.test';
