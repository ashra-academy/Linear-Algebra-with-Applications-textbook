package latex;

import java.io.File;

public class LatexParserException extends Exception {
	
	private File inputFile;
	private int line;
	private int col;

	public LatexParserException(String message, Throwable cause, File inputFile, int line, int col) {
		super(message, cause);
		this.inputFile = inputFile;
		this.line = line;
		this.col = col;
	}

	public LatexParserException(String message, File inputFile, int line, int col) {
		super(message);
		this.inputFile = inputFile;
		this.line = line;
		this.col = col;
	}
	
	public File getFile() {
		return inputFile;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getCol() {
		return col;
	}

}
