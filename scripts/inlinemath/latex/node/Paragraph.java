package latex.node;

import java.io.File;

public class Paragraph extends Block {

	public Paragraph(File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
	}
	
	@Override
	public String toLatexString() {
		return super.toLatexString(false) + "\n\n";
	}

}
