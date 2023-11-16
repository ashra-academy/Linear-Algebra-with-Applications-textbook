package latex.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import latex.NodeTraverser;
import latex.node.Block;
import latex.node.Command;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Tabular;

/**
 * Moves parameter blocks to their corresponding command or environment.  The main parser tries
 * to parse \command{param} into one Command node with one parameter.  However, it does not understand
 * commands that have 2 or more parameters, or environments that have parameters.  (To do that we would
 * need to parse the Tex libraries that define all commands.)
 * 
 * The output of the parser for a command with 2 parameters will be the following
 * (in the list of nodes for the current Block):
 *          ... -> Command{param1} -> Block which is actually param2 -> ...
 * when really this should all be one Command.  So this class will fix this up to be:
 *          ... -> Command{param1}{param2} -> ...
 * 
 * Likewise, Environments will have any parameters as the first children of their content:
 *          ... -> Environment{name} { Block of param1 -> Block of param2 -> actual children... }
 * and this will be fixed up to:
 *          ... -> Environment{name}{param1}{param2} { actual children... }
 * 
 */
public class FixupParameters extends Analyzer {
	
	private static Pattern TABU_WIDTH_RE = Pattern.compile("^[ \t]*(to|spread)[ \t]+.*");
	
	private static Hashtable<String, List<String>> environmentParameters;
	{
		environmentParameters = new Hashtable<String, List<String>>();
		
		environmentParameters.put("multicols", Arrays.asList("num of cols"));
		environmentParameters.put("adjustwidth", Arrays.asList("left margin", "right margin"));
		
		// \tcbmaketheorem{example}{Example}{examstyle}{mytheorem}{exa}
		// ==> \begin{example} (arg 1)
		//     Title before number is "Example " (arg 2).  Ex: "Example 1.1"
		//     Style is defined as arg 3 (see LyryxLearning.sty)
		//     Counter variable is arg 4
		//     Label is prefixed with arg 5, and taken from the \begin{example} param 2?.  Ex: "exa:Sets"
		// 
		// Result is \begin{example}{Title of box}{label}
		
		environmentParameters.put("example", Arrays.asList("title", "label"));
		environmentParameters.put("definition", Arrays.asList("title", "label"));
		environmentParameters.put("theorem", Arrays.asList("title", "label"));
		environmentParameters.put("corollary", Arrays.asList("title", "label"));
		environmentParameters.put("algorithm", Arrays.asList("title", "label"));
		environmentParameters.put("proposition", Arrays.asList("title", "label"));
		
		environmentParameters.put("htmlasimage", Arrays.asList("image file to use for HTML version"));
		
		// Accounting
		environmentParameters.put("acctex", Arrays.asList("learning objectives", "title"));
		environmentParameters.put("acctprob", Arrays.asList("learning objectives", "title"));
		
	}

	private static Hashtable<String, List<String>> environmentsWithPgfKeys;
	{
		environmentsWithPgfKeys = new Hashtable<String, List<String>>();

		// Economics
		environmentsWithPgfKeys.put("ApplicationBox", Arrays.asList("caption"));
		environmentsWithPgfKeys.put("ExampleBox", Arrays.asList("caption"));
		environmentsWithPgfKeys.put("Table", Arrays.asList("caption", "description", "descwidth", "deschangindent"));
		environmentsWithPgfKeys.put("TikzFigure", Arrays.asList("xscale", "yscale", "caption", "description", "descwidth", "deschangindent"));
		environmentsWithPgfKeys.put("TikzFigureWrap", Arrays.asList("width", "xscale", "yscale", "caption", "description"));
		
	}
	
