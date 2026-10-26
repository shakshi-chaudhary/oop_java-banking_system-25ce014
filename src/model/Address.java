// Lab 3 – Nested Address class (value object, supports clone via copy constructor)
package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a postal address.
 * Lab 3: demonstrates nested value objects and deep clone via copy constructor.
 */
public class Address implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String street;
    private String city;
    private String state;
    private String pincode;

    public Address(String street, String city, String state, String pincode) {
        this.street  = street;
        this.city    = city;
        this.state   = state;
        this.pincode = pincode;
    }

    /** Copy constructor for deep clone. */
    public Address(Address other) {
        this(other.street, other.city, other.state, other.pincode);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────
    public String getStreet()  { return street; }
    public String getCity()    { return city; }
    public String getState()   { return state; }
    public String getPincode() { return pincode; }

    public void setStreet(String street)   { this.street  = street; }
    public void setCity(String city)       { this.city    = city; }
    public void setState(String state)     { this.state   = state; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    // ── Object overrides ─────────────────────────────────────────────────────
    @Override
    public String toString() {
        return street + ", " + city + ", " + state + " – " + pincode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address a)) return false;
        return Objects.equals(street, a.street) &&
               Objects.equals(city,   a.city)   &&
               Objects.equals(state,  a.state)  &&
               Objects.equals(pincode,a.pincode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, pincode);
    }

    @Override
    public Address clone() {
        try {
            return (Address) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
