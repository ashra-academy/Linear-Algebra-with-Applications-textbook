package latex.analyzer;

import java.util.ArrayList;
import java.util.ListIterator;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.NodeTraverser.Action;
import latex.node.Block;
import latex.node.Command;
import latex.node.Comment;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.MathMode;
import latex.node.Node;

/**
 * Makes changes to contents in math mode to deal with MathJax incompatibilities.
 * As one example, MathJax doesn't understand the \% escape sequence inside mbox,
 * as MathJax probably just takes the literal string and plugs it into HTML.
 */
public class FixupForMathJax extends Analyzer {
	
	private static class FixupMathMode extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitCommand(Command cmd) {
			
			if (cmd.name.equals("mbox")) {
				
				// Replace \% with %
				
				for (ListIterator<Node> it = cmd.getParameter(0).values.listIterator(); it.hasNext(); ) {
					
					Node nd = it.next();
					
					if (!(nd instanceof LiteralString))
						continue;
					
					LiteralString strNode = (LiteralString)nd;
					
					strNode.value = strNode.value.replaceAll("\\\\%", "%");
					
				}
				
			}
			// TODO: Replace \parbox with \mbox?
			
			return super.beforeVisitCommand(cmd);
		}

		@Override
		protected void traverseBlock(Block blk, Node context) {
			
			for (ListIterator<Node> it = blk.values.listIterator(); it.hasNext(); ) {
				
				Node childNd = it.next();
				
				if (childNd instanceof Comment) {
					it.remove();
					continue;
				}
				
				if (!(childNd instanceof Command))
					continue;
				
				Command childCmd = (Command)childNd;
				
				if (childCmd.name.equals("ds") || childCmd.name.equals("notag") || childCmd.name.equals("nonumber"))
					it.remove();
				
			}
			
			super.traverseBlock(blk, context);
		}
		
	}
	
	private static class FindMathModes extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action visitMathMode(MathMode math) {
			
			for (ArrayList<Block> row : math.getEquations()) {
				for (Block blk : row) {
					new FixupMathMode().traverseBlock(blk, math);
				}
			}
			
			return Action.SkipChildren;
			
		}
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {

			// Traverse parameters in case they have math in their text (captions, etc)
			if (env.getParameters() != null) {
				for (Block blk : env.getParameters()) {
					traverseBlock(blk, env);
				}
			}
			
			if (env.getPgfParameters() != null) {
				for (Block blk : env.getPgfParameters().values()) {
					traverseBlock(blk, env);
				}
			}
			
			if (env.getCaption() != null)
				traverseBlock(env.getCaption().getParameter(0), env);
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			super.traverseBlock(blk, context);
		}
		
	}

	@Override
	public void doAnalysis(Block topLevelBlock) {
		new FindMathModes().traverseBlock(topLevelBlock, topLevelBlock);
	}

}