	private static Hashtable<String, List<String>> commandsWithOptionKeys;
	{
		commandsWithOptionKeys = new Hashtable<String, List<String>>();
		commandsWithOptionKeys.put("finstmtIrow", Arrays.asList("indent", "dollarI"));
		commandsWithOptionKeys.put("finstmtIIrow", Arrays.asList("indent", "dollarI", "dollarII"));
		commandsWithOptionKeys.put("finstmtIIIrow", Arrays.asList("indent", "dollarI", "dollarII", "dollarIII"));
		
	}
	
	
	// More than one parameter for a command...
	private static Hashtable<String, List<String>> commandParameters;
	{
		commandParameters = new Hashtable<String, List<String>>();
		
		commandParameters.put("addcontentsline", Arrays.asList("sec unit", "entry"));
		commandParameters.put("addtocontents", Arrays.asList("entry"));
		commandParameters.put("href", Arrays.asList("displayText"));
		commandParameters.put("lyryxexploration", Arrays.asList("title"));
		commandParameters.put("markboth", Arrays.asList("right page title"));
		commandParameters.put("multicolumn", Arrays.asList("col spec", "contents"));
		commandParameters.put("multirow", Arrays.asList("width", "text"));
		commandParameters.put("parbox", Arrays.asList("contents"));
		commandParameters.put("rule", Arrays.asList("thickness"));
		commandParameters.put("setlength", Arrays.asList("value"));
		commandParameters.put("setboolean", Arrays.asList("value"));
		commandParameters.put("setcounter", Arrays.asList("value"));
		commandParameters.put("textcolor", Arrays.asList("contents"));
		commandParameters.put("ifthenelse", Arrays.asList("if-block", "then-block"));
		commandParameters.put("equal", Arrays.asList("right-hand side"));
		commandParameters.put("newlabel", Arrays.asList("label data"));
		
		// Lyryx
		commandParameters.put("includegraphicsalt", Arrays.asList("alt text"));
		
		// Accounting
		commandParameters.put("tn", Arrays.asList("text")); // Remove this?
		commandParameters.put("debitacct", Arrays.asList("amount"));
		commandParameters.put("creditacct", Arrays.asList("amount"));
		commandParameters.put("debitacctdate", Arrays.asList("Account name", "amount"));
		commandParameters.put("creditacctdate", Arrays.asList("Account name", "amount"));
		commandParameters.put("addtocounter", Arrays.asList("change"));
		commandParameters.put("finstmtIrow", Arrays.asList("amountI"));
		commandParameters.put("finstmtIIrow", Arrays.asList("amountI", "amountII"));
		commandParameters.put("finstmtIIIrow", Arrays.asList("amountI", "amountII" , "amountIII"));
		
	}
	
	// Tabu-style commands "\x=y"
	private static Hashtable<String, String> tabuCommandParameters;
	{
		tabuCommandParameters = new Hashtable<String, String>();
		
		tabuCommandParameters.put("tabcolsep", "width");
		tabuCommandParameters.put("tabulinesep", "width");
		
	}
	
	// This traverse needs to look at htmlasimage to grab the first parameter
	private static class Traverser extends NodeTraverser {
	
		/**
		 * Look through the content to find the first Block, ignoring whitespace.
		 * If a Block is found, then remove everything up to and including it and return it.
		 * Otherwise, return null if anything other than a Block or whitespace was found first,
		 *   or no block was found at all.
		 */
		private Block getFirstBlock(Block content, int startAt) {
			
			for (int i = startAt; i < content.values.size(); i++) {
				
				Node nd = content.values.get(i);
				
				if (nd instanceof Block) {
					
					// Got what we want
					content.values.subList(startAt, i+1).clear();
					
					return (Block)nd;
				}
				else if (nd instanceof LiteralString) {
					
					LiteralString str = (LiteralString)nd;
					
					// Whitespace?
					if (str.value.trim().length() != 0) {
						// No, so no block found.
						return null;
					}
					
					// Keep looking...
				}
				else {
					// Something else (like a command), so there must be no parameters
					return null;
				}
				
			}
	
			// Ran out of content, so not found
			return null;
			
		}
		
		private Block createBlock(ArrayList<Node> nodeList, int begin, int end) {
			
			Block blk = new Block(nodeList.get(begin).getInputFile(),
					nodeList.get(begin).getLineNum(),
					nodeList.get(begin).getColNum());
			
			blk.values = new ArrayList<Node>(nodeList.subList(begin, end));
			
			// Remove empty LiteralStrings that may have been introduced by the split process 
			ListIterator<Node> blkIt = blk.values.listIterator();
			
			while (blkIt.hasNext()) {
				
				Node nd = blkIt.next();
				
				if (!(nd instanceof LiteralString))
					continue;
				
				LiteralString strNd = (LiteralString) nd;
				
				if (strNd.value.equals(""))
					blkIt.remove();
				
			}
			
			return blk;
		}
		
