package com.id.gastromanager.model;

public class IngredientsQuantity {
    String Ingredients;
    int quantity;
    public IngredientsQuantity(String name, int x){
        Ingredients = name;
        quantity = x;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getIngredients() {
        return Ingredients;
    }
}
