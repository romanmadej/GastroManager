package com.id.gastromanager.model;

public class Dishes {
    private int dishId;
    private String dishName;
    private int categoryId;
    private int quantity;

    public int getDishId(){ return dishId;}

    public void setDishId(int dishId){ this.dishId = dishId;}

    public String getDishName(){ return dishName;}

    public void setDishName(String dishName){ this.dishName = dishName;}

    public int getCategoryId(){ return categoryId;}

    public void setCategoryId(int categoryId){ this.categoryId = categoryId;}

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Dishes{" +
                "dishId=" + dishId +
                ", dishName='" + dishName + '\'' +
                ", categoryId=" + categoryId +
                ", quantity=" + quantity +
                '}';
    }
}
