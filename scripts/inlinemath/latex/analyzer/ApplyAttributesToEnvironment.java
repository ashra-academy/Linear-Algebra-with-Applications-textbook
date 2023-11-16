package latex.analyzer;


import java.util.HashSet;
import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Comment;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Tabular;

/**
 * Searches for commands like \centering and font sizes, tagging them to their
 * parent environment so that they can be handled as special cases in the
 * writer phase. 
 */
public class ApplyAttributesToEnvironment extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		private static HashSet<String> commandsToAttributes;
		{
			commandsToAttributes = new HashSet<String>();
			
			commandsToAttributes.add("tiny");
			commandsToAttributes.add("scriptsize");
			commandsToAttributes.add("footnotesize");
			commandsToAttributes.add("small");
			commandsToAttributes.add("normalsize");
			commandsToAttributes.add("large");
			commandsToAttributes.add("Large");
			commandsToAttributes.add("LARGE");
			commandsToAttributes.add("huge");
			commandsToAttributes.add("Huge");
			commandsToAttributes.add("HUGE");
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
				
			ListIterator<Node> ndIt = blk.values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Command))
					continue;
				
				Command cmd = (Command)nd;
				
				if (cmd.name.equals("centering") || cmd.name.equals("centerline")) {
					
					ndIt.remove();
					
					if (cmd.name.equals("centerline")) {
						ndIt.add(cmd.getParameter(0));
					}
					
					if (context instanceof Environment) {
						Environment env = (Environment)context;
						
						env.setCentering(true);
					}
					else if (context instanceof Block) {
						
						// Sometimes we define a block to center a longtable since that
						// environment is both like a table and inner tabular at the same time
						
						// Look for the exact case of only one tabluar inside the block and
						// whitespace
						Tabular subTab = null;
						boolean onlyWhitespace = true;
						
						ListIterator<Node> contextIt = ((Block)context).values.listIterator();
						
						while (contextIt.hasNext()) {
							
							Node tmpNd = contextIt.next();
							
							if (tmpNd instanceof LiteralString) {
								if (!((LiteralString)tmpNd).isWhitespace()) {
									onlyWhitespace = false;
									break;
								}
							}
							else if (tmpNd instanceof Tabular) {
								if (subTab != null) {
									onlyWhitespace = false;
									break;
								}
								
								subTab = (Tabular)tmpNd;
							}
							else if (tmpNd instanceof Command) {
								if (!((Command)tmpNd).name.equals("footnotesize")) {
									onlyWhitespace = false;
									break;
								}
							}
							else if (tmpNd instanceof Comment) {
								// Ignore
							}
							else {
								onlyWhitespace = false;
								break;
							}
							
						} // foreach node
						
						if (!onlyWhitespace || subTab == null)
							throw new AnalysisException("Unhandled centering on " + context.getClass().getName(), context);
						
						subTab.setCentering(true);
						
					}
					else {
						throw new AnalysisException("Unhandled centering on " + context.getClass().getName(), context);
					}
					
				}
				else if (commandsToAttributes.contains(cmd.name)) {
					
					// These commands are allowed outside of environments too, but
					// in this context are moved to an attribute of the environment
					
					if (context instanceof Environment) {
						
						ndIt.remove();
						
						Environment env = (Environment)context;
						
						env.addAttribute(cmd);
					}
					
				}
				
			}
			
			super.traverseBlock(blk, context);
			
		}
	
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		new Traverser().traverseBlock(topLevelBlock, topLevelBlock);
	}
	
}
