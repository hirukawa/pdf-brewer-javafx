package net.osdn.pdf_brewer.ui;

import java.awt.SplashScreen;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class Util {

	public static Path getApplicationPath(Class<?> cls) {
		try {
			ProtectionDomain pd = cls.getProtectionDomain();
			CodeSource cs = pd.getCodeSource();
			URL location = cs.getLocation();
			URI uri = location.toURI();
			Path path = Paths.get(uri);
			return path.getParent();
		} catch(URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static int[] getApplicationVersion() {
		String s = System.getProperty("java.application.version");
		if(s == null || s.trim().length() == 0) {
			return null;
		}
		
		s = s.trim() + ".0.0.0.0";
		String[] array = s.split("\\.", 5);
		int[] version = new int[4];
		for(int i = 0; i < 4; i++) {
			try {
				version[i] = Integer.parseInt(array[i]);
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if(version[0] == 0 && version[1] == 0 && version[2] == 0 && version[3] == 0) {
			return null;
		}
		return version;
	}
	
	public static String getApplicationVersionShortString() {
		int[] v = getApplicationVersion();
		if(v == null) {
			return "";
		} else if(v[2] == 0) {
			return String.format("%d.%d", v[0], v[1]);
		} else {
			return String.format("%d.%d.%d", v[0], v[1]);
		}
	}
}
