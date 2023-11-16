package latex.analyzer;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Tabular;
import latex.node.Tabular.CellAlignment;

/**
 * Applies some table-wide parameters to its cells, like column alignment.
 */
public class ApplyTableParameters extends Analyzer {
	
	private static class ApplyColumnSpec extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitTabular(Tabular tab) {

			Block columnSpecBlock;
			if (tab.name.equals("tabular") || tab.name.equals("longtable") || tab.name.equals("tabu") || tab.name.equals("longtabu"))
				columnSpecBlock = tab.getParameter(0);
			else if (tab.name.equals("tabularx"))
				columnSpecBlock = tab.getParameter(1);
			else
				throw new AnalysisException("Unhandled tablular type: " + tab.name, tab);
			
			if (columnSpecBlock.values.size() < 1 || !(columnSpecBlock.values.get(0) instanceof LiteralString))
				throw new AnalysisException("Column spec must start with a literal string", tab);
			
			int blockPos = 0;
			
			String columnSpec = ((LiteralString)columnSpecBlock.values.get(blockPos)).value;
			
			int columnPos = 0;
			
			for (int charPos = 0; charPos < columnSpec.length(); charPos++) {
				
				char c = columnSpec.charAt(charPos);
				
				if (c == ' ') {
					continue;
				}
				else if (c == '|') {
					//if (charPos == 0) {
					if (charPos < columnSpec.length() - 1) {
						tab.setLeftBorder(columnPos);
					}
					else {
						tab.setRightBorder(columnPos - 1);
					}
				}
				else if (c == 'l' || c == 'c' || c == 'r' | c == 'p' || c == 'X') {
					
					CellAlignment align = CellAlignment.NOT_SET;
					if (c == 'l' || c == 'p' || c == 'X')
						align = CellAlignment.LEFT;
					else if (c == 'c')
						align = CellAlignment.CENTER;
					else if (c == 'r')
						align = CellAlignment.RIGHT;
					
					// tabu X column type extensions that affect alignment, like "X[1,c]"
					if (c == 'X' && charPos < (columnSpec.length() - 1) && columnSpec.charAt(charPos + 1) == '[') {
						
						int beginIndex = charPos + 2;
						int endIndex = columnSpec.indexOf(']', beginIndex);
						
						if (endIndex == -1)
							throw new AnalysisException("Found X column type with [] option, but missing closing ]", tab);
						
						String optionStr = columnSpec.substring(beginIndex, endIndex);
						
						String[] parts = optionStr.split(",");
						
						if (parts.length > 1) {
							
							if (parts[1].equals("l") || parts[1].equals("p"))
								align = CellAlignment.LEFT;
							else if (parts[1].equals("c"))
								align = CellAlignment.CENTER;
							else if (parts[1].equals("r"))
								align = CellAlignment.RIGHT;
							
						}
						
						//skip to ]
						charPos = endIndex;
					}

					try {
						// One issue is that the columns aren't filled in for every row and so this
						// may throw an exception
						tab.setColumnAlignment(columnPos, align);
					}
					catch (Exception ex) {
						throw new AnalysisException("Error while setting column alignment", ex, tab);
					}
					
					columnPos++;
					
				}
				else if (c == '@') {
					
					// TODO: For now we ignore @-expressions
					// See: http://en.wikibooks.org/wiki/LaTeX/Tables#.40-expressions
					
				}
				else if (c == '>') {
					
					// TODO: For now we ignore >-expressions
					
				}
				else
					throw new AnalysisException("Unhandled column type: " + c, tab);
				
				
				// Skip expressions after some column types
				if (c == 'p' || c == '@' || c == '>') {
					
					// TODO: Apply p-type widths to the cells in the table
					
					if (charPos < (columnSpec.length() - 1))
						throw new AnalysisException(c + " type must have a block just after it", tab);
					
					blockPos++;
					
					if (blockPos == columnSpecBlock.values.size() || !(columnSpecBlock.values.get(blockPos) instanceof Block))
						throw new AnalysisException(c + " type must have a block just after it", tab);
					
					blockPos++;
					
					// This column may just be the last thing in the list
					if (blockPos == columnSpecBlock.values.size())
						break;
					
					if (!(columnSpecBlock.values.get(blockPos) instanceof LiteralString))
						throw new AnalysisException("Column spec has unknown node type in it: " + columnSpecBlock.values.get(blockPos).getClass().getName(), tab);
					
					// Move on to the next set of characters and keep parsing like usual
					columnSpec = ((LiteralString)columnSpecBlock.values.get(blockPos)).value;
					charPos = -1; // -1 since for loop will add +1 to give 0
					
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
		
		ApplyColumnSpec apply = new ApplyColumnSpec();
		
		apply.doWork(document.content);
		
	}

}
