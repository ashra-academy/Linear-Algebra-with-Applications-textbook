package latex.node;

import java.io.File;
import java.util.ArrayList;


public class Block extends Node {
	
	public ArrayList<Node> values = new ArrayList<Node>();

	public Block(File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
	}

	@Override
	public String toLatexString() {
		return toLatexString(true);
	}
	
	public String toLatexString(boolean printBraces) {
		StringBuffer sb = new StringBuffer();
		if (printBraces)
			sb.append("{");
		for (Node v : values) {
			sb.append(v.toLatexString());
		}
		if (printBraces)
			sb.append("}");
		return sb.toString();
	}

}
