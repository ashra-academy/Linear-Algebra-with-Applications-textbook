package latex.analyzer;

import java.io.File;
import java.util.Iterator;

import latex.LatexParser;
import latex.LatexParserException;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Tabular;

/**
 * Traverses down the given block of latex and replaces all \input tags with the parsed latex files.
 * This operates recursively for any files that include other files.
 */
public class ParseInputCommands extends Analyzer {
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		throw new AnalysisException("This analyzer needs to know what the working directory for the latex file is", topLevelBlock);
	}

	public void doAnalysis(Block topLevelBlock, File currentWorkingDirectory) {
		
		for (int i = 0; i < topLevelBlock.values.size(); i++) {

			Node nd = topLevelBlock.values.get(i);

			if (nd instanceof Block) {
				doAnalysis((Block)nd, currentWorkingDirectory);
			}
			else if (nd instanceof Tabular) {

				Tabular tab = (Tabular)nd;
				
				for (Iterator<Tabular.Cell> cellIt = tab.getCellIterator(); cellIt.hasNext(); ) {
					doAnalysis(cellIt.next().getContent(), currentWorkingDirectory);
				}
				
			}
			else if (nd instanceof Environment) {

				Environment env = (Environment)nd;

				doAnalysis(env.content, currentWorkingDirectory);

			}
			else if (nd instanceof Command) {
				Command cmd = (Command)nd;

				if (cmd.name.equals("input") || cmd.name.equals("subimport")) {

					String relPath = "";

					topLevelBlock.values.remove(i);

					if (cmd.name.equals("input")) {

						relPath = ((LiteralString)(cmd.getParameter(0).values.get(0))).value;
					}
					else {
						
						// Since FixupParameters hasn't run yet, the second parameter isn't attached
						// to the subimport command.  It is just block that comes next.

						Block filenamePart = (Block)topLevelBlock.values.remove(i);

						relPath = filenamePart.toLatexString(false);
					}

					if (!relPath.contains("."))
						relPath += ".tex";
					
					File inputFile = new File(currentWorkingDirectory, relPath);

					Node topLevelFromInput;
					LatexParser parser = new LatexParser(inputFile);
					try {
						topLevelFromInput = parser.parse();
					}
					catch (LatexParserException ex) {
						throw new AnalysisException("Error parsing input file: " + inputFile.getAbsolutePath() + ", Line: " + ex.getLine() + ", Col: " + ex.getCol(), ex, null);
					}
					
					if (topLevelFromInput instanceof Block) {

						Block topLevelFromInputBlock = (Block)topLevelFromInput;

						new RemoveSolutionFiles().doAnalysis(topLevelFromInputBlock);

						topLevelBlock.values.addAll(i, topLevelFromInputBlock.values);

						i--; // Force reparse of new nodes

					}
					else {
						topLevelBlock.values.add(i, topLevelFromInput);
						i--; // Force reparse of new node
					}

				}

			}

		}

	}

}
