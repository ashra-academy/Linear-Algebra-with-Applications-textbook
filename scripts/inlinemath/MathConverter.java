import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.ListIterator;

import latex.LatexParser;
import latex.LatexParserException;
import latex.NodeTraverser;
import latex.LatexParser.State;
import latex.analyzer.AnalysisException;
import latex.analyzer.ApplyAttributesToEnvironment;
import latex.analyzer.ApplyTableParameters;
import latex.analyzer.ExtractEnumerate;
import latex.analyzer.FixupObsoleteCommands;
import latex.analyzer.FixupParameters;
import latex.analyzer.ParseInputCommands;
import latex.analyzer.RemoveAdjustWidth;
import latex.analyzer.RemoveSolutionFiles;
import latex.analyzer.TableApplyLinesAndColors;
import latex.analyzer.TableApplyWidths;
import latex.analyzer.TableExpandRowColSpans;
import latex.analyzer.TableExtractSpans;
import latex.analyzer.TableRemoveRowSpacingAdjustment;
import latex.analyzer.UnicodeCharacters;
import latex.analyzer.WrapLongTable;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.MathMode;
import latex.node.Node;
import latex.node.Paragraph;
import latex.node.Enumerate.Item;
import patterns.*;

/**
 * Helper program that uses the LatexToHTML parser to convert various
 * Latex constrcuts that look like math into math modes.  For example,
 * using \textit{x}\textsuperscript{2} looks like x^2 so that will be
 * replaced with $x^2$.
 * 
 * This program can either take a document as --input and write out
 * the contents (minus things outside begin{document} to --output.
 * 
 * Or the --fragment can be used to take arbitrary latex fragment
 * (assuming it is syntatically valid) in stdin and write the converted
 * fragment to stdout.  This can be used to replace text with emacs.
 * 
 */
public class MathConverter {
	
	private static final String USAGE = "Usage: MathConverter --input input_file --output output_path\n OR MathConverter --fragment < input > output";
	
	private static class MathPatternTraverser extends NodeTraverser {
		
		protected void doWork(ArrayList<Node> nodeList) {
			
			SuperAndSubScripts scPat = new SuperAndSubScripts();
			
			scPat.processNodeList(nodeList);
			
			SingleVariable sv = new SingleVariable();
			
			sv.processNodeList(nodeList);
			
			boolean madeChanges;
			
			do {
				
				madeChanges = false;
				
				Coefficients coef = new Coefficients();
				
				madeChanges |= coef.processNodeList(nodeList);
				
				BinaryOperators bops = new BinaryOperators();
				
				madeChanges |= bops.processNodeList(nodeList);
				
				Functions func = new Functions();
				
				madeChanges |= func.processNodeList(nodeList);
				
				ListOfItems loI = new ListOfItems();
				
				madeChanges |= loI.processNodeList(nodeList);
				
				MergeMathModes mmerge = new MergeMathModes();
				
				madeChanges |= mmerge.processNodeList(nodeList);
				
				RemoveBracesMinus removeBraces = new RemoveBracesMinus();
				
				madeChanges |= removeBraces.processNodeList(nodeList);
				
			} while (madeChanges);
			
		}
		
		@Override
		protected Action beforeVisitParagraph(Paragraph para) {
			
			doWork(para.values);
			
			return super.beforeVisitParagraph(para);
		}
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {
			
			if (env.name.equals("definition") || env.name.equals("example") || env.name.equals("lemma") || env.name.equals("theorem") ||
					env.name.equals("proof") || env.name.equals("solution") || env.name.equals("ex") || env.name.equals("sol")) {
				doWork(env.content.values);
			}
			
			return super.beforeVisitEnvironment(env);
		}
		
