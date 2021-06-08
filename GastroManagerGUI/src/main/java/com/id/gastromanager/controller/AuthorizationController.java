package com.id.gastromanager.controller;

import java.io.IOException;
import java.sql.SQLException;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.Navigator;
import com.id.gastromanager.model.Customer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class AuthorizationController extends Controller {
	@FXML
	private TextField loginEmailTextField;
	@FXML
	private PasswordField loginPasswordTextField;
	@FXML
	private Button loginButton;

	private static final String emailRegex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

	@FXML
	private void loginButtonOnMouseClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != loginButton) {
			throw new UnsupportedOperationException("Wrong button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}

		String email = loginEmailTextField.getText().trim();
		String password = loginPasswordTextField.getText().trim();

		if (!email.matches(emailRegex)) {
			AlertFactory.showErrorAlert("Wpisany email ma niepoprawny format.");
			return;
		}

		Customer customer;
		try {
			if (!Database.isPasswordCorrect(email, password)) {
				AlertFactory.showErrorAlert("Niepoprawny email lub hasło.");
				return;
			}
			customer = Database.getCustomer(email);
			assert customer != null;
			if (customer.isSystemUser()) {
				Navigator.createStageNamed("/AdminView.fxml").show();
			}
		} catch (SQLException e) {
			AlertFactory.showErrorAlert();
			return;
		}

		Stage stage = (Stage) loginButton.getScene().getWindow();
		Navigator.of(stage).setNamed("/HomeView.fxml", customer);
	}

	@FXML
	private TextField registerEmailTextField;
	@FXML
	private TextField registerNameTextField;
	@FXML
	private TextField registerSurnameTextField;
	@FXML
	private TextField registerAddressTextField;
	@FXML
	private TextField registerCityTextField;
	@FXML
	private TextField registerPhoneTextField;
	@FXML
	private PasswordField registerPasswordTextField;
	@FXML
	private PasswordField registerRepeatPasswordTextField;
	@FXML
	private Button registerButton;

	@FXML
	private void registerButtonOnMouseClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != registerButton) {
			throw new UnsupportedOperationException("Wrong button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}

		String email = registerEmailTextField.getText().trim();
		String name = registerNameTextField.getText().trim();
		String surname = registerSurnameTextField.getText().trim();
		String address = registerAddressTextField.getText().trim();
		String city = registerCityTextField.getText().trim();
		String phone = registerPhoneTextField.getText().trim();
		String password = registerPasswordTextField.getText().trim();
		String repeatedPassword = registerRepeatPasswordTextField.getText().trim();

		if (!email.matches(emailRegex)) {
			AlertFactory.showErrorAlert("Wpisany email ma niepoprawny format.");
			return;
		}

		if (!name.matches("[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+") || !surname.matches("[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+")) {
			AlertFactory.showErrorAlert("Wpisane imie lub nazwisko ma niepoprawny format.");
			return;
		}

		if (!address.matches("[A-Za-z0-9ĄĆĘŁŃÓŚŹŻąćęłńóśźż ,.\"'-]+")) {
			AlertFactory.showErrorAlert("Wpisany adres ma niepoprawny format.");
			return;
		}

		if (!city.matches("[A-ZĄĆĘŁŃÓŚŹŻ][A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż ]+")) {
			AlertFactory.showErrorAlert("Wpisane miasto ma niepoprawny format.");
			return;
		}

		if (!phone.matches("[0-9 +]+")) {
			AlertFactory.showErrorAlert("Wpisany telefon ma niepoprawny format.");
			return;
		}

		if (!password.equals(repeatedPassword)) {
			AlertFactory.showErrorAlert("Wpisane hasła różnią się.");
			return;
		}

		if (password.length() < 8) {
			AlertFactory.showErrorAlert("Wpisane hasło jest zbyt krótkie.");
			return;
		}

		if (!password.matches("[A-Za-z0-9!@#$%^&*()]+")) {
			AlertFactory.showErrorAlert(
					"Wpisane hasło ma niepoprawny format.\nMożesz użyć małych i wielkich liter, cyfr oraz znaków \"!@#$%^&*()\".");
			return;
		}

		try {
			if (Database.getCustomer(email) != null) {
				AlertFactory.showErrorAlert("Podany email jest już użyty.");
				return;
			}
		} catch (SQLException e) {
			AlertFactory.showErrorAlert();
			return;
		}

		Customer customer;
		try {
			Database.insertCustomer(email, name, surname, address, city, phone, password);
			customer = Database.getCustomer(email);
		} catch (SQLException e) {
			AlertFactory.showErrorAlert();
			return;
		}

		Stage stage = (Stage) registerButton.getScene().getWindow();
		Navigator.of(stage).setNamed("/HomeView.fxml", customer);
	}
}
