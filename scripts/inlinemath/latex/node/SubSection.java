package latex.node;

import java.io.File;


public class SubSection extends SectionNode {
	
	public SubSection(int number, Block title, Block content, File inputFile, int lineNum, int colNum) {
		super(number, title, content, inputFile, lineNum, colNum);
	}
	
	public void setNumber(int number) {
		this.number = number;
	}
	
	@Override
	protected String getLatexCommandName() {
		return "subsection";
	}

}
