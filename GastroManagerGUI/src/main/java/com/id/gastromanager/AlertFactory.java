package com.id.gastromanager;

import javafx.scene.control.Alert;

public class AlertFactory {
	public static void showErrorAlert() {
		showErrorAlert("Nieoczekiwany błąd, spróbuj ponownie później.");
	}

	public static void showErrorAlert(String textContent) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setContentText(textContent);
		alert.show();
	}

	public static void showInformationAlert(String textContent) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setContentText(textContent);
		alert.show();
	}
}
