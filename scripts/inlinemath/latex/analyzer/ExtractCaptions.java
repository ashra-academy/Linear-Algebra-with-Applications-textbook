package latex.analyzer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Chapter;
import latex.node.Command;
import latex.node.Environment;
import latex.node.MathMode;
import latex.node.Node;
import latex.node.Section;

/**
 * Finds caption commands and sets them as the Environment's caption field,
 * numbering the environments as it goes along.
 */
public class ExtractCaptions extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		private int currentChapterNum;
		private int currentSectionNum;
		private Hashtable<String, Integer> chapterCounters;
		private Hashtable<String, Integer> sectionCounters;
		
		@Override
		protected Action beforeVisitChapter(Chapter chap) {
			
			// Reset counters that depend on the chapter
			chapterCounters = new Hashtable<String, Integer>();
			
			currentChapterNum = chap.getNumber();
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitSection(Section sec) {
			
			// Reset counters that depend on the section
			sectionCounters = new Hashtable<String, Integer>();
			
			// This is how Latex behaves.  \thesection is a global variable that
			// is only updated for numbered sections, so using \thesection in
			// an unnumbered section will result in the last numbered section being
			// returned.
			if (sec.getNumber() > 0)
				currentSectionNum = sec.getNumber();
			
			return Action.TraverseChildren;
		}
		
		private int getNextChapterCount(String counterName) {
			
			if (counterName.equals("FigureBox"))
				counterName = "figure";
			else if (counterName.equals("longtabu") || counterName.equals("Table"))
				counterName = "table";
				
			Integer counter = chapterCounters.get(counterName);
			if (counter == null)
				counter = 0;
			
			int retVal = counter + 1;
			
			chapterCounters.put(counterName, retVal);
			
			return retVal;
		}
		
		private int getNextSectionCount(String counterName) {
			
			Integer counter = sectionCounters.get(counterName);
			if (counter == null)
				counter = 0;
			
			int retVal = counter + 1;
			
			sectionCounters.put(counterName, retVal);
			
			return retVal;
		}
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {
			
			Block captionBlock = null;
			
			// Special econ environments
			if (env.name.equals("ApplicationBox") || env.name.equals("ExampleBox")
					|| env.name.equals("Table") || env.name.equals("TikzFigure")
					|| env.name.equals("TikzFigureWrap") ) {
				
				captionBlock = env.getPgfParameter("caption");
				
			}
			else if (env.name.equals("ex")) {
				env.setReferenceNumber(currentChapterNum + "." + currentSectionNum + "." + getNextSectionCount(env.name));
			}
			else if (env.name.equals("econex")) {
				env.setReferenceNumber(currentChapterNum + "." + getNextChapterCount(env.name));
			}
			else if (env.name.equals("acctex") || env.name.equals("acctprob")) {
				env.setReferenceNumber(currentChapterNum + "--" + getNextChapterCount(env.name));
			}
			else if (env.name.equals("definition") || env.name.equals("theorem") || env.name.equals("corollary") || env.name.equals("example")) {
				// These boxes share the same counter in Calculus
				env.setReferenceNumber(currentChapterNum + "." + getNextChapterCount("theorem"));
			}
			
			
			if (captionBlock != null) {
				
				Command caption = new Command(captionBlock.getInputFile(), captionBlock.getLineNum(), captionBlock.getColNum());
				caption.name = "caption";
				caption.addParameter(captionBlock);
				env.setCaption(caption);
				
				env.setReferenceNumber(currentChapterNum + "." + getNextChapterCount(env.name));
				
			}
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			
			for (Iterator<Node> ndIt = blk.values.iterator(); ndIt.hasNext(); ) {
				
				Node nd = ndIt.next();
				
				if (nd instanceof Command) {
					
					Command cmd = (Command)nd;
					
					if (cmd.name.equals("caption")) {
						
						int pos = currentContext.size(); // one past the stack, where "context" would be
						Node parent = context;
						
						while (pos >= 0 && !(parent instanceof Environment)) {
							pos--;
							parent = currentContext.get(pos);
						}
						if (pos < 0)
							throw new AnalysisException("\\caption not contained in any Environment/Tabular", cmd);
						
						Environment envParent = ((Environment)parent);
						
						envParent.setCaption(cmd);
						
						envParent.setReferenceNumber(currentChapterNum + "." + getNextChapterCount(envParent.name));
						
						ndIt.remove();
						
					}
					
				} // node is a caption
				
			} // for each node
			
			super.traverseBlock(blk, context);
		}
		
		private boolean blockContainsNoTag(Block blk) {
			
			for (Iterator<Node> it = blk.values.iterator(); it.hasNext(); ) {
				
				Node nd = it.next();
				
				if (!(nd instanceof Command))
					continue;
				
				Command cmd = (Command)nd;
				
				if (cmd.name.equals("notag") || cmd.name.equals("nonumber"))
					return true;
				
			}
			
			return false;
			
		}
		
		@Override
		protected Action visitMathMode(MathMode math) {
			
			if (math.getStyle() != MathMode.Style.GATHER &&
				math.getStyle() != MathMode.Style.ALIGN) {
				
				return Action.TraverseChildren;
			}
				
			if (math.getName().endsWith("*"))
				return Action.TraverseChildren;
			
			int startAt = getNextChapterCount("math");
			
			ArrayList<ArrayList<Block>> equations = math.getEquations();
			ArrayList<String> referenceNumbers = math.getReferenceNumbers();
			
			Node parent = currentContext.peek();
			if (parent instanceof Environment) {
				
				Environment env = (Environment)parent;
				
				if (env.name.equals("subequations")) {
					
					// TODO: How to handle \nonumber or \notag commands?
					
					env.setReferenceNumber(currentChapterNum + "." + startAt);
					
					char c = 'a';
					
					for (int i = 0; i < equations.size(); i++) {
						referenceNumbers.add(currentChapterNum + "." + startAt + Character.toString(c));
						c++;
						if (c == '{')
							throw new RuntimeException("Too many subequations to number");
					}
					
					// startAt was already set as the last used number in the HT
					
					return Action.TraverseChildren;
				}
				
			}
			
			int curNumber = startAt;
			
			for (int i = 0; i < equations.size(); i++) {
				
				ArrayList<Block> eqnLine = equations.get(i);
				boolean hasNoTag = false;
				
				for (int j = 0; j < eqnLine.size(); j++) {
					
					if (blockContainsNoTag(eqnLine.get(j))) {
						hasNoTag = true;
						break;
					}
					
				}
				
				if (hasNoTag)
					referenceNumbers.add(""); // Don't number this line
				else {
					referenceNumbers.add(currentChapterNum + "." + curNumber);
					curNumber++;
				}
				
			}
			
			chapterCounters.put("math", curNumber - 1);
			
			return Action.TraverseChildren;
			
		}
		
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		
		Environment document = getDocumentEnvironment(topLevelBlock); 
		
		new Traverser().traverseBlock(document.content, document);
		
	}

}
