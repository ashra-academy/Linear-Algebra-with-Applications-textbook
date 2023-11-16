package latex.analyzer;


import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Environment;
import latex.node.Node;

/**
 * Removes the adjustwidth environment, as that doesn't apply to HTML formats.
 * 
 * Alternatively, the writer could just be written to skip down into the contents
 * of this environment instead.
 * 
 */
public class RemoveAdjustWidth extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
	
		@Override
		protected void traverseBlock(Block blk, Node context) {
				
			currentContext.push(context);
			currentBlock.push(blk);
			prevBlockPositions.push(currentBlockPosition);
			
			try {
				
				for (int i = 0; i < blk.values.size(); i++) {
					
					currentBlockPosition = i;
					
					Node nd = blk.values.get(i);
					
					if (nd instanceof Environment) {
						Environment env = (Environment)nd;
						
						if (env.name.equals("adjustwidth")) {
							
							blk.values.remove(i);
							
							blk.values.addAll(i, env.content.values);
							
							i--;
							continue;
						}
						
					}
					
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
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		new Traverser().traverseBlock(topLevelBlock, topLevelBlock);
	}
	
}
