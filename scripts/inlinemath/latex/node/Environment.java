package latex.node;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;


public class Environment extends Node {
	
	public String name;
	public Block options;
	protected ArrayList<Block> parameters;
	protected Hashtable<String, Block> pgfParameters;
	public Block content;
	protected String referenceNumber;
	protected boolean centering;
	protected Command caption;
	protected ArrayList<Command> additionalAttributes;
	
	public Environment(File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
		this.options = null;
		this.parameters = null;
		this.pgfParameters = new Hashtable<String, Block>();
		this.content = null;
		this.referenceNumber = null;
		this.centering = false;
		this.additionalAttributes = new ArrayList<Command>();
		this.caption = null;
	}
	
	public void addParameter(Block parameterBlock) {
		if (parameters == null)
			parameters = new ArrayList<Block>();
		
		parameters.add(parameterBlock);
	}
	
	public Block getParameter(int index) {
		return parameters.get(index);
	}
	
	public ArrayList<Block> getParameters() {
		return parameters;
	}
	
	public void addPgfParameter(String key, Block value) {
		pgfParameters.put(key, value);
	}
	
	public void addPgfParameters(Map<String, Block> kvPairs) {
		pgfParameters.putAll(kvPairs);
	}
	
	public Block getPgfParameter(String key) {
		return pgfParameters.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public Hashtable<String, Block> getPgfParameters() {
		return (Hashtable<String, Block>) pgfParameters.clone();
	}
	
	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}
	
	public String getReferenceNumber() {
		return referenceNumber;
	}
	
	public void setCentering(boolean centering) {
		this.centering = centering;
	}
	
	public boolean isCentering() {
		return centering;
	}
	
	public void addAttribute(Command cmd) {
		additionalAttributes.add(cmd);
	}
	
	public void addAllAttributes(ArrayList<Command> attrs) {
		additionalAttributes.addAll(attrs);
	}
	
	public ArrayList<Command> getAdditionalAttributes() {
		return additionalAttributes;
	}
	
	public void setCaption(Command caption) {
		this.caption = caption;
	}
	
	public Command getCaption() {
		return caption;
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
		if (parameters != null) {
			for (Block paramBlock : parameters) {
				sb.append(paramBlock.toLatexString(true));
			}
		}
		if (centering) {
			sb.append("\\centering ");
		}
		for (Command cmd : this.additionalAttributes) {
			sb.append(cmd.toLatexString());
		}
		if (content != null) {
			sb.append(content.toLatexString(false));
		}
		sb.append("\\end{").append(name).append("}");
		
		return sb.toString();
	}

}
