package latex.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.Node;

/**
 * Base class for all analysis actions.
 */
public abstract class Analyzer {

	public abstract void doAnalysis(Block topLevelBlock);
	
	protected Environment getDocumentEnvironment(Block topLevelBlock) {
		
		for (Node nd : topLevelBlock.values) {
			
			if (nd instanceof Environment) {
				
				Environment envNode = (Environment)nd;
				
				if (envNode.name.equalsIgnoreCase("document")) {
					return envNode;
				}
				
			}
			
		}
		
		throw new AnalysisException("Unable to find begin{document} in top level block", null, null);
	}

	/**
	 * Splits the list of nodes into Blocks, based on the command argument.  For example,
	 * splitBy(... "chapter") will produce a list of Blocks.  Each Block will start with
	 * a chapter Command object, except the first Block which will be anything before the first \chapter.
	 * 
	 * NOTE: This method automatically searches for the * version of the passed in commands.
	 * 
	 * If no commands of the given name are found, then the result will be one Block containing
	 * everything in nodeList.
	 */
	protected static ArrayList<Block> splitBy(ArrayList<Node> nodeList, String... commandNames) {
		
		HashSet<String> commandNameSet = new HashSet<String>(Arrays.asList(commandNames));
		for (String str : commandNames) {
			commandNameSet.add(str + "*");
		}
		
		ArrayList<Block> retVal = new ArrayList<Block>();
		
		int lastPos = 0;
		
		for (int i = 0; i < nodeList.size(); i++) {
			
			Node nd = nodeList.get(i);
			
			if (nd instanceof Command) {
	
				Command cmdNode = (Command)nd;
				
				if (commandNameSet.contains(cmdNode.name)) {
	
					if (i > 0) {
	
						Block blk = new Block(nodeList.get(lastPos).getInputFile(),
								nodeList.get(lastPos).getLineNum(),
								nodeList.get(lastPos).getColNum());
	
						blk.values = new ArrayList<Node>(nodeList.subList(lastPos, i));
	
						retVal.add(blk);
	
						lastPos = i;
					}
	
				}
	
			} // is a command
			
		} // for each item
		
		if ((lastPos == 0 && nodeList.size() == 1) ||
				(lastPos != nodeList.size() - 1)) {
			
			Block blk = new Block(nodeList.get(lastPos).getInputFile(),
					nodeList.get(lastPos).getLineNum(),
					nodeList.get(lastPos).getColNum());
			
			blk.values = new ArrayList<Node>(nodeList.subList(lastPos, nodeList.size()));
			
			retVal.add(blk);
		}
		
		return retVal;
	}
	
}
