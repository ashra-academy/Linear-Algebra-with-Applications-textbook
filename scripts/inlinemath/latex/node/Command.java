package latex.node;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;


public class Command extends Node {

	public String name;
	public Block options;
	private Hashtable<String, Block> optionsHash;
	private ArrayList<Block> parameters;
	private String equalsParameter;

	public Command(File inputFile, int lineNum, int colNum) {
		super(inputFile, lineNum, colNum);
		this.options = null;
		this.parameters = new ArrayList<Block>();
		this.optionsHash = new Hashtable<String, Block>();
	}

	@Override
	public String toLatexString() {
		StringBuffer sb = new StringBuffer();

		sb.append("\\").append(name);

		if (equalsParameter != null) {
			sb.append("=").append(equalsParameter);
		}

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

		return sb.toString();
	}

	public void addParameter(Block blk) {
		if (equalsParameter != null)
			throw new RuntimeException("Cannot add regular parameter when equals parameter exists");
		parameters.add(blk);
	}

	public void appendToFirstParameterBlock(Node nd) {
		parameters.get(0).values.add(nd);
	}

	public Block getParameter(int index) {
		return parameters.get(index);
	}

	public int getNumParameters() {
		return parameters.size();
	}

	public void setEqualsParameter(String value) {
		if (parameters.size() > 0 || options != null)
			throw new RuntimeException("Cannot set equals parameter when regular parameters or options exist");
		equalsParameter = value;
	}

	public void addOptionsKeyValues(Map<String, Block> kvPairs) {
		optionsHash.putAll(kvPairs);
	}

	public void addOptionsKeyValue(String key, Block value) {
		optionsHash.put(key, value);
	}
	
	public Block getOptionValue(String key) {
		return optionsHash.get(key);
	}

}
