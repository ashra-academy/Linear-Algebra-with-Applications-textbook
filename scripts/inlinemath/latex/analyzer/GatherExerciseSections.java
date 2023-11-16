package latex.analyzer;

import java.util.ArrayList;

import latex.NodeTraverserIgnoreHTMLAsImage;
import latex.node.Block;
import latex.node.Chapter;
import latex.node.Node;
import latex.node.Section;
import latex.node.SubSection;

/**
 * In the Calculus textbook, there are some sections that have
 * unnumbered exercise sections in the middle of a numbered section.
 * The ChapterSectionSplit analyzer already handles merging the
 * numbered subsections into the proper section, but it will leave
 * the two numbered exercise sections side-by-side, which will
 * confuse the HTMLWriter.  This analyzer merges the exercise
 * sections into one.
 */
public class GatherExerciseSections extends Analyzer {
	
	
	private static class ExerciseSectionFinder extends NodeTraverserIgnoreHTMLAsImage {
		
		@Override
		protected Action beforeVisitChapter(Chapter chap) {
			
			ArrayList<Node> sectionList = chap.getContent().values;
			
			// Look for an unnumbered section that contains the title "Exercises"
			
			for (int i = 1; i < sectionList.size(); i++) {
				
				Node nd = sectionList.get(i);
				
				if (!(nd instanceof Section))
					continue;
				
				Section sectionNd = (Section)nd;
				
				if (sectionNd.getNumber() > 0)
					continue;
				
				String sectionTitleLatex = sectionNd.getTitle().toLatexString(false);
				
				if (!sectionTitleLatex.contains("Exercises"))
					continue;
				
				// Is there an Exercises section right before this one?
				
				Node prevNd = sectionList.get(i - 1);
				
				if (!(prevNd instanceof Section))
					continue;
				
				Section prevSectionNd = (Section)prevNd;
				
				if (prevSectionNd.getNumber() > 0)
					continue;
				
				sectionTitleLatex = prevSectionNd.getTitle().toLatexString(false);
				
				if (!sectionTitleLatex.contains("Exercises"))
					continue;
				
				// Merge the contents of this exercises section into the next one
				
				ArrayList<Node> newValues = prevSectionNd.getContent().values;
				newValues.addAll(sectionNd.getContent().values);
				
				// Delete this exercises section
				
				sectionList.remove(i);
				i--;
				
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
		new ExerciseSectionFinder().traverseBlock(topLevelBlock, topLevelBlock);
	}

}
