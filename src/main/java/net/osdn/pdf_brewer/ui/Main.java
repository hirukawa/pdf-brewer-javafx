package net.osdn.pdf_brewer.ui;

import javafx.application.Platform;
import net.osdn.util.javafx.application.SingletonApplication;

public class Main {

	public static void main(String[] args) {
		Platform.setImplicitExit(false);
		SingletonApplication.launch(MainApp.class, args);
	}
}
