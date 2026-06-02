package com.farmermarket.model;

import javafx.beans.property.*;

/*
 Represents a purchase order placed by a Buyer for a Product.
 */
public class Order {

    private final IntegerProperty id         = new SimpleIntegerProperty();
    private final IntegerProperty buyerId    = new SimpleIntegerProperty();
    private final IntegerProperty productId  = new SimpleIntegerProperty();
    private final IntegerProperty quantity   = new SimpleIntegerProperty();
    private final DoubleProperty  totalPrice = new SimpleDoubleProperty();
    private final ObjectProperty<OrderStatus> status = new SimpleObjectProperty<>();

    // Convenience display fields (populated by JOIN queries, not persisted separately)
    private final StringProperty productName = new SimpleStringProperty();
    private final StringProperty buyerName   = new SimpleStringProperty();

    // Constructors


    public Order() {}

    public Order(int id, int buyerId, int productId, int quantity,
                 double totalPrice, OrderStatus status) {
        setId(id);
        setBuyerId(buyerId);
        setProductId(productId);
        setQuantity(quantity);
        setTotalPrice(totalPrice);
        setStatus(status);
    }

    // id


    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    // buyerId


    public int getBuyerId() { return buyerId.get(); }
    public void setBuyerId(int value) { buyerId.set(value); }
    public IntegerProperty buyerIdProperty() { return buyerId; }

    // productId

    public int getProductId() { return productId.get(); }
    public void setProductId(int value) { productId.set(value); }
    public IntegerProperty productIdProperty() { return productId; }

    // quantity


    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int value) { quantity.set(value); }
    public IntegerProperty quantityProperty() { return quantity; }


    // totalPrice

    public double getTotalPrice() { return totalPrice.get(); }
    public void setTotalPrice(double value) { totalPrice.set(value); }
    public DoubleProperty totalPriceProperty() { return totalPrice; }


    // status


    public OrderStatus getStatus() { return status.get(); }
    public void setStatus(OrderStatus value) { status.set(value); }
    public ObjectProperty<OrderStatus> statusProperty() { return status; }


    // Display productName

    public String getProductName() { return productName.get(); }
    public void setProductName(String value) { productName.set(value); }
    public StringProperty productNameProperty() { return productName; }


    // Display buyerName


    public String getBuyerName() { return buyerName.get(); }
    public void setBuyerName(String value) { buyerName.set(value); }
    public StringProperty buyerNameProperty() { return buyerName; }

    // Object overrides


    @Override
    public String toString() {
        return "Order{id=" + getId() + ", buyerId=" + getBuyerId()
                + ", productId=" + getProductId() + ", qty=" + getQuantity()
                + ", total=" + getTotalPrice() + ", status=" + getStatus() + "}";
    }
}
