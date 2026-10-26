// Lab 6 – InterestBearing interface
package model;

/**
 * Marks accounts that accrue interest.
 * Lab 6: interface with default method.
 */
public interface InterestBearing {
    double getInterestRate();
    void   applyInterest();

    /** Returns a human-readable description of the interest type. */
    default String interestDescription() {
        return String.format("Interest rate: %.2f%%", getInterestRate() * 100);
    }
}
