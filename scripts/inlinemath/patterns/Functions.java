package patterns;

import java.util.ArrayList;

import latex.node.*;

/**
 * Identifies functions and merges them into a single math mode.
 * Examples:
 * 
 * $f$($x$) => $f(x)$
 * 
 */
public class Functions extends MathPattern {
	
	@Override
	public boolean processNodeList(ArrayList<Node> nodeList) {
		
		boolean madeChange = false;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			if (!patternLength(nodeList, i, 4))
				continue;
			
			Node nd = nodeList.get(i);
			
			if (!isInlineMath(nd))
				continue;
			
			Node secondNd = nodeList.get(i + 1);
			
			String openParen = getLiteralString(secondNd);
			if (openParen == null || !openParen.trim().equals("("))
				continue;
			
			Node thirdNd = nodeList.get(i + 2);
			
			if (!isInlineMath(thirdNd))
				continue;
			
			Node lastNode = nodeList.get(i + 3);
			
			if (!stringStartsWith(lastNode, ")", true))
				continue;
			
			
			// Extract ) from last node
			LiteralString postNode = (LiteralString)lastNode;
			
			int j = 0;
			while (postNode.value.charAt(j) != ')')
				j++;
			String keepPostStr = postNode.value.substring(0, j + 1);
			postNode.value = postNode.value.substring(j + 1);

			
			// Merge first three nodes
			
			MathMode firstMM = (MathMode) nd;
			MathMode thirdMM = (MathMode) thirdNd;
			
			ArrayList<Node> mathList = firstMM.getEquations().get(0).get(0).values;
			
			mathList.add(secondNd);
			mathList.addAll(thirdMM.getEquations().get(0).get(0).values);
			mathList.add(new LiteralString(keepPostStr,
					postNode.getInputFile(), postNode.getLineNum(), postNode.getColNum()));

			// Remove second and third nodes.  Fourth was the string that we removed
			nodeList.remove(i + 1);
			nodeList.remove(i + 1);
			
			madeChange = true;
			
		}
		
		
		return madeChange;
	}

}
