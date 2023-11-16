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
 * Extracts \tag commands from math mode into the references list
 * so that they can be displayed by the writer in table format.
 */
public class ExtractMathModeTags extends Analyzer {
	
	private static class ExtractTagCommand extends NodeTraverserIgnoreHTMLAsImage {
		
		private String tagValue = null;
		
		public String getTagValue() {
			return tagValue;
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
				
				if (!childCmd.name.equals("tag"))
					continue;
				
				if (tagValue != null)
					throw new AnalysisException("Math equation has more than one tag command", childCmd);
				
				it.remove();
				
				if (childCmd.getNumParameters() != 1)
					throw new AnalysisException("tag node must have exactly one parameter", childCmd);
				
				Block param = childCmd.getParameter(0);
				
				if (param.values.size() != 1)
					throw new AnalysisException("tag node parameter must have exactly one LiteralString node", childCmd);
				
				Node paramNd = param.values.get(0);
				
				if (!(paramNd instanceof LiteralString))
					throw new AnalysisException("tag node parameter must have exactly one LiteralString node", childCmd);
				
				tagValue = ((LiteralString)paramNd).value;
				
			}
			
			super.traverseBlock(blk, context);
		}
		
	}
	
	private static class FindMathModes extends NodeTraverserIgnoreHTMLAsImage {
		
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
		protected Action visitMathMode(MathMode math) {
			
			ArrayList<String> newTagsToAdd = new ArrayList<String>();
			boolean haveNewTag = false;
			
			for (ArrayList<Block> row : math.getEquations()) {
				
				String newTag = null;
				
				for (Block blk : row) {
					
					ExtractTagCommand extractor = new ExtractTagCommand();
					extractor.traverseBlock(blk, math);
					
					if (extractor.getTagValue() != null) {
						if (newTag != null)
							throw new AnalysisException("Math row has more than one tag command", math);
						newTag = extractor.getTagValue();
					}
					
				}
				
				if (newTag != null) {
					haveNewTag = true;
					newTagsToAdd.add(newTag);
				}
				else {
					newTagsToAdd.add("");
				}
				
			}
			
			if (!haveNewTag)
				return Action.SkipChildren;
			
			// There were some \tag commands so we want to update the math mode object
				
			ArrayList<String> existingList = math.getReferenceNumbers();
			
			// The easy case is if this math mode was unnumbered already, then we can
			// just apply the list that we built above
			if (existingList.size() == 0) {
				existingList.addAll(newTagsToAdd);
				return Action.SkipChildren;
			}
			
			// The harder case is to merge the two lists, where \tag overwrites any existing numbering
			if (existingList.size() != newTagsToAdd.size())
				throw new AnalysisException("Equation list size changed?", math);
			
			for (int i = 0; i < newTagsToAdd.size(); i++) {
				
				if (!"".equals(newTagsToAdd.get(i))) {
					existingList.remove(i);
					existingList.add(i, newTagsToAdd.get(i));
				}
				
			}
			
			return Action.SkipChildren;
			
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
