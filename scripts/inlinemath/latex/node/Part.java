package latex.node;

import java.io.File;

public class Part extends SectionNode {
	
	private boolean continuous;
	
	public Part(int number, boolean continuous, Block title, Block content, File inputFile, int lineNum, int colNum) {
		super(number, title, content, inputFile, lineNum, colNum);
		this.continuous = continuous;
	}
	
	@Override
	protected String getLatexCommandName() {
		if (continuous)
			return "continuouspart";
		else
			return "part";
	}

}