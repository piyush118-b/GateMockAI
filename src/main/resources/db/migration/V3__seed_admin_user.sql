-- Default admin user. Password is 'Admin@123' BCrypt encoded.
INSERT INTO users (email, password_hash, full_name, role)
VALUES (
    'admin@gate.com',
    '$2a$12$PXODam0F/iz3NZN3iA/.F.JA0nHmLwfit440oSehci3OhQOggXu86',
    'Platform Admin',
    'ADMIN'
);
