package patterns;

import java.util.ArrayList;

import latex.node.*;

/**
 * Converts cases like \textit{P}\textsuperscript{2} into a math
 * expression like $P^{2}$.  There are several kinds of sub-cases
 * as to where the variable, sub/super script is located, as well
 * as vectors instead of regular variables.
 * 
 * Various examples are listed in the methods below.
 * 
 */
public class SuperAndSubScripts extends MathPattern {
	
	/*
	 * \textit{P}\textsuperscript{2}
	 * \textit{U}\textsuperscript{${}-{}$1}
	 * \textbf{a}\textit{\textsubscript{n}}
	 * \textbf{v}\textit{\textsubscript{n} are a solution ...}
	 */
	private boolean extractOne(ArrayList<Node> nodeList, int currentPos) {
		
		if (!patternLength(nodeList, currentPos, 2))
			return false;
		
		Node nd = nodeList.get(currentPos);
		
		if (!isCommandName(nd, "textit") && !isCommandName(nd, "textbf"))
			return false;
			
		if (!hasExactlyOneLetterChild((Command)nd))
			return false;
		
		Node nextNd = nodeList.get(currentPos + 1);
		Command extractNd = null;
		
		if (isCommandName(nextNd, "textit")) {
			
			Command cmd = (Command) nextNd;
			
			ArrayList<Node> children = cmd.getParameter(0).values;
			
			//if (children.size() != 1)
			//	return false;
			
			extractNd = cmd;
			nextNd = children.get(0);
			
		}
		
		if (!isCommandName(nextNd, "textsubscript") && !isCommandName(nextNd, "textsuperscript"))
			return false;
			
		if (!hasExactlyOneLetterOrNumberChild((Command)nextNd))
			return false;
		
		
		// Build math mode with x^{string} or x_{string)
		Block newBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
	
		newBlock.values.add(createVariable((Command)nd));
		
		String expStr = "^";
		if (isCommandName(nextNd, "textsubscript"))
			expStr = "_";
		
		newBlock.values.add(new LiteralString(expStr, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum()));
		
		
		Block exponentBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2);
		
		exponentBlock.values.add(new LiteralString(getLetterOrNumberChild((Command)nextNd), nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2));
		
		newBlock.values.add(exponentBlock);
		
		
		MathMode newMath = new MathMode(MathMode.Style.DOLLAR, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
		
		newMath.addEquation(newBlock);
	
		if (extractNd == null || extractNd.getParameter(0).values.size() == 1) {
			
			// Remove the two nodes and replace with the new math mode
			nodeList.remove(currentPos);
			nodeList.remove(currentPos);
			
		}
		else {
			
			// Remove the first node and extract the sub/super from the parent node
			nodeList.remove(currentPos);
			extractNd.getParameter(0).values.remove(0);
			
		}
		
		nodeList.add(currentPos, newMath);
		
		return true;
		
	}
	
	/*
	 * \textit{a\textsubscript{n}}
	 */
	private boolean extractTwo(ArrayList<Node> nodeList, int currentPos) {
		
		Node nd = nodeList.get(currentPos);
		
		if (!isCommandName(nd, "textit"))
			return false;
		
		Command cmd = (Command) nd;
		
		ArrayList<Node> children = cmd.getParameter(0).values;
		
		if (children.size() != 2)
			return false;
		
		Node firstChild = children.get(0);
		
		String strBase = getLiteralString(firstChild);
		if (strBase == null || strBase.length() != 1)
			return false;
		
		char c = strBase.charAt(0);
		
		if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
			return false;
		
		Node secondChild = children.get(1);
		
		if (!isCommandName(secondChild, "textsubscript") && !isCommandName(secondChild, "textsuperscript"))
			return false;
			
		if (!hasExactlyOneLetterOrNumberChild((Command)secondChild))
			return false;
		
		if (isCommandName(secondChild, "textsubscript"))
			strBase += "_";
		else
			strBase += "^";
		
		
		// Build math mode with x^{string} or x_{string)
		Block newBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
		
		newBlock.values.add(new LiteralString(strBase, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum()));
		
		
		Block exponentBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2);
		
		exponentBlock.values.add(new LiteralString(getLetterOrNumberChild((Command)secondChild), nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2));
		
		newBlock.values.add(exponentBlock);
		
		
		MathMode newMath = new MathMode(MathMode.Style.DOLLAR, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
		
		newMath.addEquation(newBlock);

		// Remove the top level node and replace with math mode
		nodeList.remove(currentPos);
		
		nodeList.add(currentPos, newMath);
		
		return true;
		
	}
	
