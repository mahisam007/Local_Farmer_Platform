package com.farmermarket.model;

import javafx.beans.property.*;

/* Represents a platform user (Farmer or Buyer).
 */
public class User {

    private final IntegerProperty id           = new SimpleIntegerProperty();
    private final StringProperty  name         = new SimpleStringProperty();
    private final StringProperty  email        = new SimpleStringProperty();
    private final StringProperty  passwordHash = new SimpleStringProperty();
    private final ObjectProperty<Role> role    = new SimpleObjectProperty<>();


    // Constructors


    public User() {}

    public User(int id, String name, String email, String passwordHash, Role role) {
        setId(id);
        setName(name);
        setEmail(email);
        setPasswordHash(passwordHash);
        setRole(role);
    }


    // id


    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }


    // name


    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }


    // email


    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }


    // passwordHash


    public String getPasswordHash() { return passwordHash.get(); }
    public void setPasswordHash(String value) { passwordHash.set(value); }
    public StringProperty passwordHashProperty() { return passwordHash; }


    // role


    public Role getRole() { return role.get(); }
    public void setRole(Role value) { role.set(value); }
    public ObjectProperty<Role> roleProperty() { return role; }


    // Object overrides


    @Override
    public String toString() {
        return "User{id=" + getId() + ", name='" + getName() + "', role=" + getRole() + "}";
    }
}
