package com.id.gastromanager;

import java.io.IOException;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		Navigator.createStageNamed("/DatabaseLoginView.fxml").show();
	}

	public static void main(String[] args) {
		launch();
	}
}
