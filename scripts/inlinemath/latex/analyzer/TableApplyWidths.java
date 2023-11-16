package latex.analyzer;

import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Tabular.Cell;

/**
 * Sets cell widths and removes a top-level parbox in a cell that can be used to define
 * a cell width.
 */
public class TableApplyWidths extends Analyzer {
	
	private static class ApplyParbox extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitCell(Cell cell) {
			
			ListIterator<Node> ndIt = cell.getContent().values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Command))
					continue;
					
				Command cmd = (Command)nd;
				
				if (cmd.name.equals("parbox")) {
					
					// TODO: What if alignment doesn't match cell alignment?
					
					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Width of parbox must be a literal string", cmd);
					
					//String width = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					// TODO: In the future we might want to force a max width on the cell itself
					//cell.setWidth(width);
					
					Block content = cmd.getParameter(1);
					
					// Remove the parbox
					ndIt.remove();
					
					// Add in the content of parbox to the cell
					for (Node ndContent : content.values) {
						ndIt.add(ndContent);
					}
					
					for (int i = 0; i < content.values.size(); i++) {
						ndIt.previous();
					}
					
				}
				
			}
			
			return Action.TraverseChildren;
			
		}
		
		protected void doWork(Block documentBlock) {
			traverseBlock(documentBlock, documentBlock);
		}
		
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		
		Environment document = getDocumentEnvironment(topLevelBlock); 
		
		new ApplyParbox().doWork(document.content);
	}

}
