package net.osdn.pdf_brewer.ui;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import net.osdn.util.javafx.application.SingletonApplication;
import org.apache.pdfbox.pdmodel.PDDocument;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.osdn.util.LogFilter;
import net.osdn.util.fx.pdf.Pager;
import net.osdn.util.fx.pdf.PdfView;
import net.osdn.util.fx.toast.Toast;

public class Main extends SingletonApplication implements Initializable {

	static {
		System.setProperty("org.apache.commons.logging.LogFactory", "net.osdn.util.LogFilter");
		LogFilter.setLevel("org.apache.pdfbox", LogFilter.Level.ERROR);
		LogFilter.setLevel("org.apache.fontbox", LogFilter.Level.ERROR);
	}
	
	public static void main(String[] args) throws Throwable {
		launch(args);
	}
	
	private static Preferences preferences = Preferences.userNodeForPackage(Main.class);
	
	private Stage  primaryStage;
	private String title = "PDF BREWER";
	private File   inputFile;
	private File   lastSaveFolder;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		
		Image icon = new Image(getClass().getResourceAsStream("/img/app-icon-48px.png"));
		primaryStage.getIcons().add(icon);
		
		title = (title + " " + Util.getApplicationVersionShortString()).trim();
		primaryStage.setTitle(title);

		FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
		loader.setController(this);
		Parent root = (Parent)loader.load();
		
		Scene scene = new Scene(root);
		scene.setOnDragOver(wrap(this::window_onDragOver));
		scene.setOnDragDropped(wrap(this::window_onDragDropped));
		scene.getAccelerators().putAll(pager.createDefaultAccelerators());
		
		primaryStage.setScene(scene);
		primaryStage.setMinWidth(320);
		primaryStage.setMinHeight(320);
		primaryStage.show();

