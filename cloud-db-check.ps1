# Complex lab, task 5.2: contents of the Render PostgreSQL tables (psql from the postgres:16 image).
# Copy "External Database URL" on the Render page of mpf-labs-db, then run: .\cloud-db-check.ps1
# The URL (with the password) is read from the clipboard and is never written to a file.
$url = (Get-Clipboard | Select-Object -First 1).Trim()
if ($url -notlike 'postgresql://*') { throw 'Copy the External Database URL from Render first' }
$sql = @'
\echo '== flyway_schema_history'
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;
\echo '== users (latest)'
SELECT id, email, nickname, role, verified FROM users ORDER BY id DESC LIMIT 5;
\echo '== books (latest)'
SELECT id, title, author, pub_year FROM books ORDER BY id DESC LIMIT 5;
\echo '== comments (latest)'
SELECT id, book_id, author, LEFT(text, 50) AS text, created_at FROM comments ORDER BY id DESC LIMIT 5;
'@
$sql | docker run --rm -i postgres:16-alpine psql $url -P pager=off