		@Override
		protected Action beforeVisitEnumerateItem(Item itm) {
			
			doWork(itm.getContent().values);
			
			return super.beforeVisitEnumerateItem(itm);
		}
		
		
		@Override
		protected Action visitMathMode(MathMode math) {
			return Action.SkipChildren;
		}
		
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			super.traverseBlock(blk, context);
		}
		
	}
		
	private static class RemoveItalicsTraverser extends NodeTraverser {
		
		protected void doWork(ArrayList<Node> nodeList) {
			
			for (int i = 0; i < nodeList.size(); i++) {
				
				Node nd = nodeList.get(i);
				
				if (!(nd instanceof Command))
					continue;
				
				Command cmd = (Command) nd;
				
				if (!cmd.name.equals("textit"))
					continue;
				
				nodeList.remove(i);
				nodeList.addAll(i, cmd.getParameter(0).values);
				
			}
			
		}
		
		@Override
		protected Action beforeVisitParagraph(Paragraph para) {
			
			doWork(para.values);
			
			return super.beforeVisitParagraph(para);
		}
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {
			
			if (env.name.equals("definition") || env.name.equals("example") || env.name.equals("lemma") || env.name.equals("theorem") ||
					env.name.equals("proof") || env.name.equals("solution") || env.name.equals("ex") || env.name.equals("sol")) {
				doWork(env.content.values);
			}
			
			return super.beforeVisitEnvironment(env);
		}
		
		@Override
		protected Action beforeVisitEnumerateItem(Item itm) {
			
			doWork(itm.getContent().values);
			
			return super.beforeVisitEnumerateItem(itm);
		}
		
		
		@Override
		protected Action visitMathMode(MathMode math) {
			return Action.SkipChildren;
		}
		
		
		@Override
		protected void traverseBlock(Block blk, Node context) {
			super.traverseBlock(blk, context);
		}
		
	}
	
	
	
	private static void doFragmentMode(boolean removeItalics) {
		
		LatexParser parser = null;
		
		try {
			
			//parser = new LatexParser(new File("/lyryx/textbooks/lawa/trunk/text/9-change-of-basis/1-the-matrix-of-a-linear-transformation.tex"));
			parser = new LatexParser(new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8"))));
			
			Block topLevel = parser.parse();
			
			Block fakeTopLevel = new Block(null, 0, 0);
			
			Environment fakeDocument = new Environment(null, 0, 0);
			fakeDocument.name = "document";
			fakeDocument.content = topLevel;
			
			fakeTopLevel.values.add(fakeDocument);
			
			try {
				// Modified DefaultAnalysis...
				
				new FixupParameters().doAnalysis(fakeTopLevel);
				
				new UnicodeCharacters().doAnalysis(fakeTopLevel);
				
				//new IfThenElse().doAnalysis(fakeTopLevel);
				
				new ApplyAttributesToEnvironment().doAnalysis(fakeTopLevel);
				
				new FixupObsoleteCommands().doAnalysis(fakeTopLevel);
				
				new RemoveAdjustWidth().doAnalysis(fakeTopLevel);
				
				new WrapLongTable().doAnalysis(fakeTopLevel);
				
				//new ChapterSectionSplit().doAnalysis(fakeTopLevel);
				
				//new GatherExerciseSections().doAnalysis(fakeTopLevel);
				
				new ExtractEnumerate().doAnalysis(fakeTopLevel);
				
				//new ExtractCaptions().doAnalysis(fakeTopLevel);
				
				// Begin tables
				
				new TableRemoveRowSpacingAdjustment().doAnalysis(fakeTopLevel);
				
				new TableExtractSpans().doAnalysis(fakeTopLevel);
				
				new TableApplyLinesAndColors().doAnalysis(fakeTopLevel);
				
				new TableExpandRowColSpans().doAnalysis(fakeTopLevel);
				
				new ApplyTableParameters().doAnalysis(fakeTopLevel);
				
				new TableApplyWidths().doAnalysis(fakeTopLevel);
				
				// End tables
				
				//new ApplyDotFillToParent().doAnalysis(fakeTopLevel);
				
				//new ResolveReferences().doAnalysis(fakeTopLevel);
				
				//new ExtractMathModeTags().doAnalysis(fakeTopLevel);
				
				//if ("html".equals(writerType)) {
				//	new FixupForMathJax().doAnalysis(fakeTopLevel);
				//}
				
				//new ParagraphSplit().doAnalysis(fakeTopLevel);
				
			} catch (AnalysisException e) {
				if (e.getLocation() != null) {
					System.err.println("At " + e.getLocation().getLineNum() + ", " + e.getLocation().getColNum() + " of " + e.getLocation().getInputFile().getAbsolutePath());
				}
				e.printStackTrace();
				return;
			}
			
			if (removeItalics) {
				
				RemoveItalicsTraverser t = new RemoveItalicsTraverser();
				
				// First just do the very top level text...
				t.doWork(topLevel.values);
				
				// Now try to do children like examples, etc
				t.traverseBlock(topLevel, topLevel);
				
			}
			else {
				
				MathPatternTraverser t = new MathPatternTraverser();
				
				// First just do the very top level text...
				t.doWork(topLevel.values);
				
				// Now try to do children like examples, etc
				t.traverseBlock(topLevel, topLevel);
				
			}
			
			
			OutputStreamWriter writer = new OutputStreamWriter(System.out, "UTF-8");
			
			writer.write(topLevel.toLatexString(false));
			
			writer.flush();
			
			writer.close();
			
		}
		catch (LatexParserException ex) {
			
			if (ex.getFile() != null)
				System.err.println("Parser error " + ex.getMessage() + " at " + ex.getLine() + "," + ex.getCol() + " of " + ex.getFile().getName());
			else
				System.err.println("Parser error " + ex.getMessage());
			System.err.println("Node stack:");
			while (parser.getNodeStack().size() > 0) {
				Node stnd = parser.getNodeStack().pop();
				System.err.println(stnd.getLineNum() + "," + stnd.getColNum() + " * " + stnd.toLatexString());
			}
			System.err.println("State stack:");
			while (parser.getStateStack().size() > 0) {
				State stnd = parser.getStateStack().pop();
				System.err.println("* " + stnd);
			}
			System.err.println("Buffer: " + parser.getReadBuffer());
			
			ex.printStackTrace();
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}
	
	
	public static void main(String[] args) {
		
		File inputFile = null;
		File outputPath = null;
		boolean fragmentMode = false;
		boolean removeItalics = false;
		
		// Process command line
		try {
			
			int argPos = 0;
			while (argPos < args.length) {

				if (args[argPos].equals("--help") || args[argPos].equals("-h")) {
					System.err.println(USAGE);
					System.exit(1);
				}
				else if (args[argPos].equals("--input")) {
					inputFile = new File(args[++argPos]);
				}
				else if (args[argPos].equals("--output")) {
					outputPath = new File(args[++argPos]);
				}
				else if (args[argPos].equals("--fragment")) {
					fragmentMode = true;
				}
				else if (args[argPos].equals("--removeitalics")) {
					removeItalics = true;
				}
				else {
					System.err.println(USAGE);
					System.exit(1);
				}
				
				argPos++;
				
			} // for each arg
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		
		if (fragmentMode) {
			doFragmentMode(removeItalics);
			System.exit(0);
		}
		
		if (inputFile == null || outputPath == null) {
			System.err.println(USAGE);
			System.exit(1);
		}
		
		File workingDirectory = inputFile.getParentFile();
		
		if (!inputFile.exists()) {
			System.err.println("Input file does not exist");
			System.exit(1);
		}
		
		if (outputPath.exists()) {
			System.err.println("Output path exists, refusing to overwrite");
			System.exit(1);
		}
		
		LatexParser parser = new LatexParser(inputFile);
		
		try {
			
			Block topLevel = parser.parse();
			
			new RemoveSolutionFiles().doAnalysis(topLevel);
			
			new ParseInputCommands().doAnalysis(topLevel, workingDirectory);
			
			try {
				// Modified DefaultAnalysis...
				
				new FixupParameters().doAnalysis(topLevel);
				
				new UnicodeCharacters().doAnalysis(topLevel);
				
				//new IfThenElse().doAnalysis(topLevel);
				
				new ApplyAttributesToEnvironment().doAnalysis(topLevel);
				
				new FixupObsoleteCommands().doAnalysis(topLevel);
				
				new RemoveAdjustWidth().doAnalysis(topLevel);
				
				new WrapLongTable().doAnalysis(topLevel);
				
				//new ChapterSectionSplit().doAnalysis(topLevel);
				
				//new GatherExerciseSections().doAnalysis(topLevel);
				
				new ExtractEnumerate().doAnalysis(topLevel);
				
				//new ExtractCaptions().doAnalysis(topLevel);
				
				// Begin tables
				
				new TableRemoveRowSpacingAdjustment().doAnalysis(topLevel);
				
				new TableExtractSpans().doAnalysis(topLevel);
				
				new TableApplyLinesAndColors().doAnalysis(topLevel);
				
				new TableExpandRowColSpans().doAnalysis(topLevel);
				
				new ApplyTableParameters().doAnalysis(topLevel);
				
				new TableApplyWidths().doAnalysis(topLevel);
				
				// End tables
				
				//new ApplyDotFillToParent().doAnalysis(topLevel);
				
				//new ResolveReferences().doAnalysis(topLevel);
				
				//new ExtractMathModeTags().doAnalysis(topLevel);
				
				//if ("html".equals(writerType)) {
				//	new FixupForMathJax().doAnalysis(topLevel);
				//}
				
				//new ParagraphSplit().doAnalysis(topLevel);
				
			} catch (AnalysisException e) {
				if (e.getLocation() != null) {
					System.err.println("At " + e.getLocation().getLineNum() + ", " + e.getLocation().getColNum() + " of " + e.getLocation().getInputFile().getAbsolutePath());
				}
				e.printStackTrace();
				return;
			}
			
			ArrayList<Command> usePackageList = new ArrayList<Command>();
			Block documentBlock = null;
			
			
			for (ListIterator<Node> ndIt = topLevel.values.listIterator(); ndIt.hasNext(); ) {
				
				Node nd = ndIt.next();
				
				if (nd instanceof Command) {
					
					Command cmdNode = (Command)nd;
					
					if (cmdNode.name.equals("newcommand") || cmdNode.name.equals("renewcommand")) {
						
						Node secondParam = ndIt.next();
						
						cmdNode.addParameter((Block)secondParam);
						
					}
					
					if (cmdNode.name.equals("usepackage") || cmdNode.name.equals("tcbuselibrary") ||
							cmdNode.name.equals("usetikzlibrary") || cmdNode.name.equals("setmainfont") ||
							cmdNode.name.equals("newcommand") || cmdNode.name.equals("renewcommand") ||
							cmdNode.name.equals("newboolean") || cmdNode.name.equals("newlength") ||
							cmdNode.name.equals("setlength")) {
						
						usePackageList.add(cmdNode);
						
					}
					
					
				}
				else if (nd instanceof Environment) {
					
					Environment envNode = (Environment)nd;
					
					if (envNode.name.equalsIgnoreCase("document")) {
						documentBlock = envNode.content;
					}
					
				}
				
			}
			
			if (documentBlock == null) {
				System.err.println("File has no \\begin{document}?");
				return;
			}
			
			MathPatternTraverser t = new MathPatternTraverser();
			
			// First just do the very top level text...
			t.doWork(documentBlock.values);
			
			// Now try to do children like examples, etc
			t.traverseBlock(documentBlock, documentBlock);
			
			System.err.println("DONE!");
			
			
			try {
				FileWriter fw = new FileWriter(outputPath);
				
				fw.write(documentBlock.toLatexString(false));
				
				fw.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		catch (LatexParserException ex) {
			
			if (ex.getFile() != null)
				System.err.println("Parser error " + ex.getMessage() + " at " + ex.getLine() + "," + ex.getCol() + " of " + ex.getFile().getName());
			else
				System.err.println("Parser error " + ex.getMessage());
			System.err.println("Node stack:");
			while (parser.getNodeStack().size() > 0) {
				Node stnd = parser.getNodeStack().pop();
				System.err.println(stnd.getLineNum() + "," + stnd.getColNum() + " * " + stnd.toLatexString());
			}
			System.err.println("State stack:");
			while (parser.getStateStack().size() > 0) {
				State stnd = parser.getStateStack().pop();
				System.err.println("* " + stnd);
			}
			System.err.println("Buffer: " + parser.getReadBuffer());
			
			ex.printStackTrace();
		}
		
	}

}
