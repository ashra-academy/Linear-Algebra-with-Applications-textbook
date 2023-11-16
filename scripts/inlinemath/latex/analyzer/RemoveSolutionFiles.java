package latex.analyzer;

import java.util.HashSet;
import java.util.Iterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;

public class RemoveSolutionFiles extends Analyzer {
	
	private static class Traverser extends NodeTraverserIgnoreHTMLAsImage {
		
		private HashSet<String> solutionFiles = new HashSet<String>();
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			
			for (Iterator<Node> ndIt = blk.values.iterator(); ndIt.hasNext(); ) {
				
				Node nd = ndIt.next();
				
				if (nd instanceof Command) {
					Command cmd = (Command)nd;
					
					if (cmd.name.equals("Opensolutionfile")) {
						
						if (cmd.getParameter(0).values.size() != 1)
							throw new AnalysisException("\\Opensolutionfile parameter contains other latex", cmd);
						
						Node solFileNode = cmd.getParameter(0).values.get(0);
						if (!(solFileNode instanceof LiteralString))
							throw new AnalysisException("\\Opensolutionfile parameter not a literal string", cmd);
						
						String solFileName = ((LiteralString)solFileNode).value;
						
						solutionFiles.add(solFileName);
						
						ndIt.remove();
						
					}
					else if (cmd.name.equals("Closesolutionfile")) {
						ndIt.remove();
					}
					else if (cmd.name.equals("Writetofile")) {
						ndIt.remove();
						ndIt.next(); // second parameter
						ndIt.remove();
					}
					else if (cmd.name.equals("input")) {
						
						if (cmd.getParameter(0).values.size() != 1)
							throw new AnalysisException("\\input parameter contains other latex", cmd);
						
						Node filenameNode = cmd.getParameter(0).values.get(0);
						if (!(filenameNode instanceof LiteralString))
							throw new AnalysisException("\\input parameter not a literal string", cmd);
						
						String inputFilename = ((LiteralString)filenameNode).value;
						
						if (solutionFiles.contains(inputFilename)) {
							ndIt.remove();
						}
						
					}
					
				}
				else if (nd instanceof Environment) {
					Environment env = (Environment)nd;
					
					if (env.name.equals("Filesave")) {
						ndIt.remove();
					}
					
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
