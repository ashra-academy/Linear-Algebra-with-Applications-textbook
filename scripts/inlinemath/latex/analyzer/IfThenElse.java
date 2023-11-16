package latex.analyzer;


import java.util.Hashtable;
import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.LiteralString;
import latex.node.Node;

/**
 * Resolves \ifthenelse{}{}{} commands so that only the appropriopriate block
 * appears in the main source file.  This way commands can be hidden from the
 * rest of the HTML compilation process.
 */
public class IfThenElse extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		private Hashtable<String, Boolean> booleanVariables = new Hashtable<String, Boolean>(); 
		
		private String evaluateOperand(Block blk) {
			
			if (blk.values.size() != 1)
				throw new AnalysisException("Expected only one value in operand to equals node", blk);
			
			Node nd = blk.values.get(0);
			
			if (!(nd instanceof Command))
				throw new AnalysisException("Can't evaluate node: " + nd.getClass().getName(), nd);
				
			Command cmd = (Command)nd;
			
			if (cmd.name.equals("jobname")) {
				return "xxxxxxxxxx"; // TODO: Configurable on command line?
			}
			else if (cmd.name.equals("detokenize")) {
				
				if (cmd.getNumParameters() != 1)
					throw new AnalysisException("Detokenize needs one string parameter", cmd);
				
				if (cmd.getParameter(0).values.size() != 1)
					throw new AnalysisException("Detokenize needs one string parameter", cmd);
				
				Node ndToken = cmd.getParameter(0).values.get(0);
				
				if (!(ndToken instanceof LiteralString))
					throw new AnalysisException("Detokenize needs one string parameter", cmd);
				
				LiteralString strNd = (LiteralString)ndToken;
				
				return strNd.value;
			}
			else if (cmd.name.equals("institution")) {
				return ""; // TODO?
			}
			else
				throw new AnalysisException("Unknown operand to equals node: \\" + cmd.name, nd);
			
		}
		
		private boolean evaluateTestBlock(Command ifThenCmd, ListIterator<Node> ndIt) {
			
			Boolean retVal = null;
			boolean leftParenSeen = false;
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (nd instanceof Command) {
					
					Command cmd = (Command)nd;
					
					if (cmd.name.equals("equal")) {
						
						if (retVal != null)
							throw new AnalysisException("Unexpected EQUALS op", nd);
						
						String lhs = evaluateOperand(cmd.getParameter(0));
						String rhs = evaluateOperand(cmd.getParameter(1));
						
						retVal = lhs.equals(rhs);
						
					}
					else if (cmd.name.equalsIgnoreCase("or")) {
						
						if (retVal == null)
							throw new AnalysisException("Missing left side of OR op", nd);
						
						boolean rhs = evaluateTestBlock(ifThenCmd, ndIt);
						
						retVal |= rhs;
						
					}
					else if (cmd.name.equalsIgnoreCase("and")) {
						
						if (retVal == null)
							throw new AnalysisException("Missing left side of AND op", nd);
						
						boolean rhs = evaluateTestBlock(ifThenCmd, ndIt);
						
						retVal &= rhs;
						
					}
					else if (cmd.name.equalsIgnoreCase("boolean")) {
						
						if (cmd.getParameter(0).values.size() != 1)
							throw new AnalysisException("Missing String name for boolean", cmd);
						
						Node paramNd1 = cmd.getParameter(0).values.get(0);
						
						if (!(paramNd1 instanceof LiteralString))
							throw new AnalysisException("Missing String name for boolean", cmd);
						
						String varName = ((LiteralString)paramNd1).value;
						
						if (!booleanVariables.containsKey(varName))
							throw new AnalysisException("Missing boolean '" + varName + "' was not defined anywhere", cmd);
						
						retVal = booleanVariables.get(varName);
						
					}
					
				}
				else if (nd instanceof LiteralString) {
					
					LiteralString strNd = (LiteralString)nd;
					
					if (strNd.value.equals("(")) {
						
						if (retVal != null)
							throw new AnalysisException("Unexpected LEFT PAREN", nd);
						
						leftParenSeen = true;
						
					}
					else if (strNd.value.equals(")")) {
						
						if (!leftParenSeen)
							throw new AnalysisException("Unbalanced PARENS", nd);
						
						if (retVal == null)
							throw new AnalysisException("Missing operands inside PARENS", nd);
						
						return retVal;
					}
					
				}
				else
					throw new AnalysisException("Can't evaluate node: " + nd.getClass().getName(), nd);
				
				
			}
			
			if (retVal == null)
				throw new AnalysisException("Missing operand", ifThenCmd);
			
			return retVal;
			
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
				
			ListIterator<Node> ndIt = blk.values.listIterator();
			
			while (ndIt.hasNext()) {
				
				Node nd = ndIt.next();
				
				if (!(nd instanceof Command))
					continue;
				
				Command cmd = (Command)nd;
				
				if (cmd.name.equals("setboolean")) {
					
					if (cmd.getParameter(0).values.size() != 1)
						throw new AnalysisException("Missing String name for boolean", cmd);
					
					Node paramNd1 = cmd.getParameter(0).values.get(0);
					
					if (!(paramNd1 instanceof LiteralString))
						throw new AnalysisException("Missing String name for boolean", cmd);
					
					String varName = ((LiteralString)paramNd1).value;
					
					if (cmd.getParameter(1).values.size() != 1)
						throw new AnalysisException("Missing String value for boolean", cmd);
					
					Node paramNd2 = cmd.getParameter(1).values.get(0);
					
					if (!(paramNd2 instanceof LiteralString))
						throw new AnalysisException("Missing String value for boolean", cmd);
					
					String varValue = ((LiteralString)paramNd2).value;
					
					if (!"true".equals(varValue) && !"false".equals(varValue))
						throw new AnalysisException("Value for boolean must be the string 'true' or 'false'", cmd);
					
					booleanVariables.put(varName, "true".equals(varValue));
					
				}
				else if (cmd.name.equals("ifthenelse")) {
					
					Block testBlk = cmd.getParameter(0);
					
					ListIterator<Node> testBlkIt = testBlk.values.listIterator();
					
					int parameterToUse;
					
					if (evaluateTestBlock(cmd, testBlkIt))
						parameterToUse = 1;
					else
						parameterToUse = 2;
					
					ndIt.remove();
					
					for (Node newNd : cmd.getParameter(parameterToUse).values)
						ndIt.add(newNd);
					
					// Back up the cursor by the same number of elements so they will be parsed
					for (Node newNd : cmd.getParameter(parameterToUse).values)
						ndIt.previous();
					
				}
				
			}
			
			super.traverseBlock(blk, context);
			
		}
	
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		new Traverser().traverseBlock(topLevelBlock, topLevelBlock);
	}
	
}
