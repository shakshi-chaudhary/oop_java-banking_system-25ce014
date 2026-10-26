// Lab 6 – WithdrawRule interface
package model;

/**
 * Strategy interface for custom withdraw validation rules.
 * Lab 6: demonstrates interface as strategy pattern.
 */
@FunctionalInterface
public interface WithdrawRule {
    /**
     * @param balance  current balance before withdraw
     * @param amount   requested withdraw amount
     * @return true if the withdrawal is allowed
     */
    boolean isAllowed(double balance, double amount);
}
