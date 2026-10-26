package service;

import exception.BankException;
import exception.ValidationException;
import model.User;
import service.db.DatabaseManager;
import util.SecurityUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Service managing user authentication, password verification with PBKDF2-HMAC-SHA256,
 * role authorization, and brute-force lockout protection (5 failed attempts).
 */
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    private final DatabaseManager db;

    public AuthService(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Authenticates a user with username and password.
     * Enforces account lockout on repeated failed attempts.
     */
    public User login(String username, String password) throws BankException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("credentials", "Username and password must not be empty.");
        }

        try {
            Optional<User> optUser = db.findUserByUsername(username.trim());
            if (optUser.isEmpty()) {
                throw new BankException("AUTH_FAILED", "Invalid username or password.");
            }

            User user = optUser.get();
            if (user.isLocked()) {
                throw new BankException("USER_LOCKED",
                        "User account '" + username + "' is locked due to repeated failed login attempts. Contact an administrator.");
            }

            boolean matches = SecurityUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());
            if (!matches) {
                user.incrementFailedAttempts();
                int remaining = MAX_FAILED_ATTEMPTS - user.getFailedAttempts();
                if (remaining <= 0) {
                    user.setLocked(true);
                    db.saveUser(user);
                    throw new BankException("USER_LOCKED",
                            "Account has been locked due to 5 consecutive failed login attempts. Please contact an administrator.");
                } else {
                    db.saveUser(user);
                    throw new BankException("AUTH_FAILED",
                            "Invalid username or password. Remaining attempts before lockout: " + remaining);
                }
            }

            // Successful login: reset failed counter
            if (user.getFailedAttempts() > 0) {
                user.resetFailedAttempts();
                db.saveUser(user);
            }

            return user;

        } catch (SQLException e) {
            throw new BankException("DB_ERROR", "Authentication error: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a new customer login account.
     */
    public User registerCustomerUser(int customerId, String username, String plainPassword) throws BankException {
        if (username == null || username.trim().length() < 3) {
            throw new ValidationException("username", "Username must be at least 3 characters long.");
        }
        if (plainPassword == null || plainPassword.length() < 4) {
            throw new ValidationException("password", "Password must be at least 4 characters long.");
        }

        try {
            if (db.findUserByUsername(username.trim()).isPresent()) {
                throw new ValidationException("username", "Username '" + username + "' is already taken.");
            }

            String salt = SecurityUtil.generateSalt();
            String hash = SecurityUtil.hashPassword(plainPassword, salt);
            User user = new User(username.trim(), hash, salt, User.ROLE_CUSTOMER, customerId, 0, false);
            db.saveUser(user);
            return user;
        } catch (SQLException e) {
            throw new BankException("DB_ERROR", "Error registering user: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the initial system administrator.
     */
    public User createFirstAdmin(String username, String plainPassword) throws BankException {
        if (username == null || username.trim().length() < 3) {
            throw new ValidationException("username", "Admin username must be at least 3 characters long.");
        }
        if (plainPassword == null || plainPassword.length() < 4) {
            throw new ValidationException("password", "Admin password must be at least 4 characters long.");
        }

        try {
            String salt = SecurityUtil.generateSalt();
            String hash = SecurityUtil.hashPassword(plainPassword, salt);
            User admin = new User(username.trim(), hash, salt, User.ROLE_ADMIN, null, 0, false);
            db.saveUser(admin);
            return admin;
        } catch (SQLException e) {
            throw new BankException("DB_ERROR", "Error creating administrator: " + e.getMessage(), e);
        }
    }

    public boolean hasAdminUser() {
        try {
            return db.hasAdminUser();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Changes password for an authenticated user.
     */
    public void changePassword(String username, String oldPassword, String newPassword) throws BankException {
        if (newPassword == null || newPassword.length() < 4) {
            throw new ValidationException("newPassword", "New password must be at least 4 characters long.");
        }

        try {
            User user = db.findUserByUsername(username)
                    .orElseThrow(() -> new BankException("USER_NOT_FOUND", "User not found."));

            if (!SecurityUtil.verifyPassword(oldPassword, user.getSalt(), user.getPasswordHash())) {
                throw new BankException("AUTH_FAILED", "Current password is incorrect.");
            }

            String newSalt = SecurityUtil.generateSalt();
            String newHash = SecurityUtil.hashPassword(newPassword, newSalt);
            user.setSalt(newSalt);
            user.setPasswordHash(newHash);
            user.resetFailedAttempts();
            db.saveUser(user);
        } catch (SQLException e) {
            throw new BankException("DB_ERROR", "Error updating password: " + e.getMessage(), e);
        }
    }

    /**
     * Unlocks a locked user account (Admin operation).
     */
    public void unlockUser(String username) throws BankException {
        try {
            User user = db.findUserByUsername(username)
                    .orElseThrow(() -> new BankException("USER_NOT_FOUND", "User '" + username + "' not found."));
            user.setLocked(false);
            user.resetFailedAttempts();
            db.saveUser(user);
        } catch (SQLException e) {
            throw new BankException("DB_ERROR", "Error unlocking user: " + e.getMessage(), e);
        }
    }
}
