package com.id.gastromanager.controller;

import java.io.IOException;
import java.sql.SQLException;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.Navigator;
import com.id.gastromanager.model.Customer;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AccountSettingsController extends Controller {
	private Customer customer;
	private boolean isDeleteAccountOptionVisible;

	@FXML
	private TextField addressTextField;
	@FXML
	private TextField cityTextField;
	@FXML
	private Button changeAddressButton;
	@FXML
	private VBox deleteAccountVBox;
	@FXML
	private Button deleteAccountButton;

	@Override
	public void init(Object... args) {
		customer = (Customer) args[0];
		isDeleteAccountOptionVisible = (boolean) args[1];

		addressTextField.setText(customer.getAddress());
		cityTextField.setText(customer.getCity());
		deleteAccountVBox.setVisible(isDeleteAccountOptionVisible);
		deleteAccountVBox.setManaged(isDeleteAccountOptionVisible);
	}

	@FXML
	private void changeAddressButtonOnClicked(MouseEvent mouseEvent) {
		if (mouseEvent.getSource() != changeAddressButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}

		String address = addressTextField.getText().trim();
		String city = cityTextField.getText().trim();

		if (!address.matches("[A-Za-z0-9ĄĆĘŁŃÓŚŹŻąćęłńóśźż ,.\"'-]+")) {
			AlertFactory.showErrorAlert("Wpisany adres ma niepoprawny format.");
			return;
		}

		if (!city.matches("[A-ZĄĆĘŁŃÓŚŹŻ][A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż ]+")) {
			AlertFactory.showErrorAlert("Wpisane miasto ma niepoprawny format.");
			return;
		}

		try {
			Database.changeAddressAndCity(customer, address, city);
		} catch (SQLException e) {
			AlertFactory.showErrorAlert();
			return;
		}

		AlertFactory.showInformationAlert("Zmiana adresu dostawy powiodła się.");
		if (!isDeleteAccountOptionVisible) {
			Stage stage = (Stage) changeAddressButton.getScene().getWindow();
			Navigator.of(stage).pop();
		}
	}

	public void deleteAccountButtonOnClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != deleteAccountButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}

		Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz usunąć konto?",
				ButtonType.YES, ButtonType.NO);
		((Button) confirmationAlert.getDialogPane().lookupButton(ButtonType.YES)).setText("Tak");
		((Button) confirmationAlert.getDialogPane().lookupButton(ButtonType.NO)).setText("Nie");

		confirmationAlert.showAndWait();
		if (confirmationAlert.getResult() != ButtonType.YES) {
			return;
		}

		try {
			if (Database.deleteCustomer(customer.getCustomerId()) != 1) {
				throw new Exception();
			}
		} catch (Exception e) {
			AlertFactory.showErrorAlert();
			return;
		}

		AlertFactory.showInformationAlert("Pomyślnie usunięto konto.");
		Stage stage = (Stage) deleteAccountButton.getScene().getWindow();
		Navigator.of(stage).setNamed("/AuthorizationView.fxml");
	}
}
