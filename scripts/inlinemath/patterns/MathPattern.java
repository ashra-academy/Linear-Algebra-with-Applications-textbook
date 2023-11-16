package patterns;

import java.util.ArrayList;
import java.util.regex.Pattern;

import latex.node.*;

public abstract class MathPattern {
	
	public abstract boolean processNodeList(ArrayList<Node> nodeList);
	
	protected boolean patternLength(ArrayList<Node> nodeList, int currentPosition, int numNodes) {
		return (currentPosition + numNodes - 1) < nodeList.size();
	}
	
	protected boolean isCommandName(Node nd, String name) {
		
		if (!(nd instanceof Command))
			return false;
		
		Command cmd = (Command) nd;
		
		return cmd.name.equalsIgnoreCase(name);
		
	}
	
	protected String getExactlyOneChildString(Command cmd) {
		
		if (cmd.getNumParameters() != 1)
			return null;
		
		Block blk = cmd.getParameter(0);
		
		if (blk.values.size() != 1)
			return null;
		
		return getLiteralString(blk.values.get(0));
		
	}
	
	protected boolean hasExactlyOneLetterChild(Command cmd) {
		String str = getExactlyOneChildString(cmd);
		if (str == null)
			return false;
		
		if (str.length() != 1)
			return false;
		
		char c = str.charAt(0);
		
		return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
		
	}
	
	protected String getLetterOrNumberChild(Command cmd) {
		
		if (cmd.getNumParameters() == 1) {
			
			String str = getExactlyOneChildString(cmd);
			if (str == null)
				return null;
			
			return str;
			
		}
		else if (cmd.getNumParameters() == 2) {
			
			Block blk = cmd.getParameter(0);
			
			if (blk.values.size() != 2)
				return null;
			
			Node nd = blk.values.get(0);
			if (!(nd instanceof MathMode))
				return null;
			
			if (!nd.toLatexString().equals("${}-{}$"))
				return null;
			
			String str = getLiteralString(blk.values.get(1));
			if (str == null)
				return null;
			
			return "-" + str;
			
		}
		
		return null;
		
	}
	
	protected boolean hasExactlyOneLetterOrNumberChild(Command cmd) {
		
		String strToCheck = null;
		
		if (cmd.getNumParameters() == 1) {
			
			strToCheck = getExactlyOneChildString(cmd);
			
		}
		else if (cmd.getNumParameters() == 2) {
			
			Block blk = cmd.getParameter(0);
			
			if (blk.values.size() != 2)
				return false;
			
			Node nd = blk.values.get(0);
			if (!(nd instanceof MathMode))
				return false;
			
			if (!nd.toLatexString().equals("${}-{}$"))
				return false;
			
			strToCheck = getLiteralString(blk.values.get(1));
			
		}
		else {
			return false;
		}
		
		if (strToCheck == null)
			return false;
		
		if (strToCheck.length() == 1) {
			
			char c = strToCheck.charAt(0);
			
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
				return true;
			
		}
		
		return Pattern.matches("^[0-9]+$", strToCheck);
		
	}
	
	protected Node createVariable(Command cmd) {
		
		String str = getExactlyOneChildString(cmd);
		
		if (cmd.name.equals("textit")) {
			
			return new LiteralString(str, cmd.getInputFile(),
					cmd.getLineNum(), cmd.getColNum());
			
		}
		else if (cmd.name.equals("textbf")) {
			
			LiteralString strNode = new LiteralString(str, cmd.getInputFile(),
					cmd.getLineNum(), cmd.getColNum());
			
			Command cmdNode = new Command(cmd.getInputFile(),
					cmd.getLineNum(), cmd.getColNum());
			
			cmdNode.name = "vect";
			
			Block blk = new Block(cmd.getInputFile(),
					cmd.getLineNum(), cmd.getColNum());
			
			blk.values.add(strNode);
			
			cmdNode.addParameter(blk);
			
			return cmdNode;
			
		}
		else
			throw new RuntimeException("???");
		
	}
	
	
	
	
	
	
	
	
	
	protected boolean stringStartsWith(Node nd, String what, boolean skipWhitespace) {
		
		if (!(nd instanceof LiteralString))
			return false;
		
		LiteralString strNd = (LiteralString) nd;
		
		String str = strNd.value;
		
		if (str.length() == 0)
			return false;
		
		int pos = 0;
		
		if (skipWhitespace) {
			
			char c = str.charAt(pos);
			while (pos < str.length() - 1 && (c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
				pos++;
				c = str.charAt(pos);
			}
			
		}
		
		return str.substring(pos).toLowerCase().startsWith(what.toLowerCase());
		
	}
	
	protected boolean stringEndsWith(Node nd, String what, boolean skipWhitespace) {
		
		if (!(nd instanceof LiteralString))
			return false;
		
		LiteralString strNd = (LiteralString) nd;
		
		String str = strNd.value;
		
		if (str.length() == 0)
			return false;
		
		int pos = str.length() - 1;
		
		if (skipWhitespace) {
			
			char c = str.charAt(pos);
			while (pos > 0 && (c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
				pos--;
				c = str.charAt(pos);
			}
			
		}
		
		return str.substring(0, pos + 1).toLowerCase().endsWith(what.toLowerCase());
		
	}
	
	protected String getLiteralString(Node nd) {
		
		if (!(nd instanceof LiteralString))
			return null;
		
		LiteralString strNd = (LiteralString) nd;
		
		return strNd.value;
		
	}

	protected String getLastWord(Node nd, boolean skipWhitespace) {
		
		if (!(nd instanceof LiteralString))
			return null;
		
		LiteralString strNd = (LiteralString) nd;
		
		String str = strNd.value;
		
		if (str.length() == 0)
			return "";
		
		int pos = str.length() - 1;
		
		if (skipWhitespace) {
			
			char c = str.charAt(pos);
			while (pos > 0 && (c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
				pos--;
				c = str.charAt(pos);
			}
			
			if (pos == 0)
				return "";
			
		}
		
		str = str.substring(0, pos + 1);
		
		// Find next whitespace
		pos = str.length() - 1;
		
		char c = str.charAt(pos);
		while (pos > 0 && (c != ' ' && c != '\t' && c != '\n' && c != '\r')) {
			pos--;
			c = str.charAt(pos);
		}
		
		// Extract word
		if (pos == 0)
			return str;
		
		return str.substring(pos + 1);
		
	}
	
	protected boolean isInlineMath(Node nd) {
		
		if (!(nd instanceof MathMode))
			return false;
		
		MathMode mmNd = (MathMode) nd;
		
		return mmNd.getStyle() == MathMode.Style.DOLLAR;
	}

}
