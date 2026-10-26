// Lab 1 – BankInfo record and MenuOption enum
// A Java record is an immutable data carrier; fields are final and auto-generated.
package model;

/**
 * Immutable bank metadata (name, branch, IFSC).
 * Lab 1: demonstrates Java records introduced in Java 16.
 */
public record BankInfo(String name, String branch, String ifscCode) {

    /** Compact canonical constructor – validates non-null fields. */
    public BankInfo {
        if (name == null || name.isBlank())    throw new IllegalArgumentException("Bank name required");
        if (branch == null || branch.isBlank()) throw new IllegalArgumentException("Branch required");
        if (ifscCode == null || ifscCode.isBlank()) throw new IllegalArgumentException("IFSC required");
    }

    @Override
    public String toString() {
        return "Bank: " + name + " | Branch: " + branch + " | IFSC: " + ifscCode;
    }
}
