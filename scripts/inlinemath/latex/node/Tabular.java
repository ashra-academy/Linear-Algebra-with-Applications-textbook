package latex.node;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class Tabular extends Environment {
	
	public enum CellAlignment {
		NOT_SET,
		LEFT,
		CENTER,
		RIGHT
	}
	
	public static class Cell extends Node {
		
		private Block content;
		
		private int colSpan;
		private int rowSpan;
		
		private String width;
		
		private int leftBorder;
		private int rightBorder;
		private int topBorder;
		private int bottomBorder;
		
		private CellAlignment align;
		
		protected String referenceNumber;
		private String bgColorClass;
		
		private boolean hasDotFill;
		
		protected Cell(File inputFile, int lineNum, int colNum) {
			super(inputFile, lineNum, colNum);
			content = new Block(inputFile, lineNum, colNum);
			leftBorder = rightBorder = topBorder = bottomBorder = 0;
			colSpan = 1;
			rowSpan = 1;
			width = null;
			align = CellAlignment.NOT_SET;
			referenceNumber = null;
		}
		
		protected void appendContent(Node nd) {
			content.values.add(nd);
		}
		
		public Block getContent() {
			return content;
		}
		
		public void setColSpan(int colSpan) {
			this.colSpan = colSpan;
		}
		
		public void setLeftBorder(int leftBorder) {
			this.leftBorder = leftBorder;
		}
		
		public void setRightBorder(int rightBorder) {
			this.rightBorder = rightBorder;
		}
		
		public void setTopBorder(int topBorder) {
			this.topBorder = topBorder;
		}
		
		public void setBottomBorder(int bottomBorder) {
			this.bottomBorder = bottomBorder;
		}
		
		public int getLeftBorder() {
			return leftBorder;
		}
		
		public int getRightBorder() {
			return rightBorder;
		}
		
		public int getTopBorder() {
			return topBorder;
		}
		
		public int getBottomBorder() {
			return bottomBorder;
		}
		
		public void setAlign(CellAlignment align) {
			this.align = align;
		}
		
		public CellAlignment getAlign() {
			return align;
		}
		
		public int getColSpan() {
			return colSpan;
		}
		
		public void setRowSpan(int rowSpan) {
			this.rowSpan = rowSpan;
		}

		public void setBgColorClass(String bgColorClass) {
			this.bgColorClass = bgColorClass;
		}

		public int getRowSpan() {
			return rowSpan;
		}
		
		/**
		 * WARNING: This is only filled in for SOME cases.  p-type, X-type, and
		 * other ways to define width are not all parsed correctly into this parameter.
		 */
		public String getWidth() {
			return width;
		}
		
		public void setWidth(String width) {
			this.width = width;
		}
		
		public String getBgColorClass() {
			return bgColorClass;
		}
		
		public boolean hasDotFill() {
			return hasDotFill;
		}
		
		public void setDotFill(boolean hasDotFill) {
			this.hasDotFill = hasDotFill;
		}
		
		/**
		 * Returns true if the current cell is effectively blank, ignoring
		 * whitespace and commands that have no content like coloring.
		 */
		public boolean isBlank() {
			
			for (Node nd : content.values) {
				
				if (nd instanceof LiteralString) {
					
					LiteralString ndStr = (LiteralString)nd;
					
					if (!ndStr.isWhitespace())
						return false;
					
				}
				else if (nd instanceof Command) {
					
					Command ndCmd = (Command)nd;
					
					if (!ndCmd.name.equals("cellcolor"))
						return false;
					
				}
				else
					return false;
					
			}
			
			return true;
			
		}
		
		@Override
		public String toLatexString() {
			return content.toLatexString(false);
		}
		
	}

	/**
	 * Dummy cell to mark where a rowspan or colspan expands to.
	 */
	public static class SpanCell extends Cell {
		
		private Cell parentCell;
		private Cell originalCell;

		protected SpanCell(Cell originalCell, Cell parentCell) {
			super(parentCell.getInputFile(), parentCell.getLineNum(), parentCell.getColNum());
			this.originalCell = originalCell;
			this.parentCell = parentCell;
		}
		
		public Cell getOriginalCell() {
			return originalCell;
		}
		
		public Cell getParentCell() {
			return parentCell;
		}
		
		@Override
		public boolean isBlank() {
			return false;
		}
		
		@Override
		public void setColSpan(int colSpan) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setLeftBorder(int leftBorder) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setRightBorder(int rightBorder) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setTopBorder(int topBorder) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setBottomBorder(int bottomBorder) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setAlign(CellAlignment align) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setRowSpan(int rowSpan) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setBgColorClass(String bgColorClass) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setWidth(String width) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toLatexString() {
			return "<SpanCell>";
		}
		
	}
	
	/**
	 * Convenience Node to represent a row in the Tabular matrix.  DOES NOT actually
	 * contain any cells, but rather a pointer into the Tabular's internal matrix.
	 */
	public class Row extends Node {
		
		int rowNum;
		
		protected Row(File inputFile, int lineNum, int colNum, int rowNum) {
			super(inputFile, lineNum, colNum);
			this.rowNum = rowNum;
		}
		
		public Cell[] getCells() {
			return cellMatrix[rowNum];
		}
		
		@Override
		public String toLatexString() {
			return null;
		}
		
		public int getRowNum() {
			return rowNum;
		}
		
		public void insertSpanCells(Cell parentCell, int position, int numCells) {
			
			Cell[] newRow = new Cell[cellMatrix[rowNum].length + numCells];
			
			if (position > 0)
				System.arraycopy(cellMatrix[rowNum], 0, newRow, 0, position);
			
			for (int i = position; i < position + numCells; i++)
				newRow[i] = new SpanCell(null, parentCell);
			
			System.arraycopy(cellMatrix[rowNum], position, newRow, position + numCells, cellMatrix[rowNum].length - position);
			
			cellMatrix[rowNum] = newRow;
		}
		
	}
	
	/**
	 * 2D array of cells, where first coordinate is the row number and the second
	 * is the cell number in that row.
	 */
	private Cell[][] cellMatrix;
	
	/** A temporary variable used to build up a row while parsing raw latex. */
	private ArrayList<Cell> tempRow;

	/** A temporary variable used to build up all rows while parsing raw latex. */
	private ArrayList<ArrayList<Cell>> tempMatrix;
	
	private Block tabuSizeExtension;
	
	public Tabular(File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
		
		cellMatrix = null;
		tabuSizeExtension = null;
		
		tempRow = new ArrayList<Tabular.Cell>();
		tempMatrix = new ArrayList<ArrayList<Cell>>();
	}
	
	/**
	 * Start a new temp row (such as when parsing).
	 */
	public void addRow(File inputFile, int lineNum, int colNum) {
		tempMatrix.add(tempRow);
		tempRow = new ArrayList<Tabular.Cell>();
	}
	
	/**
	 * Start a new temp cell (such as when parsing).
	 */
	public void addCell(File inputFile, int lineNum, int colNum) {
		tempRow.add(new Cell(inputFile, lineNum, colNum));
	}
	
	/**
	 * Append some content to the current temp cell.
	 */
	public void appendToLastCell(Node nd) {
		
		if (tempRow.size() == 0) {
			Cell c = new Cell(nd.getInputFile(), nd.getLineNum(), nd.getColNum());
			tempRow.add(c);
		}
		
		tempRow.get(tempRow.size() - 1).appendContent(nd);
	}
	
	/**
	 * When done parsing, this will turn all of the temp rows/cells into the final
	 * cellMatrix 2-D array.
	 */
	public void flushTempMatrix() {
		
		tempMatrix.add(tempRow);
		tempRow = null;
		
		cellMatrix = new Cell[tempMatrix.size()][];
		
		for (int i = 0; i < tempMatrix.size(); i++) {
			
			ArrayList<Cell> r = tempMatrix.get(i);
			
			cellMatrix[i] = r.toArray(new Cell[0]);
		}
		
		tempMatrix = null;
		
	}
	
	
	/**
	 * Returns a ListIterator that will iterate through all of the rows
	 * in order.  NOTE that this creates Row objects on the fly, that
	 * point to particular rows in the internal cellMatrix.
	 */
	public ListIterator<Row> getRowIterator() {
		return new ListIterator<Tabular.Row>() {
			
			private int y = 0;
			
			@Override
			public boolean hasNext() {
				return y < cellMatrix.length;
			}
			
			@Override
			public int nextIndex() {
				if (y == cellMatrix.length)
					return y;
				return y+1;
			}
			
			@Override
			public Row next() {
				
				if (y == cellMatrix.length)
					throw new NoSuchElementException();
				
				Row r = new Row(cellMatrix[y][0].getInputFile(),
						cellMatrix[y][0].getLineNum(),
						cellMatrix[y][0].getColNum(),
						y);
				
				y++;
				
				return r;
			}
			
			@Override
			public boolean hasPrevious() {
				return y > 1;
			}
			
			@Override
			public int previousIndex() {
				return y - 1;
			}
			
			@Override
			public Row previous() {
				
				if (y == 0)
					throw new NoSuchElementException();
				
				y--;
				
				Row r = new Row(cellMatrix[y][0].getInputFile(),
						cellMatrix[y][0].getLineNum(),
						cellMatrix[y][0].getColNum(),
						y);
				
				return r;
			}
			
			@Override
			public void add(Row e) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void set(Row e) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	/**
	 * Returns an iterator that will iterate through all cells in order.
	 * NOTE: There will be no way to know which rows these cells belong to.
	 */
	public Iterator<Cell> getCellIterator() {
		return new Iterator<Tabular.Cell>() {
			
			private int y = 0;
			private int x = 0;
			
			@Override
			public boolean hasNext() {
				return y < cellMatrix.length && cellMatrix[y].length < x;
			}
			
			@Override
			public Cell next() {
				if (x == cellMatrix[y].length) {
					if (y == cellMatrix.length)
						throw new NoSuchElementException();
					y++;
					x = 0;
				}
				else
					x++;
				
				return cellMatrix[y][x];
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	public Cell getFirstCell() {
		return cellMatrix[0][0];
	}
	
	
	public Cell[][] getCellMatrix() {
		return cellMatrix;
	}
	
	/**
	 * Replace a cell at x,y in the cellMatrix with a SpanCell, which
	 * will point to parentCell.
	 */
	public void setSpanCell(int x, int y, Cell parentCell) {
		cellMatrix[y][x] = new SpanCell(cellMatrix[y][x], parentCell);
	}
	
	public int getNumRows() {
		return cellMatrix.length;
	}
	
	/**
	 * Removes the last row in the cellMatrix.
	 */
	public void removeLastRow() {
		
		Cell[][] newMatrix = new Cell[cellMatrix.length - 1][];
		
		System.arraycopy(cellMatrix, 0, newMatrix, 0, newMatrix.length);
		
		cellMatrix = newMatrix;
	}
	
	
	
	public void setLeftBorder(int columnNum) {
		
		for (Cell[] r : cellMatrix) {
			Cell c = r[columnNum];
			if (!(c instanceof SpanCell))
				c.setLeftBorder(1);
		}
	}
	
	public void setRightBorder(int columnNum) {
		for (Cell[] r : cellMatrix) {
			Cell c = r[columnNum];
			if (!(c instanceof SpanCell))
				c.setRightBorder(1);
		}
	}
	
	public void setColumnAlignment(int columnNum, CellAlignment align) {
		for (Cell[] r : cellMatrix) {
			Cell c = r[columnNum];
			if (!(c instanceof SpanCell) && c.getAlign() == CellAlignment.NOT_SET)
				c.setAlign(align);
		}
	}
	
	
	
	public void setTabuSizeExtension(Block tabuSizeExtension) {
		this.tabuSizeExtension = tabuSizeExtension;
	}
	
	
	@Override
	public String toLatexString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("\\begin{").append(name).append("}");
		if (options != null) {
			sb.append("[");
			sb.append(options.toLatexString(false));
			sb.append("]");
		}
		if (tabuSizeExtension != null) {
			sb.append(tabuSizeExtension.toLatexString(false));
		}
		if (parameters != null) {
			for (Block paramBlock : parameters) {
				sb.append(paramBlock.toLatexString(true));
			}
		}
		
		for (int y = 0; y < cellMatrix.length; y++) {
			
			Cell[] r = cellMatrix[y];
			
			for (int x = 0; x < r.length; x++) {
				
				Cell c = r[x];
				
				if (c != null) {
					
					String cellContents = c.getContent().toLatexString(false);
					
					// TODO: Exclude comments when deciding if this cell has a line break
					
					if (cellContents.contains("\\\\")) {
						// TODO: How to choose parbox width?
						cellContents = "\\parbox[c]{7cm}{" + cellContents + "}";
					}
					
					if (c.getColSpan() > 1) {
						sb.append("\\multicolumn{").append(c.getColSpan()).append("}{");
						if (c.getAlign() == CellAlignment.CENTER)
							sb.append("c}{");
						else if (c.getAlign() == CellAlignment.RIGHT)
							sb.append("r}{");
						else
							sb.append("l}{");
						sb.append(cellContents).append("}");
						// TODO: How to deal with right borders?
					}
					else {
						sb.append(cellContents);
					}

					if ((x+c.getColSpan()-1) != r.length - 1)
						sb.append(" & ");
				}
				
			}
			
			if (y < (cellMatrix.length - 1))
				sb.append(" \\\\\n");
		}
		
		sb.append("\\end{").append(name).append("}");
		
		return sb.toString();
	}
	
}
