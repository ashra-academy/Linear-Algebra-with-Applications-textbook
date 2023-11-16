package latex.analyzer;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Environment;
import latex.node.Tabular;
import latex.node.Tabular.Cell;

/**
 * Expands row span cells and verifies that the resulting 2-D array is square.
 */
public class TableExpandRowColSpans extends Analyzer {
	
	private static class ApplyRowSpans extends NodeTraverserIgnoreHTMLAsImage {
		
		protected void copyBorders(Cell originalCell, Cell newCell) {
			
			// This is needed for row spans where the main cell may be in one row
			// but the border is applied to a cell that will disappear
			if (originalCell.getTopBorder() > 0) {
				if (newCell.getTopBorder() == 0 || newCell.getTopBorder() == originalCell.getBottomBorder())
					newCell.setTopBorder(originalCell.getTopBorder());
				else
					throw new AnalysisException("Cell had top border but this is now lost", originalCell);
			}
			if (originalCell.getBottomBorder() > 0) {
				if (newCell.getBottomBorder() == 0 || newCell.getBottomBorder() == originalCell.getBottomBorder())
					newCell.setBottomBorder(originalCell.getBottomBorder());
				else
					throw new AnalysisException("Cell had bottom border but this is now lost", originalCell);
			}
			
			// TODO: Check for changes in background color?
			
		}
		
		@Override
		protected Action beforeVisitTabular(Tabular tab) {
			
			Cell[][] cellMatrix = tab.getCellMatrix();
			
			for (int y = 0; y < cellMatrix.length; y++) {
				
				for (int x = 0; x < cellMatrix[y].length; x++) {
					
					Cell cell = cellMatrix[y][x];
					
					int rowSpan = cell.getRowSpan();
					
					if (rowSpan > 1) {
						
						// TODO: This check does not work correctly.  The last row may be
						// a dummy row that gets removed later on, but it may be a real row
						// too.  So this check could be >= or just >.
						if (y + rowSpan > cellMatrix.length)
							throw new AnalysisException("multirow goes beyond end of table", cell);
						
						for (int i = 1; i < rowSpan; i++) {
							
							Cell replaceCell = cellMatrix[y + i][x];
							
							if (!replaceCell.isBlank())
								throw new AnalysisException("multirow covers a cell that is not blank or is part of another multi row/col", cell);
							
							copyBorders(cellMatrix[y + i][x], cell);
							tab.setSpanCell(x, y + i, cell);
							
						}
						
					}
					else if (rowSpan < -1) {
						
						if (y + rowSpan < 0)
							throw new AnalysisException("multirow goes beyond beginning of table", cell);
						
						int top = y + rowSpan + 1;
						
						// Move spanned row to top of the span
						copyBorders(cellMatrix[top][x], cell);
						cellMatrix[top][x] = cell;
						
						int numRows = (rowSpan * -1) - 1;
						
						// Don't consider the old position of the main cell
						for (int i = 1; i < numRows; i++) {
							
							Cell replaceCell = cellMatrix[top + i][x];
							
							if (!replaceCell.isBlank())
								throw new AnalysisException("multirow covers a cell that is not blank or is part of another multi row/col", cell);
							
							copyBorders(cellMatrix[top + i][x], cell);
							tab.setSpanCell(x, top + i, cell);
							
						}
						
						// Without the check above, just replace the old position with a SpanCell
						tab.setSpanCell(x, top + numRows, cell);
						
					}
					
				}
				
			}

			return Action.TraverseChildren;
			
		}
		
		protected void doWork(Block documentBlock) {
			traverseBlock(documentBlock, documentBlock);
		}
	}
	
	private static class VerifyMatrixSquareRemoveExtraRow extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitTabular(Tabular tab) {
			
			Cell[][] cellMatrix = tab.getCellMatrix();
			
			if (cellMatrix.length < 2)
				return Action.TraverseChildren;
			
			Cell[] lastRow = cellMatrix[cellMatrix.length - 1];
			if (lastRow.length == 1) {
				
				if (!lastRow[0].isBlank())
					throw new AnalysisException("Last row in tabular should be blank but it is not", lastRow[0]);
				
				tab.removeLastRow();
				
				cellMatrix = tab.getCellMatrix();
			}
			
			int numCols = cellMatrix[0].length;
			
			for (int y = 1; y < cellMatrix.length; y++) {
				
				if (numCols != cellMatrix[y].length)
					throw new AnalysisException("Row " + (y+1) + " of table has wrong number of columns", tab);
				
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
		
		ApplyRowSpans apply1 = new ApplyRowSpans();
		
		apply1.doWork(document.content);
		
		VerifyMatrixSquareRemoveExtraRow apply2 = new VerifyMatrixSquareRemoveExtraRow();
		
		apply2.doWork(document.content);
		
	}

}