		Thread.currentThread().setUncaughtExceptionHandler(handler);
	}
	
	protected UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if(e instanceof Exception) {
				showException((Exception)e);
			} else if(Thread.getDefaultUncaughtExceptionHandler() != null) {
				Thread.getDefaultUncaughtExceptionHandler().uncaughtException(t, e);
			} else {
				e.printStackTrace();
			}
		}
	};

	protected void showException(Throwable exception) {
		exception.printStackTrace();
		
		Runnable r = ()-> {
			String title = exception.getClass().getName();
			String message = exception.getLocalizedMessage();
			toast.show(Toast.RED, title, message, null);
		};
		if(Platform.isFxApplicationThread()) {
			r.run();
		} else {
			Platform.runLater(r);
		}
	}
	
	@FXML private MenuItem menuFileSave;
	@FXML private Pager    pager;
	@FXML private PdfView  pdfView;
	@FXML private Toast    toast;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		primaryStage.setX(preferences.getDouble("stageX", 50.0));
		primaryStage.setY(preferences.getDouble("stageY", 50.0));
		primaryStage.setWidth(preferences.getDouble("stageWidth", 384));
		primaryStage.setHeight(preferences.getDouble("stageHeight", 600));
		primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
			Rectangle2D workarea = Screen.getPrimary().getVisualBounds();
			double x = preferences.getDouble("stageX", 50.0);
			if(x + primaryStage.getWidth() > workarea.getMaxX()) {
				x = workarea.getMaxX() - primaryStage.getWidth();
			}
			if(x < workarea.getMinX()) {
				x = workarea.getMinX();
			}
			primaryStage.setX(x);
			double y = preferences.getDouble("stageY", 50.0);
			if(y + primaryStage.getHeight() > workarea.getMaxY()) {
				y = workarea.getMaxY() - primaryStage.getHeight();
			}
			if(y < workarea.getMinY()) {
				y = workarea.getMinY();
			}
			primaryStage.setY(y);
		});
		primaryStage.xProperty().addListener((observable, oldValue, newValue)-> {
			preferences.putDouble("stageX", newValue.doubleValue());
		});
		primaryStage.yProperty().addListener((observable, oldValue, newValue)-> {
			preferences.putDouble("stageY", newValue.doubleValue());
		});
		primaryStage.widthProperty().addListener((observable, oldValue, newValue)-> {
			preferences.putDouble("stageWidth", newValue.doubleValue());
		});
		primaryStage.heightProperty().addListener((observable, oldValue, newValue)-> {
			preferences.putDouble("stageHeight", newValue.doubleValue());
		});
		
		pager.maxPageIndexProperty().bind(pdfView.maxPageIndexProperty());
		pager.pageIndexProperty().bindBidirectional(pdfView.pageIndexProperty());
		
		menuFileSave.disableProperty().bind(Bindings.isNull(pdfView.documentProperty()));
		toast.maxWidthProperty().bind(primaryStage.widthProperty().subtract(14.0));
	}

	protected void open(Path input) {
		primaryStage.setTitle(input + " - " + title);
		Task<PDDocument> task = pdfView.load(new DocumentLoader(input));
		task.setOnFailed(event-> {
			primaryStage.setTitle(title);
			showException(event.getSource().getException());
		});
	}
	
	protected void openFile() {
		FileChooser fc = new FileChooser();
		fc.setTitle("開く");
		fc.getExtensionFilters().add(new ExtensionFilter("YAML", "*.yml", "*.yaml"));
		fc.getExtensionFilters().add(new ExtensionFilter("PDF Brewer", "*.pb"));
		String s = preferences.get("lastOpenFolder", null);
		if(s != null) {
			File dir = new File(s);
			if(dir.exists() && dir.isDirectory()) {
				fc.setInitialDirectory(dir);
			}
		}
		File file = fc.showOpenDialog(primaryStage);
		if(file != null) {
			String ext = file.getName().toLowerCase();
			int i = ext.lastIndexOf('.');
			if(i >= 0) {
				ext = ext.substring(i);
			}
			if(ext.equals(".yml") || ext.equals(".yaml") || ext.equals(".pb")) {
				open(file.toPath());
			}
			preferences.put("lastOpenFolder", file.getParentFile().getAbsolutePath());
		}
		
		new Alert(AlertType.WARNING);
	}
	
	protected void saveFile() throws IOException {
		FileChooser fc = new FileChooser();
		fc.setTitle("名前を付けて保存");
		if(lastSaveFolder != null && lastSaveFolder.isDirectory()) {
			fc.setInitialDirectory(lastSaveFolder);
		} else {
			lastSaveFolder = null;
			if(inputFile != null) {
				fc.setInitialDirectory(inputFile.getParentFile());
			}
		}
		String defaultName = "output.pdf";
		if(inputFile != null) {
			defaultName = inputFile.getName();
			int i = defaultName.lastIndexOf('.');
			if(i > 0) {
				defaultName = defaultName.substring(0, i);
			}
			defaultName += ".pdf";
		}
		fc.setInitialFileName(defaultName);
		File file = fc.showSaveDialog(primaryStage);
		if(file != null) {
			lastSaveFolder = file.getParentFile();
			if(pdfView.getDocument() != null) {
				pdfView.getDocument().save(file);
				toast.show(Toast.GREEN, "保存しました", file.getAbsolutePath(), Toast.LONG);
			}
		}
		
		String s = preferences.get("lastSaveFolder", null);
		if(s != null) {
			File dir = new File(s);
			if(dir.exists() && dir.isDirectory()) {
				fc.setInitialDirectory(dir);
			}
		}
	}
	
	protected Path getDragAcceptable(DragEvent event) {
		if(event.getDragboard().hasFiles()) {
			List<File> files = event.getDragboard().getFiles();
			if(files.size() == 1) {
				File file = files.get(0);
				if(file.getName().matches("(?i).+(\\.yml|\\.yaml|\\.pb)")) {
					return file.toPath();
				}
			}
		}
		return null;
	}
	
	void window_onDragOver(DragEvent event) {
		if(getDragAcceptable(event) != null) {
			event.acceptTransferModes(TransferMode.COPY);
		} else {
			event.consume();
		}
	}
	
	void window_onDragDropped(DragEvent event) {
		Path path = getDragAcceptable(event);
		if(path != null) {
			primaryStage.toFront();
			open(path);
			event.setDropCompleted(true);
		}
		event.consume();
	}
	
	@FXML
	protected void menuFileOpen_onAction(ActionEvent event) {
		openFile();
	}
	
	@FXML
	protected void menuFileSave_onAction(ActionEvent event) throws IOException {
		saveFile();
	}

	@FXML
	protected void menuFileExit_onAction(ActionEvent event) {
		Platform.exit();
	}
}
