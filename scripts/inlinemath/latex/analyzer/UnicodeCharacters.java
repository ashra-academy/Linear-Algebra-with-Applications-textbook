package latex.analyzer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;

/**
 * Replaces accent commands like \`, \', \^, and \" with accented unicode characters. 
 */
public class UnicodeCharacters extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		private static Hashtable<String, String> accentMapping;
		{
			accentMapping = new Hashtable<String, String>();
			
			// From: https://en.wikibooks.org/wiki/LaTeX/Special_Characters
			accentMapping.put("`", "\u0300");
			accentMapping.put("'", "\u0301");
			accentMapping.put("^", "\u0302");
			accentMapping.put("\"", "\u0308");
			accentMapping.put("H", "\u030B");
			accentMapping.put("~", "\u0303");
			accentMapping.put("c", "\u0327");
			accentMapping.put("k", "\u0328");
			accentMapping.put("=", "\u0304");
			accentMapping.put("b", "\u0332");
			accentMapping.put(".", "\u0307");
			accentMapping.put("d", "\u0323");
			accentMapping.put("r", "\u030A");
			accentMapping.put("u", "\u0306");
			accentMapping.put("v", "\u030C");
			
			// Affects two characters
			accentMapping.put("t", "\u0361");
		}
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {
			
			ArrayList<Block> paramArray = env.getParameters();
			
			if (paramArray != null) {
				for (ListIterator<Block> params = paramArray.listIterator(); params.hasNext(); ) {
					traverseBlock(params.next(), env);
				}
			}
			
			Command caption = env.getCaption();
			if (caption != null)
				traverseNode(caption);
			
			return super.beforeVisitEnvironment(env);
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			
			for (ListIterator<Node> ndIt = blk.values.listIterator(); ndIt.hasNext(); ) {
				
				Node nd = ndIt.next();
				
				if (nd instanceof Command) {
					Command cmd = (Command)nd;
					
					String composeStr = accentMapping.get(cmd.name);
					
					if (composeStr != null) {
						
						if (cmd.getParameter(0).values.size() != 1)
							throw new AnalysisException("Accent command parameter contains latex other than a string argument", cmd);
						
						Node strNd = cmd.getParameter(0).values.get(0);
						if (!(strNd instanceof LiteralString))
							throw new AnalysisException("Accent command parameter not a literal string", cmd);
						
						String letterStr = ((LiteralString)strNd).value;
						
						if (cmd.name.equals("t") && letterStr.length() != 2)
							throw new AnalysisException("Tie command can only be applied to a pair of characters", cmd);
						else if (letterStr.length() != 1)
							throw new AnalysisException("Accent can only be applied to a single character", cmd);
						
						letterStr += composeStr;
						
						((LiteralString)strNd).value = letterStr;

						// Replace the command with the adjusted string
						ndIt.remove();
						ndIt.add(strNd);
						
					}
					else if (cmd.name.equals("l")) {
						
						if (cmd.getParameter(0).values.size() != 0)
							throw new AnalysisException("barred l command parameter contains a non-blank argument", cmd);
						
						LiteralString strNd = new LiteralString("\u0142", // l with bar across
								cmd.getInputFile(), cmd.getLineNum(), cmd.getColNum());
						
						ndIt.remove();
						ndIt.add(strNd);
						
					}
					else if (cmd.name.equals("o")) {
						
						if (cmd.getParameter(0).values.size() != 0)
							throw new AnalysisException("slashed o command parameter contains a non-blank argument", cmd);
						
						LiteralString strNd = new LiteralString("\u00f8", // o with stroke
								cmd.getInputFile(), cmd.getLineNum(), cmd.getColNum());
						
						ndIt.remove();
						ndIt.add(strNd);
						
					}
					else if (cmd.name.equals("aa")) { // shortcut to \r{a}
						
						if (cmd.getParameter(0).values.size() != 0)
							throw new AnalysisException("barred l command parameter contains a non-blank argument", cmd);
						
						LiteralString strNd = new LiteralString("\u00e5",
								cmd.getInputFile(), cmd.getLineNum(), cmd.getColNum());
						
						ndIt.remove();
						ndIt.add(strNd);
						
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
