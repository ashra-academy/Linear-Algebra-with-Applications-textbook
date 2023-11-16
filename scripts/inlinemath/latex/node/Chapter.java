package latex.node;

import java.io.File;


public class Chapter extends SectionNode {
	
	public Chapter(int number, Block title, Block content, File inputFile, int lineNum, int colNum) {
		super(number, title, content, inputFile, lineNum, colNum);
	}
	
	@Override
	protected String getLatexCommandName() {
		return "chapter";
	}

}