		/**
		 * Split a nodeList by LiteralString commas into a list of Blocks.  If
		 * a comma is in the middle of a string, then the LiteralString node
		 * will be split into two parts each appearing the the corresponding Block.
		 */
		private ArrayList<Block> splitByComma(ArrayList<Node> nodeList) {
			
			ArrayList<Block> retVal = new ArrayList<Block>();
			
			int lastPos = 0;
			
			for (int i = 0; i < nodeList.size(); i++) {
				
				Node nd = nodeList.get(i);
				
				if (!(nd instanceof LiteralString))
					continue;
				
				LiteralString strNd = (LiteralString) nd;
				
				if (!strNd.value.contains(","))
					continue;
				
				if (strNd.value.equals(",")) {
					
					// Just a comma with nothing else, split on this node and skip it entirely
					
					Block blk = createBlock(nodeList, lastPos, i);
					
					retVal.add(blk);
					
					lastPos = i + 1;
					
					continue;
				}
				
				String[] parts = strNd.value.split(",");
				
				// First part goes back into strNd
				strNd.value = parts[0];
				
				int insertPos = i + 1;
				
				// Any remaining parts go into new LiteralString nodes
				for (int j = 1; j < parts.length; j++) {
					
					nodeList.add(insertPos, new LiteralString(",",
							strNd.getInputFile(), strNd.getLineNum(), strNd.getColNum()));
					
					insertPos++;
					
					nodeList.add(insertPos, new LiteralString(parts[j],
							strNd.getInputFile(), strNd.getLineNum(), strNd.getColNum()));
					
					insertPos++;
					
				}
				
				
				// Now try again
				i--;
				
			} // for each item
			
			if (lastPos < nodeList.size()) {
				
				Block blk = createBlock(nodeList, lastPos, nodeList.size());
				
				retVal.add(blk);
			}
			
			return retVal;
			
		}
		
		private Hashtable<String, Block> generateKeyValuePairs(List<String> expectedKeyNames, ArrayList<Node> nodeListToSplit) {
			
			Hashtable<String, Block> pgfKeyList = new Hashtable<String, Block>();
			
			if (nodeListToSplit.size() == 0)
				return pgfKeyList;
			
			ArrayList<Block> paramData = splitByComma(nodeListToSplit);
			
			// An input list that contains only an empty string will be split
			// into a block of no items (the empty string is removed)
			if (paramData.size() == 1 && paramData.get(0).values.size() == 0)
				return pgfKeyList;
			
			for (Block kvBlk : paramData) {
				
				if (kvBlk.values.size() < 1)
					throw new AnalysisException("Missing key=value pair", kvBlk);
				
				Node kNd = kvBlk.values.get(0);
				
				if (!(kNd instanceof LiteralString))
					throw new AnalysisException("Key must be a LiteralString", kNd);
				
				LiteralString kStrNd = (LiteralString) kNd;
				
				if (!kStrNd.value.contains("=")) {
					
					String keyName = kStrNd.value.trim();
					
					if (!expectedKeyNames.contains(keyName))
						throw new AnalysisException("Unexpected key name: " + keyName, kNd);
					
					pgfKeyList.put(keyName,
							new Block(kNd.getInputFile(),
									kNd.getLineNum(),
									kNd.getColNum()));
					
					continue;
				}
				
				String[] parts = kStrNd.value.split("=", 2);
				
				String keyName = parts[0].trim();
				
				if (!expectedKeyNames.contains(keyName))
					throw new AnalysisException("Unexpected key name: " + keyName, kNd);
				
				if (parts[1].length() > 0) {
					kStrNd.value = parts[1];
				}
				else {
					kvBlk.values.remove(0);
					
					if (kvBlk.values.size() < 1)
						throw new AnalysisException("Missing value after equals sign", kvBlk);
					
				}
				
				if (kvBlk.values.size() < 1)
					throw new AnalysisException("Missing value for key name: " + keyName, kvBlk);
				else if (kvBlk.values.size() > 1)
					throw new AnalysisException("Value must be a single string or Block for key name: " + keyName, kvBlk);
				
				Node vNd = kvBlk.values.get(0);
				
				Block vBlk;
				
				if (vNd instanceof LiteralString) {
					
					vBlk = new Block(vNd.getInputFile(), vNd.getLineNum(), vNd.getColNum());
					
					vBlk.values.add(vNd);
					
				}
				else if (vNd instanceof Block) {
					vBlk = (Block) vNd;
				}
				else
					throw new AnalysisException("Value must be a LiteralString or Block for key name: " + keyName, vNd);
				
				pgfKeyList.put(keyName, vBlk);
				
			}
			return pgfKeyList;
		}
		
