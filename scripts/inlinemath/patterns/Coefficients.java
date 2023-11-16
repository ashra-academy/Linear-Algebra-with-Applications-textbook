package patterns;

import java.util.ArrayList;
import java.util.regex.Pattern;

import latex.node.*;

/**
 * Moves coefficients from outside of math mode into the next
 * math mode.  Examples:
 * 
 * 3$x + 2$ => $3x + 2$
 * 2$\vect{v}$ => $2\vect{v}$
 * 
 */
public class Coefficients extends MathPattern {
	
	Pattern NUMBER = Pattern.compile("^[0-9]+$");
	
	@Override
	public boolean processNodeList(ArrayList<Node> nodeList) {
		
		boolean madeChange = false;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			if (!patternLength(nodeList, i, 2))
				continue;
			
			Node nd = nodeList.get(i);
			
			String lastWord = getLastWord(nd, false);
			if (lastWord == null)
				continue;
			
			if (!NUMBER.matcher(lastWord).matches())
				continue;
			
			
			Node nextNd = nodeList.get(i + 1);
			
			if (!isInlineMath(nextNd))
				continue;
			
			MathMode mm = (MathMode)nextNd;
			
			Block eqn = mm.getEquations().get(0).get(0);
			
			Node firstMath = eqn.values.get(0);
			
			if (isCommandName(firstMath, "vect")) {
				
				// If this is a vector then insert the coefficient before
				// the vector command
				
				LiteralString newStrNode = new LiteralString(lastWord, nd.getInputFile(),
						nd.getLineNum(), nd.getColNum());
				
				eqn.values.add(0, newStrNode);
				
			}
			else {
				
				// Otherwise if this is a string, then modify the value
				// to start with the coefficient
				
				String firstMathStr = getLiteralString(firstMath);
				if (firstMathStr == null)
					continue;
				
				char c = firstMathStr.charAt(0);
				if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z'))
					continue;
				
				((LiteralString)firstMath).value = lastWord + ((LiteralString)firstMath).value; 
				
			}
			
			// Remove lastWord from string outside of math mode
			LiteralString firstStrNd = (LiteralString)nd;
			
			firstStrNd.value = firstStrNd.value.substring(0, firstStrNd.value.length() - lastWord.length()); 
			
			madeChange = true;
			
		}
		
		
		return madeChange;
	}

}
