package com.id.gastromanager.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class MenuPosition extends Dish {
	private Diet diet;
	private String[] allergens;
	private String categoryName;

	public MenuPosition(ResultSet resultSet) throws SQLException {
		super(resultSet);
		this.diet = Diet.valueOf(resultSet.getString("diet"));
		this.allergens = (String[]) resultSet.getArray("allergens").getArray();
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
