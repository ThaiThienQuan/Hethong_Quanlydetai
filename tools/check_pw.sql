SELECT id, username, LEFT(password_hash, 30) AS hash_prefix, status, enabled FROM users WHERE id IN (1,2,3,11) ;
SELECT COUNT(*) AS users_with_real_hash FROM users WHERE password_hash LIKE '{bcrypt}$2a$10$%' AND password_hash NOT LIKE '%toBeReplaced%';
SELECT COUNT(*) AS users_placeholder FROM users WHERE password_hash LIKE '%toBeReplaced%';
