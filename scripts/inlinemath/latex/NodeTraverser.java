package latex;

import java.util.ListIterator;
import java.util.Stack;

import latex.node.*;

public abstract class NodeTraverser {
	
	public enum Action {
		TraverseChildren,
		SkipChildren,
		Stop
	}
	
	protected Stack<Node> currentContext;
	protected Stack<Block> currentBlock;
	protected Stack<Integer> prevBlockPositions;
	protected int currentBlockPosition;
	
	public NodeTraverser() {
		currentContext = new Stack<Node>();
		currentBlock = new Stack<Block>();
		prevBlockPositions = new Stack<Integer>();
	}
	
	// Traverse methods for sub-classes
	
	protected Action traverseNode(Node nd) {
		
		Action act;
		
		if (nd instanceof Paragraph) { // Must come before Block
			
			Paragraph para = (Paragraph)nd;
			
			act = beforeVisitParagraph(para);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren)
				traverseBlock((Block)nd, nd);
			
			act = afterVisitParagraph(para);
			if (act == Action.Stop)
				return act;
		}
		else if (nd instanceof Block) {
			
			Block blk = (Block)nd;
			
			act = beforeVisitBlock(blk);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren)
				traverseBlock((Block)nd, nd);
			
			act = afterVisitBlock(blk);
			if (act == Action.Stop)
				return act;
			
		}else if (nd instanceof Part) {
			
			Part part = (Part)nd;
			
			act = beforeVisitPart(part);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren)
				traverseBlock(part.getContent(), part);
			
			act = afterVisitPart(part);
			if (act == Action.Stop)
				return act;
			
		}
		
		else if (nd instanceof Chapter) {
			
			Chapter chap = (Chapter)nd;
			
			act = beforeVisitChapter(chap);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren)
				traverseBlock(chap.getContent(), chap);
			
			act = afterVisitChapter(chap);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof Section) {
			
			Section sec = (Section)nd;
			
			act = beforeVisitSection(sec);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren)
				traverseBlock(sec.getContent(), sec);
			
			act = afterVisitSection(sec);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof SubSection) {
			
			SubSection subsec = (SubSection)nd;
			
			act = beforeVisitSubSection(subsec);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren)
				traverseBlock(subsec.getContent(), subsec);
			
			act = afterVisitSubSection(subsec);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof Command) {
			
			Command cmd = (Command)nd;
			
			if (cmd.getNumParameters() == 0) {
				if (visitCommandNoParams(cmd) == Action.Stop)
					return Action.Stop;
			}
			else {
				
				act = beforeVisitCommand(cmd);
				if (act == Action.Stop)
					return act;
				
				if (act == Action.TraverseChildren) {
					
					if (cmd.options != null && shouldDescendOptions())
						traverseBlock(cmd.options, cmd);
					
					for (int i = 0; i < cmd.getNumParameters(); i++) {
						traverseBlock(cmd.getParameter(i), cmd);
					}
				}
				
				act = afterVisitCommand(cmd);
				if (act == Action.Stop)
					return act;
				
			}
		}
		else if (nd instanceof Enumerate.Item) {
			
			Enumerate.Item itm = (Enumerate.Item)nd;
			
			act = beforeVisitEnumerateItem(itm);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren) {
				
				if (itm.getLabel() != null) {
					
					act = beforeVisitEnumerateItemLabel(itm);
					if (act == Action.Stop)
						return act;
					
					if (act == Action.TraverseChildren) {
						traverseBlock(itm.getLabel(), itm);
					}
					
					act = afterVisitEnumerateItemLabel(itm);
					if (act == Action.Stop)
						return act;
					
				}
				
				traverseBlock(itm.getContent(), itm);
			}

			act = afterVisitEnumerateItem(itm);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof Enumerate) {
			
			Enumerate en = (Enumerate)nd;
			
			act = beforeVisitEnumerate(en);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren) {
				act = traverseEnumerate(en);
				if (act == Action.Stop)
					return act;
			}
			
			act = afterVisitEnumerate(en);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof Tabular.Cell) {
			
			Tabular.Cell cell = (Tabular.Cell)nd;
			
			act = beforeVisitCell(cell);
			if (act == Action.Stop)
				return act;
			
			if (act == Action.TraverseChildren)
				traverseBlock(cell.getContent(), cell);

			act = afterVisitCell(cell);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof Tabular) {
			
			Tabular tab = (Tabular)nd;
			
			act = beforeVisitTabular(tab);
			if (act == Action.Stop)
				return act;
			
			// options, parameters?
			if (act == Action.TraverseChildren) {
				act = traverseTabular(tab);
				if (act == Action.Stop)
					return act;
			}
			
			act = afterVisitTabular(tab);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof Environment) {
			
			Environment env = (Environment)nd;
			
			// TODO: Try to get rid of the special econ cases?
			boolean isHTMLAsImage = env.name.equals("htmlasimage") ||
					env.name.equals("TikzFigure") || env.name.equals("TikzFigureWrap");
			
			act = beforeVisitEnvironment(env);
			if (act == Action.Stop)
				return act;
			
			// If this is an htmlasimage and the should... method returns false, then
			// never traverse further
			if (!isHTMLAsImage || shouldDescendHTMLAsImage()) {
			
				//options, parameters?
				if (act == Action.TraverseChildren) {
					
					if (env.options != null && shouldDescendOptions())
						traverseBlock(env.options, env);
					
					traverseBlock(env.content, env);
				}
				
			}
			
			act = afterVisitEnvironment(env);
			if (act == Action.Stop)
				return act;
			
		}
		else if (nd instanceof LiteralString) {
			if (visitLiteralString((LiteralString)nd) == Action.Stop)
				return Action.Stop;
		}
		else if (nd instanceof Comment) {
			if (visitComment((Comment)nd) == Action.Stop)
				return Action.Stop;
		}
		else if (nd instanceof MathMode) {
			if (visitMathMode((MathMode)nd) == Action.Stop)
				return Action.Stop;
		}
		else
			throw new RuntimeException("Unhandled node type:" + nd.getClass().getName());
		
		return Action.TraverseChildren;
	}
	
	// TODO: This should return STOP
	protected void traverseBlock(Block blk, Node context) {
		
		currentContext.push(context);
		currentBlock.push(blk);
		prevBlockPositions.push(currentBlockPosition);
		
		try {
			
			// Note that we cannot use for (Node ...) here because we may
			// intentionally modify modes (like collapse several into one).
			// HOWEVER, the sub-class needs be very careful if it is doing that.
			for (int i = 0; i < blk.values.size(); i++) {
				
				currentBlockPosition = i;
				
				Node nd = blk.values.get(i);
				
				Action act = traverseNode(nd);
				
				if (act == Action.Stop)
					return;
				
			}
			
		}
		finally {
			currentBlockPosition = prevBlockPositions.pop();
			currentBlock.pop();
			currentContext.pop();
		}
		
	}
	
	protected Action traverseTabular(Tabular tab) {
		
		currentContext.push(tab);
		prevBlockPositions.push(currentBlockPosition);
		
		try {
			
			currentBlockPosition = -1;
			if (tab.options != null && shouldDescendOptions())
				traverseBlock(tab.options, tab);
	
			int y = 0;
			for (ListIterator<Tabular.Row> rowIt = tab.getRowIterator(); rowIt.hasNext(); ) {
				
				currentBlockPosition = y;

				Tabular.Row row = rowIt.next();

				Action act = beforeVisitRow(row);
				if (act == Action.Stop)
					return act;
				
				if (act == Action.TraverseChildren) {

					currentContext.push(row);
					prevBlockPositions.push(currentBlockPosition);

					try {
						
						for (int x = 0; x < row.getCells().length; x++) {

							currentBlockPosition = x;

							Tabular.Cell cell = row.getCells()[x];
							
							if (cell instanceof Tabular.SpanCell) {
								
								act = visitSpanCell((Tabular.SpanCell)cell);
								
								if (act == Action.Stop)
									return act;
								else if (act == Action.TraverseChildren && ((Tabular.SpanCell) cell).getOriginalCell() != null) {
									
									act = traverseNode(((Tabular.SpanCell) cell).getOriginalCell());
									if (act == Action.Stop)
										return act;
									
								}
								
							}
							else {
								act = traverseNode(cell);
								if (act == Action.Stop)
									return act;
							}

							// Reposition if cells changed
							x = currentBlockPosition;
							
						}

					}
					finally {
						currentBlockPosition = prevBlockPositions.pop();
						currentContext.pop();
					}

					act = afterVisitRow(row);
					if (act == Action.Stop)
						return act;

				}

				// Reposition if rows changed
				if (y < currentBlockPosition) {
					y = 0;
					rowIt = tab.getRowIterator();
					row = rowIt.next();
					while (y < currentBlockPosition) {
						row = rowIt.next();
						y++;
					}
				}
				while (currentBlockPosition > y) {
					row = rowIt.next();
					y++;
				}
				
				// Next row
				y++;
			}
			
			return Action.TraverseChildren; // Anything but Stop
			
		}
		finally {
			currentBlockPosition = prevBlockPositions.pop();
			currentContext.pop();
		}
		
	}
	
	protected Action traverseEnumerate(Enumerate en) {
		
		currentContext.push(en);
		prevBlockPositions.push(currentBlockPosition);
		
		try {
			
			// Note that we cannot use for (Node ...) here because we may
			// intentionally modify modes (like collapse several into one).
			// HOWEVER, the sub-class needs be very careful if it is doing that.
			for (int i = 0; i < en.getItems().size(); i++) {
				
				currentBlockPosition = i;
				
				Node nd = en.getItems().get(i);
				
				Action act = traverseNode(nd);
				
				if (act == Action.Stop)
					return act;
				
				i = currentBlockPosition;
				
			}
			
			return Action.TraverseChildren; // Anything but stop
			
		}
		finally {
			currentBlockPosition = prevBlockPositions.pop();
			currentContext.pop();
		}
		
	}
	
	// Sub-class API.
	
	// Override to change the traversal behaviour
	
	protected boolean shouldDescendHTMLAsImage() { return true; }
	protected boolean shouldDescendOptions() { return true; }
	
	// Override any methods where you want notification of these events.

	protected Action beforeVisitBlock(Block blk) { return Action.TraverseChildren; }
	protected Action afterVisitBlock(Block blk) { return Action.TraverseChildren; }
	
	protected Action beforeVisitPart(Part part) { return Action.TraverseChildren; }
	protected Action afterVisitPart(Part part) { return Action.TraverseChildren; }
	
	protected Action beforeVisitChapter(Chapter chap) { return Action.TraverseChildren; }
	protected Action afterVisitChapter(Chapter chap) { return Action.TraverseChildren; }
	
	protected Action beforeVisitSection(Section sec) { return Action.TraverseChildren; }
	protected Action afterVisitSection(Section sec) { return Action.TraverseChildren; }
	
	protected Action beforeVisitSubSection(SubSection subsec) { return Action.TraverseChildren; }
	protected Action afterVisitSubSection(SubSection subsec) { return Action.TraverseChildren; }
	
	protected Action beforeVisitCommand(Command cmd) { return Action.TraverseChildren; }
	protected Action afterVisitCommand(Command cmd) { return Action.TraverseChildren; }
	
	protected Action visitCommandNoParams(Command cmd) { return Action.TraverseChildren; }
	
	protected Action beforeVisitEnvironment(Environment env) { return Action.TraverseChildren; }
	protected Action afterVisitEnvironment(Environment env) { return Action.TraverseChildren; }
	
	protected Action beforeVisitEnumerate(Enumerate en) { return Action.TraverseChildren; }
	protected Action afterVisitEnumerate(Enumerate en) { return Action.TraverseChildren; }
	protected Action beforeVisitEnumerateItem(Enumerate.Item itm) { return Action.TraverseChildren; }
	protected Action afterVisitEnumerateItem(Enumerate.Item itm) { return Action.TraverseChildren; }
	protected Action beforeVisitEnumerateItemLabel(Enumerate.Item itm) { return Action.TraverseChildren; }
	protected Action afterVisitEnumerateItemLabel(Enumerate.Item itm) { return Action.TraverseChildren; }
	
	protected Action beforeVisitTabular(Tabular tab) { return Action.TraverseChildren; }
	protected Action afterVisitTabular(Tabular tab) { return Action.TraverseChildren; }
	protected Action beforeVisitRow(Tabular.Row row) { return Action.TraverseChildren; }
	protected Action afterVisitRow(Tabular.Row row) { return Action.TraverseChildren; }
	protected Action beforeVisitCell(Tabular.Cell cell) { return Action.TraverseChildren; }
	protected Action afterVisitCell(Tabular.Cell cell) { return Action.TraverseChildren; }
	
	// Override this to make the traverser go into the original cell contents
	protected Action visitSpanCell(Tabular.SpanCell cell) { return Action.SkipChildren; }
	
	protected Action visitLiteralString(LiteralString str) { return Action.TraverseChildren; }
	
	protected Action visitMathMode(MathMode math) { return Action.TraverseChildren; }
	
	protected Action beforeVisitParagraph(Paragraph para) { return Action.TraverseChildren; }
	protected Action afterVisitParagraph(Paragraph para) { return Action.TraverseChildren; }
	
	protected Action visitComment(Comment cmt) { return Action.TraverseChildren; }
}
