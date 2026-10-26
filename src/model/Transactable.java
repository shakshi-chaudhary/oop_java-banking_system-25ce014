// Labs 5, 6 – Interfaces used by Account hierarchy
package model;

import exception.AccountLockedException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;

/**
 * Lab 6: Marks accounts that support deposits and withdrawals.
 */
public interface Transactable {
    void deposit(double amount) throws InvalidAmountException, AccountLockedException;
    void withdraw(double amount) throws InsufficientFundsException,
                                       InvalidAmountException,
                                       AccountLockedException;
}
