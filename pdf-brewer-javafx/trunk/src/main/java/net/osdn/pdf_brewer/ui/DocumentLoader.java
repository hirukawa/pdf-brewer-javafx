package net.osdn.pdf_brewer.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.esotericsoftware.yamlbeans.YamlReader;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.osdn.pdf_brewer.BrewerData;
import net.osdn.pdf_brewer.PdfBrewer;
import net.osdn.util.io.AutoDetectReader;

public class DocumentLoader implements Callable<PDDocument> {
	
	private Path input;
	
	public DocumentLoader(Path input) {
		this.input = input;
	}

	@Override
	public PDDocument call() throws Exception {
		PdfBrewer brewer = new PdfBrewer();
		BrewerData pb;
		
		if(isYaml(input)) {
			Data data = processYaml(input);
			brewer.setTitle(data.title);
			brewer.setAuthor(data.author);
			pb = new BrewerData(data.lines, brewer.getFontLoader());
		} else {
			pb = new BrewerData(input, brewer.getFontLoader());
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		brewer.process(pb);
		brewer.save(out);
		return PDDocument.load(out.toByteArray());
	}
	
	protected boolean isYaml(Path input) {
		return input.getFileName().toString().toLowerCase().endsWith(".yml");
	}
	
	protected Data processYaml(Path input) throws IOException, TemplateException {
		try(Reader reader = new AutoDetectReader(input)) {
			Data result = new Data();
			
			@SuppressWarnings("unchecked")
			Map<String, Object> yaml = (Map<String, Object>)new YamlReader(reader).read();
			
			Object obj;
			obj = yaml.get("title");
			if(obj instanceof String) {
				result.title = (String)obj;
			}
			obj = yaml.get("author");
			if(obj instanceof String) {
				result.author = (String)obj;
			}
			
			StringWriter out = new StringWriter();
			Template template = getTemplate((String)yaml.get("template"));
			template.process(yaml, out);
			result.lines = out.toString();
			
			return result;
		}
	}
	
	protected Template getTemplate(String name) throws IOException {
		Path path = Datastore.getApplicationDirectory();
		while(path != null) {
			Path t = path.resolve("templates");
			if(Files.isDirectory(t)) {
				path = t;
				break;
			}
			path = path.getParent();
		}
		if(path == null) {
			throw new IOException("templates folder not found.");
		}
		Configuration freemarker = new Configuration(Configuration.VERSION_2_3_26);
		freemarker.setDefaultEncoding("UTF-8");
		freemarker.setDirectoryForTemplateLoading(path.toFile());
		return freemarker.getTemplate(name);
	}
	
	private class Data {
		String title;
		String author;
		String lines;
	}
}
