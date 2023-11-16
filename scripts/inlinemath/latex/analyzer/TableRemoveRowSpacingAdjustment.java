package latex.analyzer;

import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Tabular.Cell;

/**
 * Removes row spacing modifiers \\[xxx] on table rows
 */
public class TableRemoveRowSpacingAdjustment extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitCell(Cell cell) {
			
			// Only consider the first cell in a row
			if (currentBlockPosition != 0)
				return super.beforeVisitCell(cell);
			
			ListIterator<Node> ndIt = cell.getContent().values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof LiteralString))
					continue;
				
				LiteralString ndStr = (LiteralString)nd;
				
				ndStr.value = ndStr.value.replaceFirst("^\\[-?[0-9]\\.[0-9]+[a-z]+\\]", "");
				
			}
			
			return super.beforeVisitCell(cell);
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			super.traverseBlock(blk, context);
		}
		
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		new Traverser().traverseBlock(topLevelBlock, topLevelBlock);
	}

}
