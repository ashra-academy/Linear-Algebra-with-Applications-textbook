package patterns;

import java.util.ArrayList;
import java.util.HashSet;

import latex.node.*;

/**
 * Merges math modes that are separated by binary operators into
 * a single math mode.  Examples:
 * 
 * $x$ = $1$ + $y$ => $x = 1 + y$
 * 
 */
public class BinaryOperators extends MathPattern {
	
	private static HashSet<String> binaryOps;
	{
		binaryOps = new HashSet<String>();
		binaryOps.add("+");
		binaryOps.add("=");
		binaryOps.add(">");
		binaryOps.add("<");
	}
	
	private void concatMathModesAndBinOps(ArrayList<Node> nodeList, int start, int end) {
		
		MathMode destMathMode = (MathMode)nodeList.get(start);
		
		ArrayList<Node> destList = destMathMode.getEquations().get(0).get(0).values;
		
		for (int i = start + 1; i <= end; i++) {
			
			Node nd = nodeList.get(i);
			
			if (nd instanceof MathMode) {
				
				MathMode mm = (MathMode)nodeList.get(i);
				
				destList.addAll(mm.getEquations().get(0).get(0).values);
			}
			else if (nd instanceof LiteralString) {
				
				destList.add(nd);
				
			}
			else
				throw new RuntimeException("???");
			
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
				
				if (isInlineMath(nodeList.get(end))) {
					end++;
					continue;
				}
				
				String str = getLiteralString(nodeList.get(end));
				if (str == null)
					break;
				
				if (!str.trim().equals("") && !binaryOps.contains(str.trim()))
					break;
				
				end++;
			}
			if ((start + 1) == end)
				continue;
			
			// Go back one to the last item that we are interested in
			end--;
			
			// Only want to merge groups that end in a math mode,
			// so back up to the last math mode
			while (end > start) {
				
				if (isInlineMath(nodeList.get(end)))
					break;
				
				end--;
			}
			if (start == end)
				continue;
			
			// Concat the contents of each math mode
			concatMathModesAndBinOps(nodeList, start, end);
			
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
