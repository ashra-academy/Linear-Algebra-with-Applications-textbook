package latex.node;

import java.io.File;

public abstract class SectionNode extends Node {
	
	protected int number;
	protected Block title;
	protected Block content;

	protected SectionNode(int number, Block title, Block content, File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
		this.number = number;
		this.title = title;
		this.content = content;
	}
	
	protected abstract String getLatexCommandName();
	
	public int getNumber() {
		return number;
	}

	public Block getTitle() {
		return title;
	}

	public Block getContent() {
		return content;
	}

	@Override
	public String toLatexString() {
		if (number < 1)
			return "\\" + getLatexCommandName() + "*" + title.toLatexString() + "\n" + content.toLatexString(false);
		else
			return "\\" + getLatexCommandName() + title.toLatexString() + "\n" + content.toLatexString(false);
	}

}
