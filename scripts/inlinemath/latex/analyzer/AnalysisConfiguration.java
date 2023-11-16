package latex.analyzer;

import latex.node.Block;

public class AnalysisConfiguration {

	public static void defaultAnalysis(Block topLevelBlock, String writerType) {
		
		new FixupParameters().doAnalysis(topLevelBlock);
		
		new UnicodeCharacters().doAnalysis(topLevelBlock);
		
		new IfThenElse().doAnalysis(topLevelBlock);
		
		new ApplyAttributesToEnvironment().doAnalysis(topLevelBlock);
		
		new FixupObsoleteCommands().doAnalysis(topLevelBlock);
		
		new RemoveAdjustWidth().doAnalysis(topLevelBlock);
		
		new WrapLongTable().doAnalysis(topLevelBlock);
		
		new ChapterSectionSplit().doAnalysis(topLevelBlock);
		
		new GatherExerciseSections().doAnalysis(topLevelBlock);
		
		new ExtractEnumerate().doAnalysis(topLevelBlock);
		
		new ExtractCaptions().doAnalysis(topLevelBlock);
		
		// Begin tables
		
		new TableRemoveRowSpacingAdjustment().doAnalysis(topLevelBlock);
		
		new TableExtractSpans().doAnalysis(topLevelBlock);
		
		new TableApplyLinesAndColors().doAnalysis(topLevelBlock);
		
		new TableExpandRowColSpans().doAnalysis(topLevelBlock);
		
		new ApplyTableParameters().doAnalysis(topLevelBlock);
		
		new TableApplyWidths().doAnalysis(topLevelBlock);
		
		// End tables
		
		new ApplyDotFillToParent().doAnalysis(topLevelBlock);
		
		new ResolveReferences().doAnalysis(topLevelBlock);
		
		new ExtractMathModeTags().doAnalysis(topLevelBlock);
		
		if ("html".equals(writerType)) {
			new FixupForMathJax().doAnalysis(topLevelBlock);
		}
		
		new ParagraphSplit().doAnalysis(topLevelBlock);
		
	}
	
	
}
