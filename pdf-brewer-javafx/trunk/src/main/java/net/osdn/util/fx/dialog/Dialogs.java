package net.osdn.util.fx.dialog;

import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

public class Dialogs {
	
	public static ButtonType showInformation(String message) {
		return show(AlertType.INFORMATION, null, null, null, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showInformation(String title, String message) {
		return show(AlertType.INFORMATION, null, null, title, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showInformation(Window owner, String message) {
		return show(AlertType.INFORMATION, owner, null, null, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showInformation(Window owner, String title, String message) {
		return show(AlertType.INFORMATION, owner, null, title, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showConfirmation(String message) {
		return show(AlertType.CONFIRMATION, null, null, null, message, new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
	}
	
	public static ButtonType showConfirmation(String title, String message) {
		return show(AlertType.CONFIRMATION, null, null, title, message, new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
	}
	
	public static ButtonType showConfirmation(Window owner, String message) {
		return show(AlertType.CONFIRMATION, owner, null, null, message, new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
	}
	
	public static ButtonType showConfirmation(Window owner, String title, String message) {
		return show(AlertType.CONFIRMATION, owner, null, title, message, new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
	}
	
	public static ButtonType showWarning(String message) {
		return show(AlertType.WARNING, null, null, null, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showWarning(String title, String message) {
		return show(AlertType.WARNING, null, null, title, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showWarning(Window owner, String message) {
		return show(AlertType.WARNING, owner, null, null, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showWarning(Window owner, String title, String message) {
		return show(AlertType.WARNING, owner, null, title, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showError(String message) {
		return show(AlertType.ERROR, null, null, null, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showError(String title, String message) {
		return show(AlertType.ERROR, null, null, title, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showError(Window owner, String message) {
		return show(AlertType.ERROR, owner, null, null, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType showError(Window owner, String title, String message) {
		return show(AlertType.ERROR, owner, null, title, message, new ButtonType[] { ButtonType.OK });
	}
	
	public static ButtonType show(AlertType type, final Window owner, Node icon, String title, String message, ButtonType... buttons) {
		final Alert dialog = new Alert(type, message, buttons);
		
		if(owner != null && owner.getScene() != null) {
			dialog.initOwner(owner);
		}
		
		if(owner instanceof Stage) {
			ObservableList<Image> icons = ((Stage)owner).getIcons();
			if(icons != null && icons.size() > 0) {
				Stage stage = (Stage)dialog.getDialogPane().getScene().getWindow();
				stage.getIcons().add(icons.get(0));
			}
			if(title == null) {
				title = ((Stage)owner).getTitle();
			}
		}
		
		dialog.setTitle(title);
		dialog.setHeaderText(null);
		if(icon != null) {
			dialog.setGraphic(icon);
		}
		
		if(owner != null) {
			dialog.getDialogPane().layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {
				@Override
				public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
					if(dialog.getWidth() > 0 && dialog.getHeight() > 0) {
						double x = owner.getX() + owner.getWidth() / 2;
						double y = owner.getY() + owner.getHeight() / 2;
						dialog.setX(x - dialog.getWidth() / 2);
						dialog.setY(y - dialog.getHeight() / 2);
						dialog.getDialogPane().layoutBoundsProperty().removeListener(this);
					}
				}
			});
		}
		
		Optional<ButtonType> result = dialog.showAndWait();
		return result.isPresent() ? result.get() : null;
	}
}
