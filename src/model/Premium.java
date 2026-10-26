// Lab 6 – Premium marker interface
package model;

/**
 * Marker interface for premium-tier accounts.
 * Lab 6: demonstrates marker interfaces.
 */
public interface Premium {
    /** Returns the set of premium benefits for this account type. */
    String[] getPremiumBenefits();
}
