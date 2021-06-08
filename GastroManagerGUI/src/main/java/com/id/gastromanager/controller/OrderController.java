package com.id.gastromanager.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.Navigator;
import com.id.gastromanager.model.*;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class OrderController extends Controller {
	private Customer customer;
	private Restaurant restaurant;
	private List<MenuPosition> menuPositions;
	private Cart cart;

	@FXML
	private TabPane categoriesTabPane;
	@FXML
	private ListView<MenuPosition> orderSummaryListView;
	@FXML
	private Label totalCartValueLabel;
	@FXML
	private Button goToSummaryButton;

	@Override
	public void init(Object... args) {
		this.customer = (Customer) args[0];
		this.restaurant = (Restaurant) args[1];

		try {
			menuPositions = Database.getMenuPostions(restaurant);
			cart = new Cart(restaurant, menuPositions);
		} catch (SQLException e) {
			menuPositions = new ArrayList<>();
			e.printStackTrace();
		}

		if (menuPositions.isEmpty()) {
			AlertFactory.showErrorAlert();
			Stage stage = (Stage) categoriesTabPane.getScene().getWindow();
			Navigator.of(stage).pop();
			return;
		}

		FilteredList<MenuPosition> summaryList = new FilteredList<>(FXCollections.observableArrayList(menuPositions));
		summaryList.setPredicate(x -> false);
		orderSummaryListView.setItems(summaryList);
		orderSummaryListView.setCellFactory(listView -> new ListCell<>() {
			@Override
			protected void updateItem(MenuPosition item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					this.textProperty().unbind();
					this.setText(null);
					return;
				}
				this.textProperty().bind(Bindings.format("%d    %s    %.2fzł", item.numberInCartProperty.get(),
						item.getDishName(), item.numberInCartProperty.getValue() * item.getPrice()));
			}
		});

		Map<String, List<MenuPosition>> menu = new HashMap<>();
		for (MenuPosition menuPosition : menuPositions) {
			menu.computeIfAbsent(menuPosition.getCategoryName(), categoryName -> new ArrayList<>()).add(menuPosition);
		}

		for (Map.Entry<String, List<MenuPosition>> entry : menu.entrySet()) {
			ListView<MenuPosition> categoryListView = new ListView<>(
					FXCollections.observableArrayList(entry.getValue()));
			categoryListView.setCellFactory(listView -> new ListCell<>() {
				@Override
				protected void updateItem(MenuPosition item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						this.setGraphic(null);
						return;
					}

					VBox vbox = new VBox();
					vbox.setAlignment(Pos.BASELINE_LEFT);
					vbox.setSpacing(5);
					HBox hbox = new HBox();
					hbox.setSpacing(8);
					hbox.getChildren().add(new Label(item.getDishName()));

					if (item.getAllergens().length != 0) {
						Label allergenInfoLabel = new Label();
						if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
							allergenInfoLabel.setText("ⓘ");
						} else {
							allergenInfoLabel.setText("ℹ");
						}
						allergenInfoLabel
								.setTooltip(new Tooltip("Alergeny: " + String.join(", ", item.getAllergens())));
						allergenInfoLabel.getTooltip();
						hbox.getChildren().add(allergenInfoLabel);
					}
					if (item.getDiet() != Diet.standard) {
						String diet = item.getDiet() == Diet.vegan ? "wegańskie" : "wegetariańskie";
						Label dietLabel = new Label(diet);
						dietLabel.setTextFill(Color.FORESTGREEN);
						hbox.getChildren().add(dietLabel);
					}
					vbox.getChildren().add(hbox);
					vbox.getChildren().add(new Label("%.2fzł".formatted(item.getPrice())));

					Button plusButton = new Button("+");
					Button minusButton = new Button("-");
					minusButton.disableProperty().bind(item.numberInCartProperty.lessThan(1));
					plusButton.disableProperty()
							.bind(item.numberInCartProperty.greaterThan(99).or(cart.getCanAddProperty(item).not()));
					minusButton.setOnMouseClicked(e -> cart.removeFromCart(item));
					plusButton.setOnMouseClicked(e -> cart.addToCart(item));
					item.numberInCartProperty.addListener((observable, oldValue, newValue) -> {
						summaryList.setPredicate(null);
						summaryList.setPredicate(x -> x.numberInCartProperty.greaterThan(0).getValue());
					});

					Label counterLabel = new Label();
					counterLabel.textProperty().bind(item.numberInCartProperty.asString());
					HBox buttonHBox = new HBox();
					buttonHBox.setSpacing(10);
					VBox counterLabelCenterer = new VBox(counterLabel);
					counterLabelCenterer.setAlignment(Pos.CENTER);

					buttonHBox.getChildren().addAll(plusButton, minusButton, counterLabelCenterer);
					vbox.getChildren().add(buttonHBox);
					this.setGraphic(vbox);
				}
			});
			categoriesTabPane.getTabs().add(new Tab(entry.getKey(), categoryListView));
			totalCartValueLabel.textProperty().bind(Bindings.format("Łącznie %.2fzł", cart.getTotalValueProperty()));
			goToSummaryButton.disableProperty().bind(Bindings.size(summaryList).isEqualTo(0));
		}
	}

	public void goToSummaryButtonOnClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != goToSummaryButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}
		Stage stage = (Stage) goToSummaryButton.getScene().getWindow();
		Navigator.of(stage).pushNamed("/OrderSummaryView.fxml", customer, restaurant, orderSummaryListView.getItems());
	}
}
