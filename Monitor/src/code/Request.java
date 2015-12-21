package code;

import java.io.File;
import java.nio.file.Path;

// Class to encapsulate data about a request
public class Request {
	public enum Type {
		READ, WRITE, TEST;
	}

	private Type type;
	private String filePath;
	private String mode;

	public Request(Type t, String f, String m) {
		type = t;
		filePath = f;
		mode = m;
	}

	public Type getType() {
		return type;
	}

	public String getFilePath() {
		return filePath;
	}
	
	public String getFileName() {
		File file = new File(filePath);
		Path path = file.toPath();
		return path.getFileName().toString();
	}

	public String getMode() {
		return mode;
	}
}
