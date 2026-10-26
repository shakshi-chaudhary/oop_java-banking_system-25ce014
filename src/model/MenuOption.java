// Lab 1 – MenuOption enum
// An enum with a display label; used in the switch expression in MiniBank.
package model;

/**
 * All menu options for the MiniBank console application.
 * Lab 1: demonstrates enums with fields and constructors.
 */
public enum MenuOption {
    CREATE_CUSTOMER("Create Customer"),
    CREATE_ACCOUNT("Create Account"),
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw"),
    VIEW_BALANCE("View Balance"),
    VIEW_STATEMENT("View Statement"),
    TRANSFER("Transfer"),
    APPLY_INTEREST("Apply Interest"),
    LIST_ACCOUNTS("List All Accounts"),
    DELETE_ACCOUNT("Delete Account"),
    EXIT("Exit");

    private final String label;

    MenuOption(String label) { this.label = label; }

    public String getLabel() { return label; }

    /**
     * Resolve a MenuOption by its 1-based menu index.
     * @throws IllegalArgumentException if index is out of range
     */
    public static MenuOption fromIndex(int index) {
        MenuOption[] values = values();
        if (index < 1 || index > values.length)
            throw new IllegalArgumentException("Invalid choice: " + index);
        return values[index - 1];
    }
}
