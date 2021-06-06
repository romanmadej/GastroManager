package com.id.gastromanager.controller;

import java.io.IOException;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.Navigator;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class DatabaseLoginController {
	@FXML
	private TextField hostTextField;
	@FXML
	private TextField portTextField;
	@FXML
	private TextField databaseTextField;
	@FXML
	private TextField usernameTextField;
	@FXML
	private PasswordField passwordTextField;
	@FXML
	private Button databaseLoginButton;

	@FXML
	private void databaseLoginButtonOnMouseClicked(MouseEvent mouseEvent) throws IOException {
		if (mouseEvent.getSource() != databaseLoginButton) {
			throw new UnsupportedOperationException("Incorrect button assigned");
		}
		if (mouseEvent.getButton() != MouseButton.PRIMARY) {
			return;
		}

		String host = hostTextField.getText();
		String port = portTextField.getText();
		String database = databaseTextField.getText();
		String username = usernameTextField.getText();
		String password = passwordTextField.getText();

		try {
			Database.initConnection(host, port, database, username, password);
		} catch (Exception e) {
			AlertFactory.showErrorAlert("Nie udało się połączyć.");
			return;
		}
		Stage stage = (Stage) databaseLoginButton.getScene().getWindow();
		Navigator.of(stage).setNamed("/AuthorizationView.fxml");
	}
}
