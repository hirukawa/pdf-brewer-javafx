package net.osdn.pdf_brewer.ui;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import net.osdn.util.javafx.application.SingletonApplication;
import net.osdn.util.javafx.fxml.Fxml;
import net.osdn.util.javafx.scene.control.pdf.Pager;
import net.osdn.util.javafx.scene.control.pdf.PdfView;
import net.osdn.util.javafx.stage.StageUtil;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.osdn.util.fx.toast.Toast;

public class MainApp extends SingletonApplication implements Initializable {

	public static final String APPLICATION_NAME = "PDF BREWER";
	public static final String APPLICATION_VERSION;

	static {
		System.setProperty("org.apache.commons.logging.LogFactory", "net.osdn.pdf_brewer.ui.LogFilter");
		LogFilter.setLevel("org.apache.pdfbox", LogFilter.Level.ERROR);
		LogFilter.setLevel("org.apache.fontbox", LogFilter.Level.ERROR);

		int[] version = Datastore.getApplicationVersion();
		if(version != null) {
			if (version[2] == 0) {
				APPLICATION_VERSION = String.format("%d.%d", version[0], version[1]);
			} else {
				APPLICATION_VERSION = String.format("%d.%d.%d", version[0], version[1], version[2]);
			}
		} else {
			APPLICATION_VERSION = "";
		}
	}
	
	private String title = "PDF BREWER";
	private File   inputFile;
	private File   lastSaveFolder;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/app-icon-48px.png")));
		primaryStage.titleProperty().bind(new StringBinding() {
			{
				bind(inputFileProperty);
			}
			@Override
			protected String computeValue() {
				return (inputFileProperty.get() != null ? inputFileProperty.get().getAbsolutePath() + " - " : "")
						+ APPLICATION_NAME + " " + APPLICATION_VERSION;
			}
		});

		primaryStage.showingProperty().addListener((observable, oldValue, newValue) -> {
			if(oldValue == true && newValue == false) {
				Platform.exit();
			}
		});

		Parent root = Fxml.load(this);
		
		Scene scene = new Scene(root);
		scene.setOnDragOver(wrap(this::scene_onDragOver));
		scene.setOnDragDropped(wrap(this::scene_onDragDropped));
		scene.getAccelerators().putAll(pager.createDefaultAccelerators());

		StageUtil.setRestorable(primaryStage, Preferences.userNodeForPackage(getClass()));
		primaryStage.setMinWidth(320);
		primaryStage.setMinHeight(320);
		primaryStage.setScene(scene);
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
			if(message != null) {
				message = message.trim();
			}
			toast.show(Toast.RED, title, message, null);
		};
		if(Platform.isFxApplicationThread()) {
			r.run();
		} else {
			Platform.runLater(r);
		}
	}

	@FXML MenuItem menuFileOpen;
	@FXML MenuItem menuFileSave;
	@FXML MenuItem menuFileExit;
	@FXML Pager    pager;
	@FXML PdfView  pdfView;
	@FXML Toast    toast;
	ObjectProperty<File> inputFileProperty = new SimpleObjectProperty<File>();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		//
		// event handlers
		//
		menuFileOpen.setOnAction(wrap(this::menuFileOpen_onAction));
		menuFileSave.setOnAction(wrap(this::menuFileSave_onAction));
		menuFileExit.setOnAction(wrap(this::menuFileExit_onAction));

		//
		// bindings
		//
		menuFileSave.disableProperty().bind(Bindings.isNull(pdfView.documentProperty()));

		pager.maxPageIndexProperty().bind(pdfView.maxPageIndexProperty());
		pager.pageIndexProperty().bindBidirectional(pdfView.pageIndexProperty());
		
		toast.maxWidthProperty().bind(getPrimaryStage().widthProperty().subtract(32));
		toast.maxHeightProperty().bind(getPrimaryStage().heightProperty().subtract(32));
	}

	protected File getFile(DragEvent event) {
		if(event.getDragboard().hasFiles()) {
			List<File> files = event.getDragboard().getFiles();
			if(files.size() == 1) {
				return files.get(0);
			}
		}
		return null;
	}

	protected boolean isAcceptable(File file) {
		return file != null && file.getName().matches("(?i).+(\\.pdf|\\.yml|\\.yaml|\\.pb)");
	}

	void scene_onDragOver(DragEvent event) {
		if(isAcceptable(getFile(event))) {
			event.acceptTransferModes(TransferMode.COPY);
		} else {
			event.consume();
		}
	}

	void scene_onDragDropped(DragEvent event) {
		File file = getFile(event);
		if(isAcceptable(file)) {
			getPrimaryStage().toFront();
			toast.hide();
			lastSaveFolder = null;
			inputFileProperty.set(file);
			pdfView.load(new DocumentLoader(file.toPath()));
			event.setDropCompleted(true);
		}
		event.consume();
	}

	void menuFileOpen_onAction(ActionEvent event) {
		toast.hide();
		Preferences preferences = Preferences.userNodeForPackage(getClass());

		FileChooser fc = new FileChooser();
		fc.setTitle("開く");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Brewer", "*.pb"));
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML", "*.yml", ".yaml"));

		String lastOpenDirectory = preferences.get("lastOpenDirectory", null);
		if(lastOpenDirectory != null) {
			File dir = new File(lastOpenDirectory);
			if(dir.isDirectory()) {
				fc.setInitialDirectory(dir);
			}
		}
		File file = fc.showOpenDialog(getPrimaryStage());
		if(file != null) {
			preferences.put("lastOpenDirectory", file.getParentFile().getAbsolutePath());
			if(isAcceptable(file)) {
				toast.hide();
				lastSaveFolder = null;
				inputFileProperty.set(file);
				pdfView.load(new DocumentLoader(file.toPath()));
			}
		}
	}

	void menuFileSave_onAction(ActionEvent event) throws IOException {
		toast.hide();
		String defaultName = inputFileProperty.get().getName();
		int i = defaultName.lastIndexOf('.');
		if(i > 0) {
			defaultName = defaultName.substring(0, i);
		}
		defaultName += ".pdf";

		FileChooser fc = new FileChooser();
		fc.setTitle("名前を付けて保存");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
		if(lastSaveFolder != null) {
			fc.setInitialDirectory(lastSaveFolder);
		} else {
			fc.setInitialDirectory(inputFileProperty.get().getParentFile());
		}
		fc.setInitialFileName(defaultName);

		File file = fc.showSaveDialog(getPrimaryStage());
		if(file != null) {
			lastSaveFolder = file.getParentFile();
			if(pdfView.getDocument() != null) {
				pdfView.getDocument().save(file);
				toast.show(Toast.GREEN, "保存しました", file.getAbsolutePath(), Toast.LONG);
			}
		}
	}

	void menuFileExit_onAction(ActionEvent event) {
		getPrimaryStage().close();
	}
}
