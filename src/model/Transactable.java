// Labs 5, 6 – Interfaces used by Account hierarchy
package model;

/**
 * Lab 6: Marks accounts that support deposits and withdrawals.
 */
public interface Transactable {
    void deposit(double amount) throws exception.InvalidAmountException;
    void withdraw(double amount) throws exception.InsufficientFundsException,
                                       exception.InvalidAmountException;
}
