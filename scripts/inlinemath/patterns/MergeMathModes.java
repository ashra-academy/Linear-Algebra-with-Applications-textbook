package patterns;

import java.util.ArrayList;

import latex.node.*;

/**
 * Merges any adjacent math modes, possibly separated by whitespace,
 * into a single math mode.  Examples:
 * 
 * $A$ $x$ => $A x$
 * $A$$x$ => $Ax$  (the double $ is not valid Latex but the other
 *                  patterns may have created this case and so this
 *                  pattern will fix these up)
 * 
 */
public class MergeMathModes extends MathPattern {
	
	private void concatMathModes(ArrayList<Node> nodeList, int start, int end) {
		
		MathMode destMathMode = (MathMode)nodeList.get(start);
		
		ArrayList<Node> destList = destMathMode.getEquations().get(0).get(0).values;
		
		for (int i = start + 1; i <= end; i++) {
			
			MathMode mm = (MathMode)nodeList.get(i);
			
			destList.addAll(mm.getEquations().get(0).get(0).values);
			
		}
		
	}
	
	@Override
	public boolean processNodeList(ArrayList<Node> nodeList) {
		
		boolean madeChange = false;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			Node nd = nodeList.get(i);
			
			if (!isInlineMath(nd))
				continue;
			
			int start = i, end = i + 1;
			
			while (end < nodeList.size()) {
				
				if (!isInlineMath(nodeList.get(end)))
					break;
				
				end++;
			}
			if ((start + 1) == end)
				continue;
			
			// Go back one to the last math mode
			end--;
			
			// Concat the contents of each math mode
			concatMathModes(nodeList, start, end);
			
			// Don't remove the now-merged first math mode
			start++;
			
			// Remove all of the merged math modes
			while (start <= end) {
				nodeList.remove(start);
				end--;
			}
			
			madeChange = true;
			
		}
		
		
		return madeChange;
	}

}
