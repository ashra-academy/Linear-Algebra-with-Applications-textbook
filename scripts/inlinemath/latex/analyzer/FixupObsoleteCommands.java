package latex.analyzer;

import java.util.ArrayList;
import java.util.Hashtable;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Node;

/**
 * Replaces some obsolete commands with newer styles.  Old commands like \bf apply to all
 * of their siblings after the command appears, until the end of the current block/context.
 * This is messy to work with.  So instead we change the style of command to something like
 * \textbf{...} and place all of the siblings as children of this new command.
 * 
 * For example:
 *     a -> { ... -> \bf -> b -> c -> d } -> e
 * will become:
 *     a -> { ... -> \textbf{ b -> c -> d} } -> e
 * 
 * Note: This analyzer modifies the originial latex layout in a way that is not possible to
 *       revert.  A compiled PDF should look the same, but the source is clearly changed.
 * 
 */
public class FixupObsoleteCommands extends Analyzer {
	
	private static Hashtable<String, String> commandsToReplace;
	{
		commandsToReplace = new Hashtable<String, String>();
		// These are obsolete and have replacements
		commandsToReplace.put("bf", "textbf");
		commandsToReplace.put("bfseries", "textbf");
		commandsToReplace.put("sf", "textsf");
		commandsToReplace.put("sffamily", "textsf");
		commandsToReplace.put("tt", "texttt");
		commandsToReplace.put("ttfamily", "texttt");
		commandsToReplace.put("rm", "textrm");
		commandsToReplace.put("rmfamily", "textrm");
		commandsToReplace.put("it", "textit");
		commandsToReplace.put("itshape", "textit");
		commandsToReplace.put("sl", "textsl");
		commandsToReplace.put("slshape", "textsl");
		commandsToReplace.put("sc", "textsc");
		commandsToReplace.put("scshape", "textsc");
		commandsToReplace.put("em", "emph");
		commandsToReplace.put("upshape", "textup");
		commandsToReplace.put("mdseries", "textmd");
		// These are not obsolete but need to have the nodes after placed as children
		commandsToReplace.put("tiny", "tiny");
		commandsToReplace.put("scriptsize", "scriptsize");
		commandsToReplace.put("footnotesize", "footnotesize");
		commandsToReplace.put("small", "small");
		commandsToReplace.put("normalsize", "normalsize");
		commandsToReplace.put("large", "large");
		commandsToReplace.put("Large", "Large");
		commandsToReplace.put("LARGE", "LARGE");
		commandsToReplace.put("huge", "huge");
		commandsToReplace.put("Huge", "Huge");
		commandsToReplace.put("HUGE", "HUGE");
		commandsToReplace.put("flushleft", "flushleft");
		commandsToReplace.put("raggedright", "raggedright");
		// TODO: color -> but this creates 2 parameters, the color name and the sub-tree text
	}
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
	
		@Override
		protected void traverseBlock(Block blk, Node context) {
			
			currentContext.push(context);
			currentBlock.push(blk);
			prevBlockPositions.push(currentBlockPosition);
			
			try {
				
				for (int i = 0; i < blk.values.size(); i++) {
					
					Node nd = blk.values.get(i);
					
					if (!(nd instanceof Command)) {
						// Something else, so just traverse it
						Action act = traverseNode(nd);
						
						if (act == Action.Stop)
							return;
						
						continue;
					}
					
					Command cmdNode = (Command)nd;
					
					String newCommandName = commandsToReplace.get(cmdNode.name);
					
					if (newCommandName == null) {
						// Something else, so just traverse it
						Action act = traverseNode(nd);
						
						if (act == Action.Stop)
							return;
						
						continue;
					}
					
					if (cmdNode.getNumParameters() != 0)
						throw new AnalysisException(cmdNode.name + " node has parameters?", cmdNode);
					
					if (i == blk.values.size() - 1) {
						// Last item, so this has no effect really
						blk.values.remove(i);
						return;
					}
					
					// Move all the rest of the nodes to be a child of this Command
					Node nextNode = blk.values.get(i+1);
					
					Block newBlk = new Block(nextNode.getInputFile(), nextNode.getLineNum(), nextNode.getColNum());
					
					newBlk.values = new ArrayList<Node>(blk.values.subList(i+1, blk.values.size()));
					
					// Delete from current block
					blk.values.subList(i+1, blk.values.size()).clear();
					
					cmdNode.addParameter(newBlk);
					
					// Rename to the new style
					cmdNode.name = newCommandName;
					
					// Now we need to check our new children out and after that we are done
					traverseBlock(cmdNode.getParameter(0), cmdNode);
					return;
				}
				
			}
			finally {
				currentBlockPosition = prevBlockPositions.pop();
				currentBlock.pop();
				currentContext.pop();
			}
			
		}
	
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		new Traverser().traverseBlock(topLevelBlock, topLevelBlock);
	}
	
}