		@Override
		protected Action beforeVisitEnvironment(Environment env) {
			
			List<String> params = environmentParameters.get(env.name);
			List<String> expectedKeyNames = environmentsWithPgfKeys.get(env.name);
			
			if (params == null && expectedKeyNames != null) {
				params = Arrays.asList("pgfkeys");
			}
			
			if (params != null) {
				
				for (String paramName : params) {
	
					// Move a parameter from the contents into the params list
					Block blk = getFirstBlock(env.content, 0);
					if (blk == null)
						throw new AnalysisException(env.name + " environment is missing parameter: " + paramName, env);
					
					env.addParameter(blk);
					//check for commands in the block
					traverseBlock(blk, env);
					
				}
				
			}
			
			if (expectedKeyNames != null) {
				
				Hashtable<String, Block> pgfKeyList = generateKeyValuePairs(expectedKeyNames, env.getParameter(0).values);
				
				env.addPgfParameters(pgfKeyList);
				
			}
			
			// Special case for enumerate, which is so far the only "environment" where
			// we want to parse the env.options Block like pgfkeys.  Note that because
			// this runs before ExtractEnumerate we don't have Enumerate object types yet
			// so we need to do the check here using the name.
			
			if (env.name.equals("enumerate") && env.options != null) {
				
				// For backwards compatibility we support an options field that is just
				// a string (Ex: "a." for alpha), but it can also be a key=value,... list
				
				if (env.options.values.size() == 1 && env.options.values.get(0) instanceof LiteralString &&
						!((LiteralString)env.options.values.get(0)).value.contains("=")) {
					
					env.addPgfParameter("label", createBlock(env.options.values, 0, env.options.values.size()));
					
				}
				else {
					
					expectedKeyNames = Arrays.asList("label", "start", "align", "labelwidth", "leftmargin", "topsep");
					
					Hashtable<String, Block> pgfKeyList = generateKeyValuePairs(expectedKeyNames, env.options.values);
					
					env.addPgfParameters(pgfKeyList);
					
				}
				
			}
			
			if (env.name.equals("htmlasimage"))
				return Action.SkipChildren;
			
			return Action.TraverseChildren;
		}

