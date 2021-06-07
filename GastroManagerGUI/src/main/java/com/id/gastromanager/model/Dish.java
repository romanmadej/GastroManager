package com.id.gastromanager.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Dish {
	private int dishId;
	private String dishName;
	private int categoryId;

	public Dish(int dishId, String dishName, int categoryId) {
		this.dishId = dishId;
		this.dishName = dishName;
		this.categoryId = categoryId;
	}

	public Dish(ResultSet resultSet) throws SQLException {
		this.dishId = resultSet.getInt("dish_id");
		this.dishName = resultSet.getString("dish_name");
		this.categoryId = resultSet.getInt("category_id");
	}

	public int getDishId() {
		return dishId;
	}

	public void setDishId(int dishId) {
		this.dishId = dishId;
	}

	public String getDishName() {
		return dishName;
	}

	public void setDishName(String dishName) {
		this.dishName = dishName;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	public String toString() {
		return "Dishes{" + "dishId=" + dishId + ", dishName='" + dishName + '\'' + ", categoryId=" + categoryId + '}';
	}
}
