package com.id.gastromanager.model;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.id.gastromanager.Database;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class Cart {
	private final List<MenuPosition> menuPositions;
	private final Map<Integer, Integer> restaurantStock;
	private final Map<Integer, Map<Integer, Integer>> dishIngredientQuantityMap;
	private final SimpleDoubleProperty totalValue = new SimpleDoubleProperty(0);

	public SimpleDoubleProperty getTotalValueProperty() {
		return totalValue;
	}

	private final Map<Integer, SimpleBooleanProperty> canAddMap = new HashMap<>();

	public SimpleBooleanProperty getCanAddProperty(MenuPosition menuPosition) {
		return canAddMap.get(menuPosition.getDishId());
	}

	public Cart(Restaurant restaurant, List<MenuPosition> menuPositions) throws SQLException {
		this.menuPositions = menuPositions;
		restaurantStock = Database.getStock(restaurant);
		dishIngredientQuantityMap = Database.getDishIngredientQuantityMap();
		for (MenuPosition menuPosition : menuPositions) {
			canAddMap.put(menuPosition.getDishId(), new SimpleBooleanProperty(canAdd(menuPosition)));
		}
	}

	private boolean canAdd(MenuPosition menuPosition) {
		for (Map.Entry<Integer, Integer> entry : dishIngredientQuantityMap.get(menuPosition.getDishId()).entrySet()) {
			int ingredientId = entry.getKey();
			int quantity = entry.getValue();
			if (quantity > restaurantStock.get(ingredientId)) {
				return false;
			}
		}
		return true;
	}

	private void analyseStock() {
		for (MenuPosition menuPosition : menuPositions) {
			canAddMap.get(menuPosition.getDishId()).set(canAdd(menuPosition));
		}
	}

	public void addToCart(MenuPosition menuPosition) {
		for (Map.Entry<Integer, Integer> entry : dishIngredientQuantityMap.get(menuPosition.getDishId()).entrySet()) {
			int ingredientId = entry.getKey();
			int quantity = entry.getValue();
			restaurantStock.put(ingredientId, restaurantStock.get(ingredientId) - quantity);
		}
		menuPosition.numberInCartProperty.set(menuPosition.numberInCartProperty.get() + 1);
		analyseStock();
		totalValue.set(totalValue.get() + menuPosition.getPrice());
	}

	public void removeFromCart(MenuPosition menuPosition) {
		for (Map.Entry<Integer, Integer> entry : dishIngredientQuantityMap.get(menuPosition.getDishId()).entrySet()) {
			int ingredientId = entry.getKey();
			int quantity = entry.getValue();
			restaurantStock.put(ingredientId, restaurantStock.get(ingredientId) + quantity);
		}
		menuPosition.numberInCartProperty.set(menuPosition.numberInCartProperty.get() - 1);
		analyseStock();
		totalValue.set(totalValue.get() - menuPosition.getPrice());
	}
}
