package com.id.gastromanager.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import javafx.beans.property.SimpleIntegerProperty;

public class MenuPosition extends Dish {
	private Diet diet;
	private String[] allergens;
	private double price;
	private String categoryName;

	public final SimpleIntegerProperty numberInCartProperty = new SimpleIntegerProperty(0);

	public MenuPosition(ResultSet resultSet) throws SQLException {
		super(resultSet);
		this.diet = Diet.valueOf(resultSet.getString("diet"));
		this.allergens = (String[]) resultSet.getArray("allergens").getArray();
		this.price = resultSet.getDouble("price");
		this.categoryName = resultSet.getString("category_name");
	}

	public void setDiet(Diet diet) {
		this.diet = diet;
	}

	public Diet getDiet() {
		return diet;
	}

	public void setAllergens(String[] allergens) {
		this.allergens = allergens;
	}

	public String[] getAllergens() {
		return allergens;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public double getPrice() {
		return price;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryName() {
		return categoryName;
	}

	@Override
	public String toString() {
		return super.toString() + "=MenuPosition{" + "diet=" + diet + ", allergens=" + Arrays.toString(allergens)
				+ ", categoryName='" + categoryName + '\'' + '}';
	}
}
