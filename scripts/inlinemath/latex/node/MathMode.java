package latex.node;

import java.io.File;
import java.util.ArrayList;


public class MathMode extends Node {
	
	public static enum Style {
		/** Math (inline) */
		DOLLAR,
		/** Display Math (own block) */
		DOLLAR_DOLLAR,
		/** Math (inline) */
		SLASH_PAREN,
		/** Display Math (own block) */
		SLASH_SQUARE,
		/** Gather, equation environments (list of equations) */
		GATHER,
		/** Align environment (list of equations with & to define alignment across lines) */
		ALIGN
	}

	private Style style;
	
	private ArrayList<ArrayList<Block>> equations;
	
	private ArrayList<String> referenceNumbers;
	
	private String name;

	public MathMode(Style style, File inputFile, int lineNum, int colNum) {
		this("", style, inputFile, lineNum, colNum);
	}
	
	public MathMode(String name, Style style, File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);

		this.name = name;
		this.style = style;

		this.equations = new ArrayList<ArrayList<Block>>();
		addEquationRow();
		
		this.referenceNumbers = new ArrayList<String>();
		
	}
	
	@Override
	public String toLatexString() {
		if (style == Style.DOLLAR)
			return "$" + equations.get(0).get(0).toLatexString(false) + "$";
		else if (style == Style.DOLLAR_DOLLAR)
			return "$$" + equations.get(0).get(0).toLatexString(false) + "$$";
		else if (style == Style.SLASH_PAREN)
			return "\\(" + equations.get(0).get(0).toLatexString(false) + "\\)";
		else if (style == Style.SLASH_SQUARE)
			return "\\[" + equations.get(0).get(0).toLatexString(false) + "\\]";
		else if (style == Style.GATHER) {
			
			StringBuffer sb = new StringBuffer();
			
			sb.append("\\begin{").append(name).append("}");
			
			for (Block eqn : equations.get(0)) {
				sb.append(eqn.toLatexString(false)).append("\\\\");
			}
			
			if (equations.size() > 0)
				sb.delete(sb.length() - 2, sb.length());
			
			sb.append("\\end{").append(name).append("}");
			
			return sb.toString();
			
		}
		else if (style == Style.ALIGN) {
			
			StringBuffer sb = new StringBuffer();
			
			sb.append("\\begin{").append(name).append("}");
			
			for (int row = 0; row < equations.size(); row++) {
				
				ArrayList<Block> rowData = equations.get(row);
				
				for (int col = 0; col < rowData.size(); col++) {
				
					sb.append(rowData.get(col).toLatexString(false));
					
					if (col+1 != rowData.size()) {
						sb.append("&");
					}
					
				}
				
				sb.append("\\\\");
			}
			
			if (equations.size() > 0)
				sb.delete(sb.length() - 2, sb.length());
			
			sb.append("\\end{").append(name).append("}");
			
			return sb.toString();
			
		}
		else
			throw new RuntimeException("Unhandled style: " + style);
	}
	
	public String getName() {
		return name;
	}
	
	public Style getStyle() {
		return style;
	}
	
	public void setStyle(Style style) {
		this.style = style;
	}
	
	public void addEquation(Block eqnBlk) {
		equations.get(equations.size() - 1).add(eqnBlk);
	}
	
	public void addEquationRow() {
		equations.add(new ArrayList<Block>());
	}
	
	public ArrayList<ArrayList<Block>> getEquations() {
		return equations;
	}
	
	public ArrayList<String> getReferenceNumbers() {
		return referenceNumbers;
	}
	
}