		@Override
		protected Action afterVisitEnvironment(Environment env) {
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitTabular(Tabular tab) {
			
			/*
			 *  tabu can have to special formats before the column spec argument:
			 *    begin{tabu} to units {columnspec}  --> Sets a max width
			 *    begin{tabu} spread units {columnspec} --> Adds to the min width
			 */
			if (tab.name.equals("tabu")) {
				
				Tabular.Cell cell = tab.getFirstCell();
				
				Node ndFirstChild = cell.getContent().values.get(0);
				
				if (ndFirstChild instanceof LiteralString) {
					
					LiteralString strNode = (LiteralString)ndFirstChild;
					String str = strNode.value;
					
					if (!TABU_WIDTH_RE.matcher(str).matches())
						throw new AnalysisException(tab.name + " environment has text before the column spec parameter, but it is not in the expected format", tab);
					
					
					// Grab everything up until the next block, which could include commands line \linewidth
					int pos = 0;
					while (pos < cell.getContent().values.size()) {
						
						Node nd = cell.getContent().values.get(pos);
						
						if (nd instanceof Block)
							break;
						
						pos++;
					}
					
					Block blk = new Block(strNode.getInputFile(), strNode.getLineNum(), strNode.getColNum());
					
					blk.values.addAll(cell.getContent().values.subList(0, pos));
					
					cell.getContent().values = new ArrayList<Node>(cell.getContent().values.subList(pos, cell.getContent().values.size()));
					
					tab.setTabuSizeExtension(blk);
					
				}
				
			}
			
			if (tab.name.equals("tabular") || tab.name.equals("longtable") || tab.name.equals("tabu") || tab.name.equals("longtabu")) {
				
				Tabular.Cell cell = tab.getFirstCell();
				
				// tabular has one parameter
				Block blk = getFirstBlock(cell.getContent(), 0);
				if (blk == null)
					throw new AnalysisException(tab.name + " environment is missing parameter: column spec", tab);
				
				tab.addParameter(blk);
				
			}
			else if (tab.name.equals("tabularx")) {
				
				Tabular.Cell cell = tab.getFirstCell();
				
				// tabularx has two parameters
				Block blk = getFirstBlock(cell.getContent(), 0);
				if (blk == null)
					throw new AnalysisException(tab.name + " environment is missing parameter: table width", tab);
				
				tab.addParameter(blk);
				
				blk = getFirstBlock(cell.getContent(), 0);
				if (blk == null)
					throw new AnalysisException(tab.name + " environment is missing parameter: column spec", tab);
				
				tab.addParameter(blk);
				
			}
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action afterVisitTabular(Tabular tab) {
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action beforeVisitCommand(Command cmd) {
			
			List<String> params = commandParameters.get(cmd.name);
			List<String> expectedKeyNames = commandsWithOptionKeys.get(cmd.name);
			
			if (params != null) {
				
				for (String paramName : params) {
	
					// Move a block from after this command in the parent to a parameter of this block
					Block blk = getFirstBlock(currentBlock.peek(), currentBlockPosition + 1);
					if (blk == null)
						throw new AnalysisException(cmd.name + " command is missing parameter: " + paramName, cmd);
					
					cmd.addParameter(blk);
				}		
			}
			
			if (expectedKeyNames != null && cmd.options != null) {
				
				Hashtable<String, Block> pgfKeyList = generateKeyValuePairs(expectedKeyNames, cmd.options.values);
				
				cmd.addOptionsKeyValues(pgfKeyList);
				
				cmd.options = null;
				
			}
			
			return Action.TraverseChildren;
		}
		
		@Override
		protected Action visitCommandNoParams(Command cmd) {
			
			String paramName = tabuCommandParameters.get(cmd.name);
			
			if (paramName != null) {
				
				// Take a literal string of the form "=..." and turn it into a parameter
				
				Block blk = currentBlock.peek();
				
				int nextPos = currentBlockPosition + 1;
				
				if (blk.values.size() <= nextPos)
					throw new AnalysisException(cmd.name + " command is missing parameter: " + paramName, cmd);
				
				Node nextNd = blk.values.get(nextPos);
				
				if (!(nextNd instanceof LiteralString))
					throw new AnalysisException(cmd.name + " command is missing equals sign before parameter", cmd);
				
				LiteralString strNd = (LiteralString)nextNd;
				
				if (!strNd.value.startsWith("="))
					throw new AnalysisException(cmd.name + " command is missing equals sign before parameter", cmd);
				
				// [ \n]+
				int endPos = strNd.value.indexOf(" ");
				if (endPos == -1) {
					
					endPos = strNd.value.indexOf("\n");
					if (endPos == -1)
						throw new AnalysisException(cmd.name + " command is missing parameter: " + paramName, cmd);
					
				}
				
				String paramValue = strNd.value.substring(1, endPos);
				
				cmd.setEqualsParameter(paramValue);
				
				if (strNd.value.length() == (endPos + 1)) {
					// No other text in the string, delete from block
					blk.values.remove(nextPos);
				}
				else {
					strNd.value = strNd.value.substring(endPos + 1);
				}
				
				return Action.TraverseChildren;
				
			}
			// \figure ... \endfigure in Guichard
			else if (cmd.name.equals("figure")) {
				
				// Search for \endfigure
				Block parentBlock = currentBlock.peek();
				
				int start = currentBlockPosition, end = -1;
				Command beginFigure = cmd, endFigure = null;
				
				for (int i = start + 1; i < parentBlock.values.size(); i++) {
					
					Node nd = parentBlock.values.get(i);
					if (nd instanceof Command) {
						Command parCmd = (Command)nd;
						if (parCmd.name.equals("figure")) {
							
							throw new AnalysisException("Found two \\figure commands", parCmd);
							
						}
						else if (parCmd.name.equals("endfigure")) {
							endFigure = parCmd;
							end = i;
							break;
						}
					}
				}
				
				if (beginFigure == null || endFigure == null)
					throw new AnalysisException("Unable to find matching \\figure and \\endfigure", cmd);
				
				Environment figureEnv = new Environment(beginFigure.getInputFile(),
						beginFigure.getLineNum(), beginFigure.getColNum());
				figureEnv.name = "figure";
				figureEnv.options = beginFigure.options;
				figureEnv.content = new Block(
						parentBlock.values.get(start+1).getInputFile(),
						parentBlock.values.get(start+1).getLineNum(),
						parentBlock.values.get(start+1).getColNum());
				
				figureEnv.content.values = new ArrayList<Node>(parentBlock.values.subList(start + 1, end));
				
				parentBlock.values.subList(start, end+1).clear();
				parentBlock.values.add(start, figureEnv);
				
				// Now we need to traverse the content!
				traverseBlock(figureEnv.content, figureEnv);
				
			}
			// \beginpicture ... \endpicture (Guichard)
			else if (cmd.name.equals("beginpicture")) {
				
				// Search for \endpicture
				Block parentBlock = currentBlock.peek();
				
				int start = currentBlockPosition, end = -1;
				Command beginPicture = cmd, endPicture = null;
				
				for (int i = start + 1; i < parentBlock.values.size(); i++) {
					
					Node nd = parentBlock.values.get(i);
					if (nd instanceof Command) {
						Command parCmd = (Command)nd;
						if (parCmd.name.equals("beginpicture")) {
							
							throw new AnalysisException("Found two \\beginpicture commands", parCmd);
							
						}
						else if (parCmd.name.equals("endpicture")) {
							endPicture = parCmd;
							end = i;
							break;
						}
					}
				}
				
				if (beginPicture == null || endPicture == null)
					throw new AnalysisException("Unable to find matching \\beginpicture and \\endpicture", cmd);
				
				Environment pictureEnv = new Environment(beginPicture.getInputFile(),
						beginPicture.getLineNum(), beginPicture.getColNum());
				pictureEnv.name = "beginpicture";
				pictureEnv.options = beginPicture.options;
				pictureEnv.content = new Block(parentBlock.values.get(start+1).getInputFile(),
						parentBlock.values.get(start+1).getLineNum(),
						parentBlock.values.get(start+1).getColNum());
				
				pictureEnv.content.values = new ArrayList<Node>(parentBlock.values.subList(start + 1, end));
				
				parentBlock.values.subList(start, end+1).clear();
				parentBlock.values.add(start, pictureEnv);
				
				// Now we need to traverse the content!
				traverseBlock(pictureEnv.content, pictureEnv);
			}
			// Tex-style command where arg is not in {}
			else if (cmd.name.equals("vskip")) {
				
				// Get the literal string after command
				Block parentBlock = currentBlock.peek();
				
				if (currentBlockPosition + 1 >= parentBlock.values.size())
					throw new AnalysisException("vskip has no LiteralString after it", cmd);
				
				Node nextNode = parentBlock.values.get(currentBlockPosition + 1);
				
				if (!(nextNode instanceof LiteralString))
					throw new AnalysisException("vskip has no LiteralString after it", cmd);
				
				LiteralString strNode = (LiteralString)nextNode;
				
				if (!strNode.value.startsWith(" "))
					throw new AnalysisException("String after vskip does not start with a space", cmd);
				
				Matcher m = Pattern.compile("^ -?[0-9]+[a-zA-Z]+").matcher(strNode.value);
				if (!m.find())
					throw new AnalysisException("String after vskip does not appear to be a length", cmd);
				
				String lengthStr = m.group();
				
				if (lengthStr.length() == strNode.value.length()) {
					parentBlock.values.remove(currentBlockPosition+1);
					currentBlockPosition--;
				}
				else {
					// TODO: Fix column number for strNode
					strNode.value = strNode.value.substring(lengthStr.length());
				}
				
				LiteralString newStrNode = new LiteralString(lengthStr.substring(1), strNode.getInputFile(),
						strNode.getLineNum(), strNode.getColNum() + 1);
				
				Block newBlk = new Block(strNode.getInputFile(),
						strNode.getLineNum(), strNode.getColNum());
				newBlk.values.add(newStrNode);
				
				cmd.addParameter(newBlk);
				
			}
			
			return Action.SkipChildren;
		}
		
		public void doWork(Block topLevelBlock) {
			traverseBlock(topLevelBlock, topLevelBlock);
		}
	
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		new Traverser().doWork(topLevelBlock);
	}

}
