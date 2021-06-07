package com.id.gastromanager.controller;

import java.sql.SQLException;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.model.Customer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class AccountSettingsController extends Controller {
	private Customer customer;

	@FXML
	private TextField addressTextField;
	@FXML
	private TextField cityTextField;
	@FXML
	private Button changeAddressButton;

	@Override
	public void init(Object... args) {
		customer = (Customer) args[0];
		addressTextField.setText(customer.getAddress());
		cityTextField.setText(customer.getCity());
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

		if (!address.matches("[A-ZĄĆĘŁŃÓŚŹŻ][A-Za-z0-9ĄĆĘŁŃÓŚŹŻąćęłńóśźż ]+")) {
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

		AlertFactory.showInformationAlert("Zmiana adresu i miasta powiodła się.");
	}
}
