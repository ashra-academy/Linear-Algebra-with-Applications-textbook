package latex.analyzer;

import java.util.ArrayList;
import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.Enumerate;
import latex.node.LiteralString;
import latex.node.Node;

/**
 * Converts environments like enumerate, itemize, and description into
 * Enumerate nodes.
 */
public class ExtractEnumerate extends Analyzer {
	
	private static String[] letterStyles = new String[] {"1", "a", "i", "A"};
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		private boolean inEnumialphastyle = false;
		private boolean inEnumialphparenastyle = false;
		private int enumerateLevel = 0;
		
		/**
		 * 
		 * Sometimes we use environments like landscape to rotate pages
		 * within a list environment, but for the purposes of this compiler
		 * we should pull out items and place the landscape inside the item
		 * instead of the other way around
		 */
		private void pushDownEnvironments(Enumerate lstNd, Block lstBlk) {
			
			ListIterator<Node> lstNdIt = lstBlk.values.listIterator();
			
			while (lstNdIt.hasNext()) {
				
				Node nd = lstNdIt.next();
				
				if (!(nd instanceof Environment))
					continue;
					
				Environment env = (Environment)nd;
				
				if (!env.name.equals("landscape"))
					continue;
				
				ArrayList<Block> subItemBlocks = Analyzer.splitBy(env.content.values, "item");
				
				// If there are no item commands then we can quit now
				if (subItemBlocks.size() == 1)
					continue;
				
				// We want to remove this landscape from lstBlk and 
				// replace it with the item command, then the rest
				// of the content wrapped in a new landscape
				
				lstNdIt.remove();
				
				for (Block splitBlk : subItemBlocks) {
					
					if (splitBlk.values.get(0) instanceof Command) {
				
						Command itemCmd = (Command) splitBlk.values.get(0);
						
						if (itemCmd.name.equals("item")) {
							
							// Move up to parent
							splitBlk.values.remove(0);
							lstNdIt.add(itemCmd);
							
						}
						
					}
					
					if (splitBlk.values.size() < 1)
						continue;
					
					// Place the rest of the nodes into a new landscape
					
					Node subNd = splitBlk.values.get(0);
					
					Environment newEnv = new Environment(subNd.getInputFile(), subNd.getLineNum(), subNd.getColNum());
					newEnv.name = env.name;
					
					newEnv.content = new Block(subNd.getInputFile(), subNd.getLineNum(), subNd.getColNum());
					newEnv.content.values = splitBlk.values;
					
					lstNdIt.add(newEnv);
					
				}
				
			}
			
		}
		
		private void splitByItems(Enumerate lstNd, Block blk) {
			
			Block itemBlk = null;
			
			for (int pos = 0; pos < blk.values.size(); pos++) {
				
				Node nd = blk.values.get(pos);
				
				if (nd instanceof Command) {
					
					Command cmd = (Command)nd;
					
					if (cmd.name.equals("item")) {
						
						if (pos+1 == blk.values.size())
							throw new AnalysisException("Missing content after \\item command", cmd);
						
						Node nextNd = blk.values.get(pos+1);
						
						itemBlk = new Block(nextNd.getInputFile(), nextNd.getLineNum(), nextNd.getColNum());
						
						Enumerate.Item itemNd = new Enumerate.Item(cmd.getInputFile(), cmd.getLineNum(),
								cmd.getColNum(), itemBlk);
						
						if (cmd.options != null) {
							itemNd.setLabel(cmd.options);
							cmd.options = null;
						}
						
						lstNd.addItem(itemNd);
						
						continue;
					}
					
				}
				else if (nd instanceof Environment) {
					
					Environment env = (Environment)nd;
					
					// Accounting discussion environments are an alias to \item
					if (env.name.equals("acctdisc")) {
						
						itemBlk = new Block(env.getInputFile(), env.getLineNum(), env.getColNum());
						
						itemBlk.values.add(env);
						
						Enumerate.Item itemNd = new Enumerate.Item(env.getInputFile(), env.getLineNum(),
								env.getColNum(), itemBlk);
						
						lstNd.addItem(itemNd);
						
						continue;
					}
					
				}
				
				if (itemBlk != null) {
					itemBlk.values.add(nd);
				}
				
			}
			
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			
			ListIterator<Node> ndIt = blk.values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Environment))
					continue;
					
				Environment env = (Environment)nd;
				
				if (env.name.equals("enumerate") || env.name.equals("itemize")
						|| env.name.equals("description")) {
					
					Enumerate lstNd = new Enumerate(env.name, env.getInputFile(), env.getLineNum(), env.getColNum(),
							env.getPgfParameters());
					
					pushDownEnvironments(lstNd, env.content);
					
					splitByItems(lstNd, env.content);
					
					// Replace Environment with Enumerate
					ndIt.set(lstNd);
					
				}
				else if (env.name.equals("enumialphastyle")) {
					
					if (inEnumialphastyle || inEnumialphparenastyle)
						throw new AnalysisException("enumialphastyle is nested within itself", env);
					
					inEnumialphastyle = true;
					
					// Contexts will not be set properly be we do not rely on them here anyway
					traverseNode(env);
					
					inEnumialphastyle = false;
					
					ndIt.remove();
					
					ArrayList<Node> envContent = env.content.values;
					
					for (Node child : envContent) {
						ndIt.add(child);
					}
					
				}
				else if (env.name.equals("enumialphparenastyle")) {
					
					if (inEnumialphastyle || inEnumialphparenastyle)
						throw new AnalysisException("enumialphparenastyle is nested within itself", env);
					
					inEnumialphparenastyle = true;
					
					// Contexts will not be set properly be we do not rely on them here anyway
					traverseNode(env);
					
					inEnumialphparenastyle = false;
					
					ndIt.remove();
					
					ArrayList<Node> envContent = env.content.values;
					
					for (Node child : envContent) {
						ndIt.add(child);
					}
					
				}
				
			}
			
			// Note: if enumialphastyle went above then this will re-traverse the enumerations
			// But the styling should only be set once, so there should be no harm done
			
			super.traverseBlock(blk, context);
		}
		
		@Override
		protected Action beforeVisitEnumerate(Enumerate en) {
			
			if (en.getName().equals("enumerate")) {
			
				enumerateLevel++;
			
				// If item style is not set, then automatically assign numbering types by level
				if (en.getPgfParameter("label") == null) {
				
					if (enumerateLevel > 3)
						throw new AnalysisException("enumerate too deeply nested", en);
					
					if (enumerateLevel > 0) {
						
						String letter = letterStyles[enumerateLevel - 1];
						
						// enumialphastyle overrides the first level to be alpha
						if ((inEnumialphastyle || inEnumialphparenastyle) && enumerateLevel == 1)
							letter = "a";
						
						Block style = new Block(en.getInputFile(), en.getLineNum(), en.getColNum());
						
						style.values.add(new LiteralString(letter + ".", en.getInputFile(), en.getLineNum(), en.getColNum()));
						
						en.addPgfParameter("label", style);
						
					}
					
				}
				
			}
			
			
			return super.beforeVisitEnumerate(en);
		}
		
		@Override
		protected Action afterVisitEnumerate(Enumerate en) {
			
			if (en.getName().equals("enumerate"))
				enumerateLevel--;
			
			return super.afterVisitEnumerate(en);
		}
		
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		
		Environment document = getDocumentEnvironment(topLevelBlock); 
		
		new Traverser().traverseBlock(document.content, document);
	}

}
