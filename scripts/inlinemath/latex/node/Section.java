package latex.node;

import java.io.File;


public class Section extends SectionNode {
	
	public Section(int number, Block title, Block content, File inputFile, int lineNum, int colNum) {
		super(number, title, content, inputFile, lineNum, colNum);
	}
	
	@Override
	protected String getLatexCommandName() {
		return "section";
	}
	
	public void setNumber(int number) {
		this.number = number;
	}

}
