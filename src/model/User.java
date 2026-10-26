package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an authenticated user in the MiniBank system.
 * Supports role-based access control (ADMIN vs CUSTOMER),
 * secure salted-hash password storage, and account lockout tracking.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ROLE_ADMIN    = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final String  username;
    private String        passwordHash;
    private String        salt;
    private final String  role;
    private final Integer customerId; // null for pure administrators
    private int           failedAttempts;
    private boolean       isLocked;

    public User(String username, String passwordHash, String salt, String role,
                Integer customerId, int failedAttempts, boolean isLocked) {
        this.username       = username;
        this.passwordHash   = passwordHash;
        this.salt           = salt;
        this.role           = role != null ? role.toUpperCase() : ROLE_CUSTOMER;
        this.customerId     = customerId;
        this.failedAttempts = failedAttempts;
        this.isLocked       = isLocked;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return ROLE_CUSTOMER.equalsIgnoreCase(role);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public String  getUsername()       { return username; }
    public String  getPasswordHash()   { return passwordHash; }
    public String  getSalt()           { return salt; }
    public String  getRole()           { return role; }
    public Integer getCustomerId()     { return customerId; }
    public int     getFailedAttempts() { return failedAttempts; }
    public boolean isLocked()          { return isLocked; }

    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public void setSalt(String salt)         { this.salt = salt; }
    public void setFailedAttempts(int count) { this.failedAttempts = count; }
    public void setLocked(boolean locked)    { this.isLocked = locked; }

    public void incrementFailedAttempts()    { this.failedAttempts++; }
    public void resetFailedAttempts()        { this.failedAttempts = 0; }

    @Override
    public String toString() {
        return String.format("User[username=%s, role=%s, custId=%s, locked=%b]",
                username, role, customerId != null ? customerId : "None", isLocked);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
