package latex.node;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class Enumerate extends Node {
	
	public static class Item extends Node {
		
		Block label;
		Block content;
		
		public Item(File inputFile, int lineNum, int colNum, Block content) {
			super(inputFile, lineNum, colNum);
			this.content = content;
			this.label = null;
		}
		
		public Block getLabel() {
			return label;
		}
		
		public void setLabel(Block label) {
			this.label = label;
		}
		
		public Block getContent() {
			return content;
		}
		
		@Override
		public String toLatexString() {
			
			StringBuffer sb = new StringBuffer();
			
			sb.append("\\item");
			
			if (label != null)
				sb.append("[").append(label.toLatexString(false)).append("]");
			
			sb.append(content.toLatexString(false));
			
			return sb.toString();
		}
		
	}
	
	private String name;
	private Hashtable<String, Block> pgfParameters;
	private ArrayList<Item> items;
	
	public Enumerate(String name, File inputFile, int lineNum, int colNum, Hashtable<String, Block> pgfParameters) {
		
		super(inputFile, lineNum, colNum);
		
		this.name = name;
		this.items = new ArrayList<Enumerate.Item>();
		this.pgfParameters = pgfParameters;
	}
	
	public String getName() {
		return name;
	}
	
	public void addItem(Item item) {
		items.add(item);
	}
	
	public ArrayList<Item> getItems() {
		return items;
	}
	
	public String getStyle() {
		
		// TODO: What about the older [a.] as an option instead of a pgf param?
		
		if (!pgfParameters.containsKey("label"))
			return null;
		
		Block blk = pgfParameters.get("label");
		
		// Old style using only a string
		if (blk.values.size() == 1 && blk.values.get(0) instanceof LiteralString) {
			return ((LiteralString)blk.values.get(0)).value;
		}
		
		String blkStr = blk.toLatexString(false);
		
		if (blkStr.equals("\\alph*.") || blkStr.equals("(\\alph*)"))
			return "a.";
		else if (blkStr.equals("\\Alph*.") || blkStr.equals("(\\Alph*)"))
			return "a.";
		else if (blkStr.equals("\\arabic*.") || blkStr.equals("(\\arabic*)"))
			return "1.";
		else if (blkStr.equals("\\roman*.") || blkStr.equals("(\\roman*)"))
			return "i.";
		else if (blkStr.equals("\\Roman*.") || blkStr.equals("(\\Roman*)"))
			return "I.";
		else
			return blkStr; // custom style
	}
	
	public void addPgfParameter(String key, Block value) {
		pgfParameters.put(key, value);
	}
	
	public Block getPgfParameter(String key) {
		return pgfParameters.get(key);
	}
	
	
	@Override
	public String toLatexString() {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("\\begin{").append(name).append("}");
		
		String optionsStr = null;
		
		// Old-style only a string, ex: "a."
		if (pgfParameters.size() == 1 && pgfParameters.containsKey("label")) {
			
			Block blk = pgfParameters.get("label");
			
			if (blk.values.size() == 1 && blk.values.get(0) instanceof LiteralString) {
				
				optionsStr = ((LiteralString)blk.values.get(0)).value;
				
			}
			
		}
		
		if (optionsStr != null) {
			sb.append("[").append(optionsStr).append("]");
		}
		else if (pgfParameters.size() > 0) {
			
			sb.append("[");
			
			for (Enumeration<String> en = pgfParameters.keys(); en.hasMoreElements(); ) {
				
				String key = en.nextElement();
				Block value = pgfParameters.get(key);
				
				sb.append(key).append("=").append(value.toLatexString(true)).append(",");
				
			}
			
			sb.deleteCharAt(sb.length() - 1);
			sb.append("]");
			
		}
		
		for (Item item : items) {
			sb.append(item.toLatexString());
		}
		
		sb.append("\\end{").append(name).append("}");
		
		return sb.toString();
	}
	
}
