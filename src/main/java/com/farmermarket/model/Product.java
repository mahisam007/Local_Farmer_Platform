package com.farmermarket.model;

import javafx.beans.property.*;

/*
  Represents a product listed by a Farmer.
 */
public class Product {

    private final IntegerProperty id           = new SimpleIntegerProperty();
    private final IntegerProperty farmerId     = new SimpleIntegerProperty();
    private final StringProperty  name         = new SimpleStringProperty();
    private final StringProperty  category     = new SimpleStringProperty();
    private final IntegerProperty quantity     = new SimpleIntegerProperty();
    private final DoubleProperty  pricePerUnit = new SimpleDoubleProperty();
    /** Computed by FairPriceCalculator — not stored in DB. */
    private final DoubleProperty  fairPrice    = new SimpleDoubleProperty();
    // Constructors

    public Product() {}

    public Product(int id, int farmerId, String name, String category,
                   int quantity, double pricePerUnit) {
        setId(id);
        setFarmerId(farmerId);
        setName(name);
        setCategory(category);
        setQuantity(quantity);
        setPricePerUnit(pricePerUnit);
    }


    // id

    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    // farmerId


    public int getFarmerId() { return farmerId.get(); }
    public void setFarmerId(int value) { farmerId.set(value); }
    public IntegerProperty farmerIdProperty() { return farmerId; }

    // name


    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }


    // category


    public String getCategory() { return category.get(); }
    public void setCategory(String value) { category.set(value); }
    public StringProperty categoryProperty() { return category; }


    // quantity


    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int value) { quantity.set(value); }
    public IntegerProperty quantityProperty() { return quantity; }


    // pricePerUnit

    public double getPricePerUnit() { return pricePerUnit.get(); }
    public void setPricePerUnit(double value) { pricePerUnit.set(value); }
    public DoubleProperty pricePerUnitProperty() { return pricePerUnit; }


    // fairPrice (computed, not persisted)

    public double getFairPrice() { return fairPrice.get(); }
    public void setFairPrice(double value) { fairPrice.set(value); }
    public DoubleProperty fairPriceProperty() { return fairPrice; }


    // Object overrides


    @Override
    public String toString() {
        return "Product{id=" + getId() + ", name='" + getName()
                + "', category='" + getCategory() + "', qty=" + getQuantity()
                + ", price=" + getPricePerUnit() + "}";
    }
}
