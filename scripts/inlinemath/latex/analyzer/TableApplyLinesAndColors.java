package latex.analyzer;

import java.util.ArrayList;
import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.NodeTraverser.Action;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Tabular;
import latex.node.Tabular.Cell;

/**
 * Applies row lines (hline, etc) and cell colors to cells.
 */
public class TableApplyLinesAndColors extends Analyzer {
	
	private static class ApplyRowLines extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitCell(Cell cell) {
			
			ListIterator<Node> ndIt = cell.getContent().values.listIterator();
			
			Tabular tab = (Tabular)currentContext.get(currentContext.size() - 2);
			int rowNum = prevBlockPositions.get(prevBlockPositions.size() - 1);
			Tabular.Cell[] rowCells = ((Tabular.Row)currentContext.get(currentContext.size() - 1)).getCells();
			
			// TODO: This method doesn't handle all situations for row borders correctly when colspans
			//       are present. In Latex the borders seem to act independently of spans.  In HTML
			//       we apply borders to specific cells, meaning that span can either have a border
			//       or not.
			
			boolean bottomBorder = (rowNum != 0);
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Command))
					continue;
					
				Command cmd = (Command)nd;
				
				if (cmd.name.equals("hline")) {
					
					ndIt.remove();
					
					if (bottomBorder) {
						
						Cell[] prevRowCells = tab.getCellMatrix()[rowNum - 1];
						
						for (Tabular.Cell c : prevRowCells) {
							if (!(c instanceof Tabular.SpanCell))
								c.setBottomBorder(1);
						}
						
					}
					else {
						for (Tabular.Cell c : rowCells) {
							if (!(c instanceof Tabular.SpanCell))
								c.setTopBorder(1);
						}
					}
					
				}
				else if (cmd.name.equals("hhline")) {
					
					ndIt.remove();
					
					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Line types must be a literal string", cmd);
					
					String lineType = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					if (lineType.contains("*")) {
						lineType = expandAsteriskColumnType(cmd, lineType).toString();
					}
					
					if (bottomBorder) {
						
						Cell[] prevRowCells = tab.getCellMatrix()[rowNum - 1];
						
						// hhline doesn't take into account colspans, so this the total number of cols always
						if (lineType.length() != prevRowCells.length) {
							throw new AnalysisException("Line type argument isn't the same size as number of cells " + 
									lineType.length() + " != " + prevRowCells.length, cmd);
						}
						
						for (int x = 0; x < prevRowCells.length; x++) {
							
							char ch = lineType.charAt(x);
							
							Tabular.Cell applyCell = prevRowCells[x];
							
							if (ch == '-')
								applyCell.setBottomBorder(1);
							else if (ch == '=')
								applyCell.setBottomBorder(2);
							else if (ch != '~')
								throw new AnalysisException("Unhandled line type: " + ch, cmd);
							
							// TODO: We should check that there are no character changes between the start and end of the colspan
							// In other words, we only support one continuous line type
							if (applyCell.getColSpan() > 1) {
								
								for (int i = 1; i < applyCell.getColSpan(); i++) {
									if (lineType.charAt(x + i) != ch)
										throw new AnalysisException("hhline line type changes in the middle of a colspan.  We only support a single border type across the span.", cmd);
								}
								
								x += applyCell.getColSpan() - 1;
							}
							
						}
						
					}
					else {
						
						// hhline doesn't take into account colspans, so this the total number of cols always
						if (lineType.length() != rowCells.length) {
							throw new AnalysisException("Line type argument isn't the same size as number of cells no border " 
									+ lineType.length() + " != " + rowCells.length, cmd);
						}
						
						for (int x = 0; x < rowCells.length; x++) {
							
							char ch = lineType.charAt(x);
							
							Tabular.Cell applyCell = rowCells[x];
							
							if (ch == '-')
								applyCell.setTopBorder(1);
							else if (ch == '=')
								applyCell.setTopBorder(2);
							else if (ch != '~')
								throw new AnalysisException("Unhandled line type: " + ch, cmd);
							
							if (applyCell.getColSpan() > 1) {
								
								for (int i = 1; i < applyCell.getColSpan(); i++) {
									if (lineType.charAt(x + i) != ch)
										throw new AnalysisException("hhline line type changes in the middle of a colspan.  We only support a single border type across the span.", cmd);
								}
								
								x += applyCell.getColSpan() - 1;
							}
							
						}
					}
					
				}
				else if (cmd.name.equals("cline") || cmd.name.equals("tabucline")) {
					
					ndIt.remove();
					
					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Column range must be a literal string", cmd);
					
					String colRangeStr = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					int minCol, maxCol;
					
					// TODO: How to handle colspans
					if (colRangeStr.contains("-")) {
						String[] parts = colRangeStr.split("-");
						minCol = Integer.parseInt(parts[0]);
						maxCol = Integer.parseInt(parts[1]);
					}
					else {
						maxCol = minCol = Integer.parseInt(colRangeStr);
					}
					
					if (bottomBorder) {
						
						Cell[] prevRowCells = tab.getCellMatrix()[rowNum - 1];
						
						for (int j = (minCol-1); j < maxCol; j++) {
							Tabular.Cell c = prevRowCells[j];
							if (!(c instanceof Tabular.SpanCell))
								c.setBottomBorder(1);
						}
						
					}
					else {
						for (int j = (minCol-1); j < maxCol; j++) {
							Tabular.Cell c = rowCells[j];
							if (!(c instanceof Tabular.SpanCell))
								c.setTopBorder(1);
						}
					}
					
					continue;
				}
				
			}			
			
			return Action.TraverseChildren;
		}
		
		protected Action beforeVisitEnvironment(Environment env) 
		{
			
			if (!env.name.equals("financialstatementI") &&
				!env.name.equals("financialstatementII") &&
				!env.name.equals("financialstatementIII")) {
				
				return Action.TraverseChildren;
			}
			
			
			Command currentRow = null;
			ListIterator<Node> ndIt = env.content.values.listIterator();

			while (ndIt.hasNext()) {

				Node nd = ndIt.next();

				if (!(nd instanceof Command))
					continue;

				Command cmd = (Command) nd;

				if (cmd.name.equals("finstmtIrow") ||
					cmd.name.equals("finstmtIIrow") ||
					cmd.name.equals("finstmtIIIrow")) {
					
					currentRow = cmd;
					continue;
				}

				if (cmd.name.equals("hhline"))
				{
					
					// Ideally we would do error checking line this, but sometimes we
					// use raw table syntax so this check messes up.
					//if (currentRow == null)
					//	throw new AnalysisException("hhline found with no row to apply to", cmd);
					if (currentRow == null)
						continue;
					
					ndIt.remove();

					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Line types must be a literal string", cmd);

					String lineType = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					if (currentRow.name.equals("finstmtIrow") && lineType.length() != 4)
						throw new AnalysisException("Line spec for finstmtIrow must be of length 4", cmd);
					else if (currentRow.name.equals("finstmtIIrow") && lineType.length() != 7)
						throw new AnalysisException("Line spec for finstmtIrow must be of length 7", cmd);
					else if (currentRow.name.equals("finstmtIIIrow") && lineType.length() != 10)
						throw new AnalysisException("Line spec for finstmtIrow must be of length 10", cmd);
					
					Block childBlk = new Block(cmd.getInputFile(), cmd.getLineNum(), cmd.getColNum());
					childBlk.values.add(cmd);
					
					// Format is always: "~~" plus up to three copies of "--" (or == for double underline)
					// with an extra ~ in between each number column.  Ex: ~~== or ~~==~== or ~~==~==~==
					
					lineType = lineType.substring(2);
					
					if (lineType.startsWith("--")) {
						currentRow.addOptionsKeyValue("ul1", childBlk);
					}
					else if (lineType.startsWith("==")) {
						currentRow.addOptionsKeyValue("dl1", childBlk);
					}
					
					lineType = lineType.substring(2);
					
					if (lineType.length() < 3)
						continue;
					
					lineType = lineType.substring(1);
					
					if (lineType.startsWith("--")) {
						currentRow.addOptionsKeyValue("ul2", childBlk);
					}
					else if (lineType.startsWith("==")) {
						currentRow.addOptionsKeyValue("dl2", childBlk);
					}
					
					lineType = lineType.substring(2);
					
					if (lineType.length() < 3)
						continue;
					
					lineType = lineType.substring(1);
					
					if (lineType.startsWith("--")) {
						currentRow.addOptionsKeyValue("ul3", childBlk);
					}
					else if (lineType.startsWith("==")) {
						currentRow.addOptionsKeyValue("dl3", childBlk);
					}
					
				}
			}
			
			return Action.TraverseChildren;
		}
		
		/**
		 * Takes a string with * column type (like *{20}{-}) and converts it into
		 * the expanded form of the second parameter (like 20 -'s in a row).
		 * 
		 * @param cmd
		 * @param string
		 * @return The expanded column spec string
		 */
		private CharSequence expandAsteriskColumnType(Command cmd, String string) {
			
			ArrayList<Node> cmdValues = cmd.getParameter(0).values;
			
			StringBuffer retStr = new StringBuffer();
			
			for (int i = 0; i < cmdValues.size(); i++) {
				
				Node nd = cmdValues.get(i);
				
				if (nd instanceof LiteralString) {
					
					LiteralString ndStr = (LiteralString)nd;
					
					// No * expression, so we just append the string and continue
					if (!ndStr.value.contains("*")) {
						retStr.append(ndStr.value);
						continue;
					}
					
					if (!ndStr.value.endsWith("*"))
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);
					
					// Append everything up to the *
					if (ndStr.value.length() > 1)
						retStr.append(ndStr.value.substring(0, ndStr.value.length() - 1));
					
					// First parameter, the number of repetitions
					i++;
					if (i == cmdValues.size())
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);
					
					Node firstParam = cmdValues.get(i);
					
					if (!(firstParam instanceof Block))
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);
					
					Block firstParamBlk = (Block)firstParam;
					
					if (firstParamBlk.values.size() != 1 || !(firstParamBlk.values.get(0) instanceof LiteralString))
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);
					
					int numRepeat;
					try {
						numRepeat = Integer.parseInt(((LiteralString)firstParamBlk.values.get(0)).value);
					}
					catch (NumberFormatException ex) {
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);
					}
					
					// Second parameter, the contents to repeat
					i++;
					if (i == cmdValues.size())
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);

					Node secondParam = cmdValues.get(i);
					
					if (!(secondParam instanceof Block))
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);
					
					Block secondParamBlk = (Block)secondParam;
					
					if (secondParamBlk.values.size() != 1 || !(secondParamBlk.values.get(0) instanceof LiteralString))
						throw new AnalysisException("After *, need two brace expresssions: number of repetitions, and contents to repeat", cmd);
					
					String repeatContent = ((LiteralString)firstParamBlk.values.get(0)).value;
					
					for (int j = 0; j < numRepeat; j++)
						retStr.append(repeatContent);
					
				}
				else {
					throw new AnalysisException("Unexpected type of node", cmd);
				}
				
			}
			
			return retStr;
		}
		
		protected void doWork(Block documentBlock) {
			traverseBlock(documentBlock, documentBlock);
		}
		
	}
	
	private static class ApplyCellColors extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitCell(Cell cell) {
			
			ListIterator<Node> ndIt = cell.getContent().values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Command))
					continue;
					
				Command cmd = (Command)nd;
				
				if (cmd.name.equals("cellcolor")) {
					
					ndIt.remove();
					
					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Cell color class must be a literal string", cmd);
					
					String cellColor = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					cell.setBgColorClass(cellColor);
					
				}
				else if (cmd.name.equals("rowcolor")) {
					
					ndIt.remove();
					
					if (cmd.getParameter(0).values.size() < 1 || !(cmd.getParameter(0).values.get(0) instanceof LiteralString))
						throw new AnalysisException("Row color class must be a literal string", cmd);
					
					String rowColor = ((LiteralString)cmd.getParameter(0).values.get(0)).value;
					
					Tabular.Row row = (Tabular.Row)currentContext.get(currentContext.size() - 1);
					
					for (Tabular.Cell c: row.getCells()) {
						if (!(c instanceof Tabular.SpanCell))
							c.setBgColorClass(rowColor);
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
		
		ApplyRowLines apply1 = new ApplyRowLines();
		
		apply1.doWork(document.content);
		
		ApplyCellColors apply2 = new ApplyCellColors();
		
		apply2.doWork(document.content);
		
	}

}
