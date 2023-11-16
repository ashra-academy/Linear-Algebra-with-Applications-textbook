package patterns;

import java.util.ArrayList;

import latex.node.*;

/**
 * Converts something that looks like a list of math items into a single
 * math mode.  This covers things like points and sets and some vectors.
 * Examples:
 * 
 * ($x$, $y$) => $(x, y)$
 * \{$1$, $2$, \dots, $n$\} => $\{1, 2, \dots, n\}$
 * [$a$ $b$ $c$] => $[a b c]$
 * 
 */
public class ListOfItems extends MathPattern {
	
	private boolean isListOfMath(ArrayList<Node> nodeList, int start, int end) {
		
		// Empty list
		if (start >= end)
			return false;
		
		for (int i = start; i <= end; i++) {
			
			Node nd = nodeList.get(i);
			
			if (isInlineMath(nd))
				continue;
			
			if (isCommandName(nd, "dots"))
				continue;
			
			String strValue = getLiteralString(nd);
			if (strValue == null)
				return false;
			
			if (!strValue.trim().equals(",") && strValue.trim().length() != 0)
				return false;
			
		}
		
		return true;
		
	}
	
	private MathMode mergeIntoMath(ArrayList<Node> nodeList, int start, int end) {
		
		Node firstNd = nodeList.get(start);
		
		Block newBlock = new Block(firstNd.getInputFile(),
				firstNd.getLineNum(), firstNd.getColNum());
		
		for (int i = start; i <= end; i++) {
			
			Node nd = nodeList.get(i);
			
			if (isInlineMath(nd)) {
				newBlock.values.addAll(((MathMode)nd).getEquations().get(0).get(0).values);
			}
			else if (isCommandName(nd, "dots")) {
				newBlock.values.add(nd);
			}
			else if (nd instanceof LiteralString) {
				newBlock.values.add(nd);
			}
			else
				throw new RuntimeException("???");
			
		}
		
		
		MathMode newMath = new MathMode(MathMode.Style.DOLLAR, firstNd.getInputFile(),
				firstNd.getLineNum(), firstNd.getColNum());
		
		newMath.addEquation(newBlock);
		
		return newMath;
		
	}
	

	@Override
	public boolean processNodeList(ArrayList<Node> nodeList) {
		
		boolean madeChange = false;
		String startTagType = null, endTagType = null;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			Node nd = nodeList.get(i);
			
			if (stringEndsWith(nd, "(", true)) {
				startTagType = "(";
				endTagType = ")";
			}
			else if (stringEndsWith(nd, "[", true)) {
				startTagType = "[";
				endTagType = "]";
			}
			else if (stringEndsWith(nd, "\\{", true)) {
				startTagType = "\\{";
				endTagType = "\\}";
			}
			else {
				continue;
			}
			
			int start = i, end = i + 1;
			
			while (end < nodeList.size()) {
				
				if (stringStartsWith(nodeList.get(end), endTagType, true))
					break;
				
				end++;
			}
			
			if (end == nodeList.size())
				continue;
			
			if (!isListOfMath(nodeList, start + 1, end - 1))
				continue;
			
			
			
			MathMode newMath = mergeIntoMath(nodeList, start + 1, end - 1);
			
			// Remove parens from strings around the new node
			
			LiteralString preNode = (LiteralString)nodeList.get(start);
			LiteralString postNode = (LiteralString)nodeList.get(end);
			
			int j = preNode.value.length() - 1;
			while (preNode.value.charAt(j) != startTagType.charAt(startTagType.length() - 1))
				j--;
			// Skip backslash too
			j -= (startTagType.length() - 1);
			String keepPreStr = preNode.value.substring(j);
			preNode.value = preNode.value.substring(0, j);
			
			j = 0;
			while (postNode.value.charAt(j) != endTagType.charAt(0))
				j++;
			// Skip backslash too
			j += (endTagType.length() - 1);
			String keepPostStr = postNode.value.substring(0, j + 1);
			postNode.value = postNode.value.substring(j + 1);
			
			// Add parens into newMath
			newMath.getEquations().get(0).get(0).values.add(0,
					new LiteralString(keepPreStr, newMath.getInputFile(), newMath.getLineNum(), newMath.getColNum()));
			newMath.getEquations().get(0).get(0).values.add(
					new LiteralString(keepPostStr, newMath.getInputFile(), newMath.getLineNum(), newMath.getColNum()));
			
			if (preNode.value.length() != 0)
				start++;
			
			if (postNode.value.length() != 0)
				end--;
			
			// Splice list
			while (start <= end) {
				nodeList.remove(start);
				end--;
			}
			
			nodeList.add(start, newMath);
			
			madeChange = true;
			
		}
		
		
		return madeChange;
	}

}
