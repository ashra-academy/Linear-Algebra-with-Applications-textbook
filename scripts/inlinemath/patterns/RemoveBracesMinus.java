package patterns;

import java.util.ArrayList;

import latex.node.*;

/**
 * Removes the empty braces around the minus symbols.  When combined
 * with the other patterns this should move the minus into surrounding
 * math modes where it belongs.  Example:
 * 
 * ${}-{}$ => $-$
 * 
 */
public class RemoveBracesMinus extends MathPattern {
	
	private boolean isEmptyBlock(Node nd) {
		
		if (!(nd instanceof Block))
			return false;
		
		Block blk = (Block) nd;
		
		return blk.values.size() == 0;
		
	}
	
	private boolean processMathMode(ArrayList<Node> nodeList) {
		
		boolean madeChanges = false;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			if (!patternLength(nodeList, i, 3))
				continue;
			
			Node nd = nodeList.get(i);
			
			if (!isEmptyBlock(nd))
				continue;
			
			String opStr = getLiteralString(nodeList.get(i + 1));
			if (!"-".equals(opStr))
				continue;
			
			if (!isEmptyBlock(nodeList.get(i + 2)))
				continue;
			
			// Only delete the empty braces if there is something after it
			//if (i + 3 != nodeList.size())
				nodeList.remove(i + 2); 
			
			// Same with first set of braces
			//if (i != 0)
				nodeList.remove(i);
			
			madeChanges = true;
			
		}
		
		return madeChanges;
		
	}
	
	@Override
	public boolean processNodeList(ArrayList<Node> nodeList) {
		
		boolean madeChange = false;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			Node nd = nodeList.get(i);
			
			if (!isInlineMath(nd))
				continue;
			
			MathMode mm = (MathMode) nd;
			
			if (!processMathMode(mm.getEquations().get(0).get(0).values))
				continue;
			
			madeChange = true;
			
		}
		
		
		return madeChange;
	}

}
