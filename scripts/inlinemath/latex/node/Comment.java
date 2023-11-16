package latex.node;

import java.io.File;


public class Comment extends Node {

	public String value;

	public Comment(String value, File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
		this.value = value;
	}
	
	@Override
	public String toLatexString() {
		return "%" + value + "\n";
	}
	
}
