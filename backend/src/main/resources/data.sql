INSERT INTO app_user (username, password, role, create_userid, create_datetime)
VALUES (
  'admin',
  '{bcrypt}$2a$10$7QJZ0uYQZpQkzV6YQ9Z7Oe3Z9cQ5ZfQ5ZfQ5ZfQ5ZfQ5ZfQ5ZfQ5Zf', -- password = admin123
  'ROLE_ADMIN',
  'system',
  NOW()
);

INSERT INTO member (first_name, last_name, phone, email, status, join_date, address1, city, state, zip, create_userid, create_datetime)
VALUES ('John', 'Doe', '612-555-1234', 'john@example.com', 'Active', '2024-01-15', '123 Main St', 'Minneapolis', 'MN', '55401', 'admin', NOW());