package patterns;

import java.util.ArrayList;

import latex.node.*;

/**
 * Converts a single variable into math mode.  Examples:
 * 
 * \textit{x} => $x$
 * \textbf{v} => $\vect{v}$
 * \textbf{0} => $\vect{0}$
 * 
 */
public class SingleVariable extends MathPattern {
	
	private boolean isZeroVector(Command cmd) {
		
		if (!cmd.name.equalsIgnoreCase("textbf"))
			return false;
		
		Block blk = cmd.getParameter(0);
		
		if (blk.values.size() != 1)
			return false;
		
		String str = getLiteralString(blk.values.get(0));
		if (str == null || !"0".equals(str))
			return false;
		
		return true;
		
	}
	
	@Override
	public boolean processNodeList(ArrayList<Node> nodeList) {
		
		boolean madeChange = false;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			Node nd = nodeList.get(i);
			
			if (!isCommandName(nd, "textit") && !isCommandName(nd, "textbf"))
				continue;
			
			Command cmd = (Command)nd;
			
			if (!isZeroVector(cmd) && !hasExactlyOneLetterChild(cmd))
				continue;
			
			
			Node newNd = createVariable(cmd);
			
			
			Block newBlock = new Block(nd.getInputFile(),
					nd.getLineNum(), nd.getColNum());
			
			newBlock.values.add(newNd);
			
			MathMode newMath = new MathMode(MathMode.Style.DOLLAR, nd.getInputFile(),
					nd.getLineNum(), nd.getColNum());
			
			newMath.addEquation(newBlock);
			
			nodeList.remove(i);
			
			nodeList.add(i, newMath);
			
			madeChange = true;
			
		}
		
		
		return madeChange;
	}

}
