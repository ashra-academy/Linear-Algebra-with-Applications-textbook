package latex.node;

import java.io.File;

public abstract class Node {
	
	private File inputFile;
	private int lineNum;
	private int colNum;
	
	protected Node(File inputFile, int lineNum, int colNum) {
		this.inputFile = inputFile;
		this.lineNum = lineNum;
		this.colNum = colNum;
	}
	
	public File getInputFile() {
		return inputFile;
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	public int getColNum() {
		return colNum;
	}
	
	public String getFileAndPosition() {
		return ("File: " + inputFile.toString() + " at " + lineNum + ", " + colNum);
	}
	
	public abstract String toLatexString();
	
}
