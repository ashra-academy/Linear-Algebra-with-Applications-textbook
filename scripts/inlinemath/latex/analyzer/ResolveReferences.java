package latex.analyzer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import latex.NodeTraverser;
import latex.node.Block;
import latex.node.Chapter;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.MathMode;
import latex.node.Node;
import latex.node.Paragraph;
import latex.node.Part;
import latex.node.Section;
import latex.node.SubSection;
import latex.node.Tabular;

/**
 * Finds all \label commands and numbers the chapters, sections, subsections and environments
 * that the label applies to.  Then finds all \ref commands and replaces them with some text
 * based on the label (like "Chapter X" or "Figure X.Y").
 * 
 * Note that labels are left in place in case the writer wants to use that to create anchor tags.
 */
public class ResolveReferences extends Analyzer {
	
	private Hashtable<String, String> labels = new Hashtable<String, String>();
	
	private String getLabelName(Command cmd) {
		
		if (cmd.getParameter(0).values.size() != 1)
			throw new AnalysisException("\\" + cmd.name + " parameter contains other latex", cmd);
		
		Node labelNode = cmd.getParameter(0).values.get(0);
		if (!(labelNode instanceof LiteralString))
			throw new AnalysisException("\\" + cmd.name + " parameter not a literal string", cmd);
		
		String labelStr = ((LiteralString)labelNode).value;
		labelStr = labelStr.replaceAll("[\n ]+", " "); // Latex treats multiple \n and ' ' as just one space
		
		if (labels.containsKey(labelStr))
			throw new AnalysisException("\\" + cmd.name + " already exists:" + labelStr, cmd);
		
		return labelStr;
	}
	
	private class LabelBuilder extends NodeTraverser {
		
		private int currentChapterNum;
		
