package latex.analyzer;

import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.Node;
import latex.node.Tabular;

/**
 * Searches for \dotfill commands and sets appropriate flags on a parent tabular cell.
 */
public class ApplyDotFillToParent extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			
			ListIterator<Node> ndIt = blk.values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Command))
					continue;
					
				Command cmd = (Command)nd;

				if (cmd.name.equals("dotfill")) {

					int pos = currentContext.size(); // One past the stack, where context will be
					Node parent = context;

					while (pos >= 0 && !(parent instanceof Tabular.Cell) &&
							!(parent instanceof Environment)) {
						pos--;
						parent = currentContext.get(pos);
					}

					if (pos == 0 || !(parent instanceof Tabular.Cell))
						throw new AnalysisException("\\dotfill is only supported inside tabular cells", cmd);

					Tabular.Cell cell = (Tabular.Cell)parent;

					cell.setDotFill(true);
					
					ndIt.remove();

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