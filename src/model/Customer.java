// Labs 2, 3 – Customer class
// Encapsulation (private fields, getters/setters), static ID counter,
// toString, equals/hashCode, nested Address, Cloneable
package model;

import java.io.Serializable;
import java.util.Objects;
import model.annotation.Id;
import model.annotation.MaxLength;

/**
 * Represents a bank customer.
 * Lab 2: encapsulation, static ID counter.
 * Lab 3: toString, equals/hashCode, Address, clone.
 */
public class Customer implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private static int idCounter = 1000;

    @Id
    private final int customerId;

    @MaxLength(50)
    private String name;
    private String email;
    private String phone;
    private Address address;

    public Customer(String name, String email, String phone, Address address) {
        this.customerId = ++idCounter;
        this.name       = name;
        this.email      = email;
        this.phone      = phone;
        this.address    = address;
    }

    /** Returns the next customer ID without incrementing (for display). */
    public static int peekNextId() { return idCounter + 1; }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public int     getCustomerId() { return customerId; }
    public String  getName()       { return name; }
    public String  getEmail()      { return email; }
    public String  getPhone()      { return phone; }
    public Address getAddress()    { return address; }

    public void setName(String name)     { this.name    = name; }
    public void setEmail(String email)   { this.email   = email; }
    public void setPhone(String phone)   { this.phone   = phone; }
    public void setAddress(Address addr) { this.address = addr; }

    // ── Object overrides ─────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format("Customer[id=%d, name=%s, email=%s, phone=%s, address=%s]",
                customerId, name, email, phone, address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer c)) return false;
        return customerId == c.customerId;
    }

    @Override
    public int hashCode() { return Objects.hash(customerId); }

    /** Deep clone – copies the nested Address. */
    @Override
    public Customer clone() {
        try {
            Customer copy = (Customer) super.clone();
            if (address != null) {
                copy.address = address.clone();
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
