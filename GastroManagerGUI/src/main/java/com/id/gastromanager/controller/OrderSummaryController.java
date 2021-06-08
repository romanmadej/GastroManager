package com.id.gastromanager.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.Navigator;
import com.id.gastromanager.model.Customer;
import com.id.gastromanager.model.MenuPosition;
import com.id.gastromanager.model.Restaurant;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class OrderSummaryController extends Controller {
	private Customer customer;
	private Restaurant restaurant;
	private List<MenuPosition> cartContents;

	@FXML
	private ListView<MenuPosition> orderSummaryListView;
	@FXML
	private ChoiceBox<String> deliveryChoiceBox;
	@FXML
	private Button settingsButton;
	@FXML
	private Label deliveryInformationLabel;
	@FXML
	private Label totalCartValueLabel;
	@FXML
	private Button submitOrderButton;

	private boolean similarEnough(String s1, String s2) {
		return StringUtils.stripAccents(s1).equalsIgnoreCase(StringUtils.stripAccents(s2));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(Object... args) {
		this.customer = (Customer) args[0];
		this.restaurant = (Restaurant) args[1];
		this.cartContents = (List<MenuPosition>) args[2];

		double priceTotal = 0;

		Map<Integer, Double> discountedPriceMap = new HashMap<>();
		try {
			for (MenuPosition menuPosition : cartContents) {
				double result = Database.getDiscountPrice(menuPosition, customer);
				discountedPriceMap.put(menuPosition.getDishId(), result);
				priceTotal += Double.min(menuPosition.getPrice(), result)
						* menuPosition.numberInCartProperty.getValue();
			}
		} catch (SQLException e) {
			AlertFactory.showErrorAlert();
			Stage stage = (Stage) orderSummaryListView.getScene().getWindow();
			Navigator.of(stage).pop();
			return;
		}

		totalCartValueLabel.setText("Łącznie %.2fzł".formatted(priceTotal));

		orderSummaryListView.setItems(FXCollections.observableArrayList(cartContents));
		orderSummaryListView.setCellFactory(listView -> new ListCell<>() {
			@Override
			protected void updateItem(MenuPosition item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					// todo clear
					return;
				}

				HBox hBox = new HBox();
				hBox.setSpacing(10);

				Label label1 = new Label("%d    %s    ".formatted(item.numberInCartProperty.get(), item.getDishName()));
				Label label2 = new Label("%.2fzł".formatted(item.numberInCartProperty.getValue() * item.getPrice()));
				hBox.getChildren().addAll(label1, label2);

				double discountedPrice = discountedPriceMap.get(item.getDishId());
				if (discountedPrice < item.getPrice()) {
					label2.getStylesheets().addAll(
							Objects.requireNonNull(getClass().getResource("/Strikethrough.css")).toExternalForm());
					label2.setTextFill(Color.DARKRED);
					Label label3 = new Label(
							"    %.2fzł".formatted(item.numberInCartProperty.getValue() * discountedPrice));
					hBox.getChildren().add(label3);
				}
				this.setGraphic(hBox);
			}
		});

		final String errorText = "Dla wybranej restauracji realizujemy dostawy jedynie w obrębie miasta %s."
				.formatted(restaurant.getCity());

		deliveryChoiceBox.getSelectionModel().select(0);
		if (!similarEnough(customer.getCity(), restaurant.getCity())) {
			deliveryChoiceBox.setDisable(true);
			deliveryInformationLabel.setVisible(true);
			deliveryInformationLabel.setText(errorText);
			settingsButton.setVisible(true);
		} else {
			deliveryChoiceBox.setDisable(false);
			deliveryInformationLabel.setVisible(false);
			settingsButton.setVisible(false);
		}

		deliveryChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.intValue() == 0) {
				deliveryInformationLabel.setVisible(false);
				settingsButton.setVisible(false);
			} else {
				deliveryInformationLabel.setVisible(true);
				deliveryInformationLabel
						.setText("Dostawa na adres: %s, %s.".formatted(customer.getAddress(), restaurant.getCity()));
				settingsButton.setVisible(true);
			}
		});

		ChangeListener<String> listener = (observable, oldValue, newValue) -> {
			if (!similarEnough(customer.getCity(), restaurant.getCity())) {
				deliveryChoiceBox.getSelectionModel().select(0);
				deliveryChoiceBox.setDisable(true);
				deliveryInformationLabel.setVisible(true);
				deliveryInformationLabel.setText(errorText);
				settingsButton.setVisible(true);
			} else {
				deliveryChoiceBox.setDisable(false);
				if (deliveryChoiceBox.getSelectionModel().getSelectedIndex() == 0) {
					deliveryInformationLabel.setVisible(false);
					settingsButton.setVisible(false);
				} else {
					deliveryInformationLabel.setVisible(true);
					deliveryInformationLabel.setText(
							"Dostawa na adres: %s, %s.".formatted(customer.getAddress(), restaurant.getCity()));
					settingsButton.setVisible(true);
				}
			}
		};
		customer.getCityProperty().addListener(listener);
		customer.getAddressProperty().addListener(listener);
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
		Navigator.of(stage).pushNamed("/AccountSettingsView.fxml", customer);
	}

	public void submitOrderButton(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != submitOrderButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}

		boolean isDelivery = deliveryChoiceBox.getSelectionModel().getSelectedIndex() == 1;

		try {
			Database.submitOrder(customer, restaurant, cartContents, isDelivery);
		} catch (SQLException e) {
			AlertFactory.showErrorAlert();
			return;
		}

		AlertFactory.showInformationAlert(
				"Pomyślnie złożono zamówienie. Na twojego maila %s przesłane zostaną szczegóły zamówienia."
						.formatted(customer.getEmail()));

		Stage stage = (Stage) submitOrderButton.getScene().getWindow();
		Navigator.of(stage).setNamed("/HomeView.fxml", customer);
	}
}
