package com.id.gastromanager.model;

public class Orders {
    private int orderId;
    private int customerId;
    private int restaurantsId;
    private String orderedDate;
    private String status;
    private boolean isDelivery;

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getRestaurantsId() {
        return restaurantsId;
    }

    public void setRestaurantsId(int restaurantsId) {
        this.restaurantsId = restaurantsId;
    }

    public String getOrderedDate() {
        return orderedDate;
    }

    public void setOrderedDate(String orderedDate) {
        this.orderedDate = orderedDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setDelivery(boolean delivery) {
        isDelivery = delivery;
    }

    public boolean Delivery() {
        return isDelivery;
    }

    @Override
    public String toString() {
        return "Orders{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", restaurantsId=" + restaurantsId +
                ", orderedDate='" + orderedDate + '\'' +
                ", status='" + status + '\'' +
                ", isDelivery=" + isDelivery +
                '}';
    }
}
