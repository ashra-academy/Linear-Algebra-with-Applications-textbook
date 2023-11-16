package latex.analyzer;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Environment;
import latex.node.Node;

public class WrapLongTable extends Analyzer {
	
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
						
						if (env.name.equals("longtabu") || env.name.equals("longtable")) {
							
							// Remove the longtabu from this block
							blk.values.remove(i);
							
							Block wrappertableBlk = new Block(env.getInputFile(), env.getLineNum(), env.getColNum());
							
							wrappertableBlk.values.add(env);
							
							Environment wrappertable = new Environment(env.getInputFile(), env.getLineNum(), env.getColNum());
							
							// Pretend to be a normal table environment that contains a tabular style environment
							wrappertable.name = "table";
							wrappertable.content = wrappertableBlk;
							
							// Transfer properties to wrapper table
							wrappertable.setCentering(env.isCentering());
							wrappertable.addAllAttributes(env.getAdditionalAttributes());
							wrappertable.setReferenceNumber(env.getReferenceNumber());
							wrappertable.setCaption(env.getCaption());
							
							env.setCentering(false);
							env.setReferenceNumber(null);
							env.setCaption(null);
							
							// Insert a wrapper table to replace the longtabu in this block
							blk.values.add(i, wrappertable);
							
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
