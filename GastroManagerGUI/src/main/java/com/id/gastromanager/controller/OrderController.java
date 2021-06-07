package com.id.gastromanager.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.model.Customer;
import com.id.gastromanager.model.MenuPosition;
import com.id.gastromanager.model.Restaurant;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public class OrderController extends Controller {
	private Customer customer;
	private Restaurant restaurant;
	private List<MenuPosition> menuPositions;

	@FXML
	private ListView<MenuPosition> menuPositionsListView;

	@Override
	public void init(Object... args) {
		this.customer = (Customer) args[0];
		this.restaurant = (Restaurant) args[1];

		try {
			menuPositions = Database.getMenuPostions(restaurant);
		} catch (SQLException e) {
			menuPositions = new ArrayList<>();
			e.printStackTrace();
		}

		if (menuPositions.isEmpty()) {
			AlertFactory.showErrorAlert();
			return;
		}

		menuPositionsListView.setItems(FXCollections.observableArrayList(menuPositions));
		menuPositionsListView.setCellFactory(listView -> new ListCell<>() {
			@Override
			protected void updateItem(MenuPosition item, boolean empty) {
				super.updateItem(item, empty);
				if (item == null) {
					return;
				}
				// todo edit cells
				this.setText(item.getCategoryName() + " " + item.getDishName() + " " + item.getDiet().name() + " "
						+ Arrays.toString(item.getAllergens()));
			}
		});
	}
}
