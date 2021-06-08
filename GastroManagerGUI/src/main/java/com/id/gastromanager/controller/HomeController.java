package com.id.gastromanager.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.Navigator;
import com.id.gastromanager.model.Customer;
import com.id.gastromanager.model.Restaurant;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class HomeController extends Controller {
	private Customer customer;
	private Restaurant selectedRestaurant = null;

	@FXML
	private Label greetingsLabel;
	@FXML
	public ListView<Restaurant> restaurantsListView;
	@FXML
	private Button selectRestaurantButton;
	@FXML
	private Button settingsButton;
	@FXML
	private Button logoutButton;

	@Override
	public void init(Object... args) {
		customer = (Customer) args[0];
		greetingsLabel.setText(greetingsLabel.getText().formatted(customer.getName()));

		List<Restaurant> restaurants;
		Map<Integer, Boolean> isOpenMap = new HashMap<>();
		try {
			restaurants = Database.getRestaurants();
			for (Restaurant restaurant : restaurants) {
				isOpenMap.put(restaurant.getRestaurantId(), Database.isOpen(restaurant));
			}
		} catch (SQLException e) {
			AlertFactory.showErrorAlert();
			Stage stage = (Stage) restaurantsListView.getScene().getWindow();
			Navigator.of(stage).pop();
			return;
		}

		restaurantsListView.setPlaceholder(new Label("Aktualnie wszystkie restauracje są zamknięte."));

		FilteredList<Restaurant> filteredList = new FilteredList<>(FXCollections.observableArrayList(restaurants));
		filteredList.setPredicate(restaurant -> isOpenMap.get(restaurant.getRestaurantId()));
		restaurantsListView.setItems(filteredList);
		if (filteredList.isEmpty()) {
			selectRestaurantButton.setVisible(false);
		}

		restaurantsListView.setCellFactory(listView -> new ListCell<>() {
			@Override
			protected void updateItem(Restaurant item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					this.setText(null);
					this.setOnMouseClicked(null);
					return;
				}
				this.setText(item.getAddress() + ", " + item.getCity());
				this.setOnMouseClicked(event -> {
					if (event.getButton() != MouseButton.PRIMARY) {
						return;
					}
					selectedRestaurant = item;
				});
			}
		});

		if (!restaurants.isEmpty()) {
			restaurantsListView.getSelectionModel().select(0);
			selectedRestaurant = restaurants.get(0);
		}
	}

	@FXML
	private void selectRestaurantButtonOnClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != selectRestaurantButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}
		Stage stage = (Stage) selectRestaurantButton.getScene().getWindow();
		Navigator.of(stage).pushNamed("/OrderView.fxml", customer, selectedRestaurant);
	}

	@FXML
	private void settingsButtonOnClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != settingsButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}
		Stage stage = (Stage) settingsButton.getScene().getWindow();
		Navigator.of(stage).pushNamed("/AccountSettingsView.fxml", customer, true);
	}

	@FXML
	private void logoutButtonOnClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != logoutButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}
		Stage stage = (Stage) logoutButton.getScene().getWindow();
		Navigator.of(stage).setNamed("/AuthorizationView.fxml");
	}
}
