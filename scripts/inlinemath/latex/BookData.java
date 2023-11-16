package latex;

import latex.node.Block;

public class BookData {
	
	private Block booktitle;
	private Block bookauthor;
	private Block textbookid;
	private Block epubuid;
	
	public BookData(Block booktitle, Block bookauthor, Block textbookid,
			Block epubuid) {
		super();
		this.booktitle = booktitle;
		this.bookauthor = bookauthor;
		this.textbookid = textbookid;
		this.epubuid = epubuid;
	}

	public Block getBooktitle() {
		return booktitle;
	}

	public Block getBookauthor() {
		return bookauthor;
	}

	public Block getTextbookid() {
		return textbookid;
	}

	public Block getEpubuid() {
		return epubuid;
	}
	
}