		@Override
		protected Action beforeVisitChapter(Chapter chap) {
			
			currentChapterNum = chap.getNumber();
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {
			
			if (env.name.equals("example") || env.name.equals("definition")
					|| env.name.equals("theorem") || env.name.equals("corollary")
					|| env.name.equals("algorithm") || env.name.equals("proposition")) {

				String prefix = null;
				if (env.name.equals("example"))
					prefix = "exa";
				else if (env.name.equals("definition"))
					prefix = "def";
				else if (env.name.equals("theorem"))
					prefix = "thm";
				else if (env.name.equals("corollary"))
					prefix = "cor";
				else if (env.name.equals("algorithm"))
					prefix = "algo";
				else if (env.name.equals("proposition"))
					prefix = "prop";
				
				// TODO: Do we need to actually parse this as latex?
				String labelStr = prefix + ":" + env.getParameter(1).toLatexString(false);
				
				labels.put(labelStr, env.getReferenceNumber());
			}
			
			// Look for labels that are inside captions as well
			if (env.getCaption() != null)
				traverseBlock(env.getCaption().getParameter(0), env);
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitTabular(Tabular tab) {
			
			// Look for labels that are inside captions as well
			if (tab.getCaption() != null)
				traverseBlock(tab.getCaption().getParameter(0), tab);
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitCommand(Command cmd) {
			
			if (cmd.name.equals("label")) {
				
				String labelStr = getLabelName(cmd);
				
				int pos = currentContext.size() - 1;
				Node parent = currentContext.get(pos);
				
				while (pos >= 0 && (parent instanceof Paragraph || parent instanceof Command || parent instanceof SubSection)) {
					pos--;
					parent = currentContext.get(pos);
				}
				if (pos < 0)
					throw new AnalysisException("\\label not contained in any valid node type for " + labelStr, cmd);
				
				if (parent instanceof Section) {
					
					Section sec = (Section)parent;
					
					if (sec.getNumber() < 1)
						throw new AnalysisException("TODO: How to deal with unnumbered sections for " + labelStr, cmd);
					
					// A reference to a section number
					labels.put(labelStr, currentChapterNum + "." + sec.getNumber());
				}
				else if (parent instanceof Chapter) {
					
					Chapter chap = (Chapter)parent;
					
					if (chap.getNumber() < 1)
						throw new AnalysisException("TODO: How to deal with unnumbered chapters for " + labelStr, cmd);
					
					// A reference to a chapter number
					labels.put(labelStr, "" + currentChapterNum);
				}
				else if (parent instanceof Environment) {
					
					Environment env = (Environment)parent;
					
					if (env.name.equals("figure") || env.name.equals("table") || env.name.equals("longtabu") 
							|| env.name.equals("array") || env.name.equals("subequations")
							
							|| env.name.equals("example") || env.name.equals("theorem") || env.name.equals("ex")
							|| env.name.equals("app")  // Math
							
							|| env.name.equals("acctdisc") || env.name.equals("acctex") || env.name.equals("acctprob") // Accounting
							
							|| env.name.equals("ApplicationBox") || env.name.equals("ExampleBox") || env.name.equals("Table") // Econ
							|| env.name.equals("TikzFigure") || env.name.equals("TikzFigureWrap") || env.name.equals("econex")
							) {
						
						labels.put(labelStr, env.getReferenceNumber());
					}
					else {
						throw new AnalysisException("Unhandled environment type: " + env.name + " for " + labelStr, cmd);
					}
					
				}
				else {
					throw new AnalysisException("Unhandled type: " + parent.getClass().getName() + " for " + labelStr, cmd);
				}
				
				// TODO: Remove from parent?  Or use to build anchor tags?
				
				return Action.SkipChildren;
			}
			
			return Action.TraverseChildren;
		}

		@Override
		protected Action visitMathMode(MathMode math) {

			// Check for labels in math environments (equation, gather, etc)
			for (int row = 0; row < math.getEquations().size(); row++) {
				
				ArrayList<Block> rowData = math.getEquations().get(row);

				for (int col = 0; col < rowData.size(); col++) {

					Block block = rowData.get(col);

					// TODO: We should call traverseBlock to re-use the code above...

					for (Iterator<Node> ndIt = block.values.iterator(); ndIt.hasNext(); ) {

						Node nd = ndIt.next();

						if (nd instanceof Command) {

							Command cmd = (Command)nd;

							if (cmd.name.equals("label")) {

								String labelStr = getLabelName(cmd);

								labels.put(labelStr, math.getReferenceNumbers().get(row));

							}

						}

					}
				}
				
			}

			return Action.TraverseChildren;
		}
		
		protected void run(Block blk, Node context) {
			try {
				super.traverseBlock(blk, context);
			}
			catch (ThreadDeath t) {
				throw t;
			}
			catch (Throwable t) {
				if (currentContext.size() > 0)
					throw new AnalysisException("Exception thrown", t, currentContext.peek());
				else
					throw new AnalysisException("Exception thrown", t, context);
			}
		}
		
	}
	
	
	private class NewLabelBuilder extends NodeTraverser {
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {
			
			// Shortcut to avoid parsing the entire document
			if (env.name.equals("document"))
				return Action.SkipChildren;
			
			return super.beforeVisitEnvironment(env);
		}
		
		@Override
		protected Action beforeVisitCommand(Command cmd) {
			
			if (cmd.name.equals("newlabel")) {
				
				String labelStr = getLabelName(cmd);
				
				Block secondParam = cmd.getParameter(1);
				
				if (secondParam.values.size() != 5)
					throw new AnalysisException("Second argument to newlabel should have 5 sub-blocks", cmd);
				
				for (Node nd : secondParam.values) {
					if (!(nd instanceof Block))
						throw new AnalysisException("Second argument to newlabel should have 5 sub-blocks", cmd);
				}
				
				Block chapNumBlock = (Block) secondParam.values.get(0);
				
				if (chapNumBlock.values.size() != 1)
					throw new AnalysisException("newlabel chapter number parameter contains other latex", cmd);
				
				Node chapNumNode = chapNumBlock.values.get(0);
				if (!(chapNumNode instanceof LiteralString))
					throw new AnalysisException("newlabel chapter number parameter not a literal string", cmd);
				
				String chapNumStr = ((LiteralString)chapNumNode).value;
				
				labels.put(labelStr, chapNumStr);
				
			}
			
			return super.beforeVisitCommand(cmd);
		}
		
		protected void run(Block blk, Node context) {
			try {
				super.traverseBlock(blk, context);
			}
			catch (ThreadDeath t) {
				throw t;
			}
			catch (Throwable t) {
				if (currentContext.size() > 0)
					throw new AnalysisException("Exception thrown", t, currentContext.peek());
				else
					throw new AnalysisException("Exception thrown", t, context);
			}
		}
		
	}
	
	
	private class RefResolver extends NodeTraverser {
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {

			// Traverse parameters in case they have references in their text (captions, etc)
			if (env.getParameters() != null) {
				for (Block blk : env.getParameters()) {
					traverseBlock(blk, env);
				}
			}
			
			if (env.getPgfParameters() != null) {
				for (Block blk : env.getPgfParameters().values()) {
					traverseBlock(blk, env);
				}
			}
			
			if (env.getCaption() != null)
				traverseBlock(env.getCaption().getParameter(0), env);
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitCommand(Command cmd) {
			
			
			if (cmd.name.equals("ref") || cmd.name.equals("ref*")) {
				
				if (cmd.getParameter(0).values.size() != 1)
					throw new AnalysisException("\\ref parameter contains other latex", cmd);
				
				Node labelNode = cmd.getParameter(0).values.get(0);
				if (!(labelNode instanceof LiteralString))
					throw new AnalysisException("\\ref parameter not a literal string", cmd);
				
				String labelStr = ((LiteralString)labelNode).value;
				labelStr = labelStr.replaceAll("[\n ]+", " "); // Latex treats multiple \n and ' ' as just one space
				
				String value = labels.get(labelStr);
				if (value == null)
					throw new AnalysisException("Undefined reference to: " + labelStr, cmd);
				
				Block parent = currentBlock.peek();
				
				parent.values.remove(currentBlockPosition);
				parent.values.add(currentBlockPosition, new LiteralString(value,
						cmd.getInputFile(), cmd.getLineNum(), cmd.getColNum()));
				
				return Action.SkipChildren;
			}
			else if (cmd.name.equals("eqref")) {
				
				if (cmd.getParameter(0).values.size() != 1)
					throw new AnalysisException("\\eqref parameter contains other latex", cmd);
				
				Node labelNode = cmd.getParameter(0).values.get(0);
				if (!(labelNode instanceof LiteralString))
					throw new AnalysisException("\\eqref parameter not a literal string", cmd);
				
				String labelStr = ((LiteralString)labelNode).value;
				labelStr = labelStr.replaceAll("[\n ]+", " "); // Latex treats multiple \n and ' ' as just one space
				
				String value = labels.get(labelStr);
				if (value == null)
					throw new AnalysisException("Undefined reference to: " + labelStr, cmd);
				
				Block parent = currentBlock.peek();
				
				parent.values.remove(currentBlockPosition);
				parent.values.add(currentBlockPosition, new LiteralString(value,
						cmd.getInputFile(), cmd.getLineNum(), cmd.getColNum()));
				
				return Action.SkipChildren;
			}
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action visitMathMode(MathMode math) {
			
			for (ArrayList<Block> row : math.getEquations()) {
				
				for (Block cell : row) {
			
					traverseBlock(cell, math);
					
				}
				
			}
			
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitPart(Part part) {
			traverseBlock(part.getTitle(), part);
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitChapter(Chapter chap) {
			traverseBlock(chap.getTitle(), chap);
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitSection(Section sec) {
			traverseBlock(sec.getTitle(), sec);
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitSubSection(SubSection subsec) {
			traverseBlock(subsec.getTitle(), subsec);
			return Action.TraverseChildren;
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			super.traverseBlock(blk, context);
		}
		
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		
		Environment document = getDocumentEnvironment(topLevelBlock);
		
		new NewLabelBuilder().run(topLevelBlock, topLevelBlock);
		new LabelBuilder().run(document.content, document);
		new RefResolver().traverseBlock(document.content, document);
		
	}
	
}
