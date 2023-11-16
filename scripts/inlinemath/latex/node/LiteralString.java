package latex.node;

import java.io.File;


public class LiteralString extends Node {

	public String value;

	public LiteralString(String value, File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
		this.value = value;
	}
	
	@Override
	public String toLatexString() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	public boolean isWhitespace() {
		return value.length() == 0 || value.matches("[ \t\n]+");
	}
	
}