	/*
	 * \textit{... if and only if x}\textsubscript{1}
	 */
	private boolean extractThree(ArrayList<Node> nodeList, int currentPos) {
		
		if (!patternLength(nodeList, currentPos, 2))
			return false;
		
		Node nd = nodeList.get(currentPos);
		
		if (!isCommandName(nd, "textit"))
			return false;
		
		Command cmd = (Command)nd;
		
		Block blk = cmd.getParameter(0);
		
		Node childNd = blk.values.get(blk.values.size() - 1);
		
		String str = getLiteralString(childNd);
		if (str == null || str.length() < 2)
			return false;
		
		char c = str.charAt(str.length() - 1);
		if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
			return false;
		
		char whitespaceChar = str.charAt(str.length() - 2);
		if (whitespaceChar != ' ')
			return false;
		
		Node nextNd = nodeList.get(currentPos + 1);
		
		if (!isCommandName(nextNd, "textsubscript") && !isCommandName(nextNd, "textsuperscript"))
			return false;
			
		if (!hasExactlyOneLetterOrNumberChild((Command)nextNd))
			return false;
		
		
		// Remove char from childNd
		str = str.substring(0, str.length() - 1);
		((LiteralString)childNd).value = str;
		
		// Build math mode with x^{string} or x_{string)
		Block newBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
	
		String strBase = Character.toString(c);
		
		if (isCommandName(nextNd, "textsubscript"))
			strBase += "_";
		else
			strBase += "^";
		
		newBlock.values.add(new LiteralString(strBase, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum()));
		
		
		Block exponentBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2);
		
		exponentBlock.values.add(new LiteralString(getLetterOrNumberChild((Command)nextNd), nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2));
		
		newBlock.values.add(exponentBlock);
		
		
		MathMode newMath = new MathMode(MathMode.Style.DOLLAR, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
		
		newMath.addEquation(newBlock);
		
		// Remove the super/sub node and replace with the new math mode
		nodeList.remove(currentPos + 1);
		
		nodeList.add(currentPos, newMath);
		
		return true;
		
	}
	
	/*
	 * \textit{x\textsubscript{n} are a solution ...}
	 */
	private boolean extractFour(ArrayList<Node> nodeList, int currentPos) {
		
		Node nd = nodeList.get(currentPos);
		
		if (!isCommandName(nd, "textit"))
			return false;
		
		Command cmd = (Command)nd;
		
		Block blk = cmd.getParameter(0);
		
		if (!patternLength(blk.values, 0, 2))
			return false;
		
		Node firstChild = blk.values.get(0);
		
		String str = getLiteralString(firstChild);
		if (str == null || str.length() != 1)
			return false;
		
		char c = str.charAt(str.length() - 1);
		if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
			return false;
		
		Node secondChild = blk.values.get(1);
		
		if (!isCommandName(secondChild, "textsubscript") && !isCommandName(secondChild, "textsuperscript"))
			return false;
			
		if (!hasExactlyOneLetterOrNumberChild((Command)secondChild))
			return false;
		
		
		// Build math mode with x^{string} or x_{string)
		Block newBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
	
		String strBase = Character.toString(c);
		
		if (isCommandName(secondChild, "textsubscript"))
			strBase += "_";
		else
			strBase += "^";
		
		newBlock.values.add(new LiteralString(strBase, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum()));
		
		
		Block exponentBlock = new Block(nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2);
		
		exponentBlock.values.add(new LiteralString(getLetterOrNumberChild((Command)secondChild), nd.getInputFile(),
				nd.getLineNum(), nd.getColNum() + 2));
		
		newBlock.values.add(exponentBlock);
		
		
		MathMode newMath = new MathMode(MathMode.Style.DOLLAR, nd.getInputFile(),
				nd.getLineNum(), nd.getColNum());
		
		newMath.addEquation(newBlock);

		
		
		// Remove both children from the parent textit
		blk.values.remove(0);
		blk.values.remove(0);
		
		
		// Insert new math mode into top list before the textit
		nodeList.add(currentPos, newMath);
		
		return true;
		
	}
	
	@Override
	public boolean processNodeList(ArrayList<Node> nodeList) {
		
		boolean madeChange = false;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			if (extractOne(nodeList, i)) {
				madeChange = true;
				continue;
			}
			
			if (extractTwo(nodeList, i)) {
				madeChange = true;
				continue;
			}
			
			if (extractThree(nodeList, i)) {
				madeChange = true;
				continue;
			}
			
			if (extractFour(nodeList, i)) {
				madeChange = true;
				continue;
			}
			
		}
		
		
		return madeChange;
	}

}
