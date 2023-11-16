package latex.analyzer;

import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Tabular;
import latex.node.Tabular.Cell;
import latex.node.Tabular.CellAlignment;

/**
 * Extracts multicolumn and multirow commands into fields on the Cell objects.
 * multicolumn commands will expand out to rowspans (needed for future steps), but
 * multirow commands will not affect the 2-D array yet (See ExpandRowColSpans for later
 * steps).
 */
public class TableExtractSpans extends Analyzer {
	
	private static class ExtractSpans extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitCell(Cell cell) {
			
			boolean hasRowSpan = false;
			boolean hasColSpan = false;
			
			ListIterator<Node> ndIt = cell.getContent().values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Command))
					continue;
					
				Command cmd = (Command)nd;
				
				if (cmd.name.equals("multicolumn")) {
					
					if (hasColSpan)
						throw new AnalysisException("Two multicolumns found in table cell", cell);
					
					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Num of columns must be a literal string", cmd);
					
					String colSpanStr = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					int colSpan = Integer.parseInt(colSpanStr);
					
					if (cmd.getParameter(1).values.size() < 1 || !(cmd.getParameter(1).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Column spec must be a literal string", cmd);
					
					String colSpec = ((LiteralString)cmd.getParameter(1).values.get(0)).value;
					
					Block content = cmd.getParameter(2);
					
					// Handle colspan
					cell.setColSpan(colSpan);
					
					// Handle colspec
					char c = colSpec.charAt(0);
					if (c == '|') {
						cell.setLeftBorder(1);
						c = colSpec.charAt(1);
					}
					
					if (c == 'l')
						cell.setAlign(CellAlignment.LEFT);
					else if (c == 'c')
						cell.setAlign(CellAlignment.CENTER);
					else if (c == 'r')
						cell.setAlign(CellAlignment.RIGHT);
					
					if (colSpec.length() > 1 && colSpec.charAt(colSpec.length() - 1) == '|')
						cell.setRightBorder(1);
					
					
					// Remove the multicolumn
					ndIt.remove();
					
					// Add in the content of multicolumn to the cell
					for (Node ndContent : content.values) {
						ndIt.add(ndContent);
					}
					
					for (int i = 0; i < content.values.size(); i++) {
						ndIt.previous();
					}
					
					// In the parent row, insert cell spans to fill out the matrix
					
					Tabular.Row row = (Tabular.Row) currentContext.peek();
					
					row.insertSpanCells(cell, currentBlockPosition + 1, colSpan - 1);
					
					hasColSpan = true;
				}
				else if (cmd.name.equals("multirow")) {
					
					if (hasRowSpan)
						throw new AnalysisException("Two multirows found in table cell", cell);
					
					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Num of rows must be a literal string", cmd);
					
					String rowSpanStr = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					int rowSpan = Integer.parseInt(rowSpanStr);
					
					// TODO: Handle widths that are computed based on \linewidth or a fraction thereof
					
					if (cmd.getParameter(1).values.size() == 1 && cmd.getParameter(1).values.get(0) instanceof LiteralString)
					{
						String widthStr = ((LiteralString)cmd.getParameter(1).values.get(0)).value;
						
						if (!widthStr.equals("*")) {
							cell.setWidth(widthStr);
						}
					}
					
					Block content = cmd.getParameter(2);
					
					// Handle rowspan
					cell.setRowSpan(rowSpan);

					// Remove the multirow
					ndIt.remove();
					
					// Add in the content of multirow to the cell
					for (Node ndContent : content.values) {
						ndIt.add(ndContent);
					}
					
					for (int i = 0; i < content.values.size(); i++) {
						ndIt.previous();
					}
					
					// Adding SpanCells to the matrix will be handled in a later step
					
					hasRowSpan = true;
					
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
		
		new ExtractSpans().doWork(document.content);
		
	}

}
