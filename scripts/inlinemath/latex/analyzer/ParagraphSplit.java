package latex.analyzer;

import java.util.ArrayList;
import java.util.regex.Pattern;

import latex.node.Block;
import latex.node.Chapter;
import latex.node.Enumerate;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.MathMode;
import latex.node.Node;
import latex.node.Paragraph;
import latex.node.Part;
import latex.node.Section;
import latex.node.SubSection;

/**
 * Goes through parts, chapters, sections, and subsections, and splits all LiteralStrings by
 * \n\n, wrapping the paragraphs into Paragraph nodes.
 */
public class ParagraphSplit extends Analyzer {
	
	private static Pattern oneOrMoreBlankLinePattern = Pattern.compile("^([ \\t]*\\n)+$");
	
	private boolean isOneOrMoreBlankLines(String str) {
		return oneOrMoreBlankLinePattern.matcher(str).matches();
	}
	
	
	/**
	 * Within the given list of notes, split any literal strings into separate strings of:
	 * "pre content" + "\n\n" + "post content".  In other words, every literal string
	 * that contains \n\n will be split into 3 or more literal strings, containing
	 * the same content.
	 * 
	 * Note that ultimately this does not change the high-level content of the block, as
	 * a writer could just concatenate the literal strings and get the original content
	 * back.
	 */
	private ArrayList<Node> splitLiteralStringByNewlines(ArrayList<Node> nodeList) {
		
		ArrayList<Node> retVal = new ArrayList<Node>();
		
		for (int nodePos = 0; nodePos < nodeList.size(); nodePos++) {
			
			Node nd = nodeList.get(nodePos);
			
			if (!(nd instanceof LiteralString)) {
				retVal.add(nd);
				continue;
			}
			
			LiteralString strNode = (LiteralString)nd;
			
			// Counting line numbers within this node
			int lineOffset = 0;
			int lastSplitPos = 0;
			int lastLineOffset = 0;
			
			for (int charPos = 0; charPos < strNode.value.length(); charPos++) {
				
				if (strNode.value.charAt(charPos) != '\n')
					continue;
				
				lineOffset++;
				
				int newlineCount = 1;
				
				int checkPos = charPos + 1;
				while (checkPos < strNode.value.length()) {
					
					char c = strNode.value.charAt(checkPos);
					
					if (!Character.isWhitespace(c))
						break;
					
					if (c == '\n')
						newlineCount++;
					
					checkPos++;
					
				}
				
				// Looking for a blank line in between two paragraphs (ignoring indenting)
				if (newlineCount < 2)
					continue;
				
				if (checkPos != strNode.value.length()) {
					
					checkPos--; // To last whitespace char seen
					
					// Back up to last newline seen
					while (checkPos > charPos) {
						
						if (strNode.value.charAt(checkPos) == '\n')
							break;
						
						checkPos--;
						
					}
					
				}
				
				if (checkPos == charPos)
					throw new RuntimeException("Should never happen");
				
				
				if (charPos > 0) {
					String beforeSplit = strNode.value.substring(lastSplitPos, charPos);
					retVal.add(new LiteralString(beforeSplit, strNode.getInputFile(), strNode.getLineNum() + lastLineOffset, strNode.getColNum()));
				}
				
				retVal.add(new LiteralString("\n\n", strNode.getInputFile(), strNode.getLineNum() + lineOffset - 1, 0));
				
				// Skip over the paragraph break that we just completed
				charPos = checkPos + 1;
				lineOffset += newlineCount - 1;
				
				lastSplitPos = charPos;
				lastLineOffset = lineOffset;
				
			}
			
			if (lastSplitPos < strNode.value.length()) {
				
				// Add the last bit of this string
				String afterSplit = strNode.value.substring(lastSplitPos);
				retVal.add(new LiteralString(afterSplit, strNode.getInputFile(), strNode.getLineNum() + lastLineOffset, 0));
				
			}
			
			
			
		} // for each node
		
		return retVal;
	}
	
	/**
	 * Remove any whitespace at the beginning or end of a paragraph, to clean up the source a bit.
	 */
	private void trimParagraphWhitespace(Paragraph para) {
		
		// Remove any paragraph breaks "\n\n" that were added into the paragraph
		while (para.values.size() > 0) {
			
			Node nd = para.values.get(0);
			
			if (!(nd instanceof LiteralString))
				break;
			
			LiteralString strNode = (LiteralString)nd;
			
			if (!isOneOrMoreBlankLines(strNode.value))
				break;
			
			para.values.remove(0);
		}
		
		while (para.values.size() > 0) {
			
			Node nd = para.values.get(para.values.size() - 1);
			
			if (!(nd instanceof LiteralString))
				break;
			
			LiteralString strNode = (LiteralString)nd;
			
			if (!isOneOrMoreBlankLines(strNode.value))
				break;
			
			para.values.remove(para.values.size() - 1);
		}
		
		if (para.values.size() < 1)
			return;
		
		Node firstNd = para.values.get(0);
		
		if (firstNd instanceof LiteralString) {
			String str = ((LiteralString)firstNd).value.replaceAll("^[ \\n\\t]+", "");
			if (str.length() == 0)
				para.values.remove(0);
			else
				((LiteralString)firstNd).value = str;
		}
		
		if (para.values.size() < 1)
			return;
		
		Node lastNd = para.values.get(para.values.size() - 1);
		
		if (lastNd instanceof LiteralString) {
			String str = ((LiteralString)lastNd).value.replaceAll("[ \\n\\t]+$", "");
			if (str.length() == 0)
				para.values.remove(para.values.size() - 1);
			else
				((LiteralString)lastNd).value = str;
		}
		
	}
	
	/**
	 * Given a list of notes returned from splitLiteralStringByNewlines(), wrap literal strings
	 * into Paragraph nodes.  Literal strings that are exactly "\n\n" are breakpoints that define
	 * the beginning and end of paragraphs.  Environment nodes (like tabular, etc) also act to break
	 * paragraphs, but "normal" commands are kept inline in its paragraph.
	 */
	private ArrayList<Node> splitByNLOrEnvironments(ArrayList<Node> nodeList) {
		
		ArrayList<Node> retVal = new ArrayList<Node>();
		
		if (nodeList.size() == 0)
			return retVal;
		
		int lastPos = 0;
		
		for (int pos = 1; pos < nodeList.size(); pos++) {
			
			Node nd = nodeList.get(pos);
			
			if (nd instanceof LiteralString) {
				
				LiteralString strNode = (LiteralString)nd;
				
				if (isOneOrMoreBlankLines(strNode.value)) {
					
					createParagraph(nodeList, lastPos, pos, retVal);
					
					lastPos = pos + 1;
					
				}
				
			} // is a literal string
			else if (nd instanceof Environment) {
				
				Environment envNode = (Environment)nd;
				
				createParagraph(nodeList, lastPos, pos, retVal);
				
				retVal.add(envNode);
				
				lastPos = pos + 1;
				
			}
			else if (nd instanceof Enumerate) {
				
				Enumerate enNode = (Enumerate)nd;
				
				createParagraph(nodeList, lastPos, pos, retVal);
				
				retVal.add(enNode);
				
				lastPos = pos + 1;
				
			}
			else if (nd instanceof MathMode) {
				
				MathMode mmNode = (MathMode)nd;
				
				// For inline styles, just continue on so these will be added inside a paragraph
				if (mmNode.getStyle() == MathMode.Style.DOLLAR || mmNode.getStyle() == MathMode.Style.SLASH_PAREN)
					continue;
				
				createParagraph(nodeList, lastPos, pos, retVal);
				
				retVal.add(mmNode);
				
				lastPos = pos + 1;
				
			}
			else if (nd instanceof Section) {
				// Don't wrap subsections into paragraphs, but recursively call down to split their contents
				
				Section secNode = (Section)nd;
				
				secNode.getContent().values = splitIntoParagraphs(secNode.getContent().values);
				
				createParagraph(nodeList, lastPos, pos, retVal);
				
				retVal.add(secNode);
				
				lastPos = pos + 1;
			}
			else if (nd instanceof SubSection) {
				// Don't wrap subsections into paragraphs, but recursively call down to split their contents
				
				SubSection subsecNode = (SubSection)nd;
				
				subsecNode.getContent().values = splitIntoParagraphs(subsecNode.getContent().values);
				
				createParagraph(nodeList, lastPos, pos, retVal);
				
				retVal.add(subsecNode);
				
				lastPos = pos + 1;
			}
			else if (nd instanceof Paragraph) {
				throw new AnalysisException("Paragraph splitting appears to have been called twice.", nd);
			}
			
		} // for each item
		
		if (lastPos < nodeList.size()) {
			
			if (nodeList.size() == 1 && (nodeList.get(0) instanceof Environment ||
					nodeList.get(0) instanceof Enumerate || nodeList.get(0) instanceof MathMode)) {
				retVal.add(nodeList.get(0));
			}
			else {
				
				createParagraph(nodeList, lastPos, nodeList.size(), retVal);
				
			}
		}
		
		return retVal;
	}

	/**
	 * Copy sub-list of nodes from nodeList into a new Paragraph node, trimming extra whitespace.  If
	 * the nodes in fromPos to toPos are just whitespace, no Paragraph is added to the result list.
	 * 
	 * @param nodeList The list to extract nodes from
	 * @param fromPos Inclusive
	 * @param toPos Exclusive
	 * @param resultList The list to append the resulting Paragraph to
	 * @returns
	 */
	private void createParagraph(ArrayList<Node> nodeList, int fromPos, int toPos, ArrayList<Node> resultList) {
		
		if (fromPos == toPos)
			return;
		
		ArrayList<Node> subList = new ArrayList<Node>(nodeList.subList(fromPos, toPos));
		
		boolean allWhitespace = true;
		for (Node nd : subList) {
			
			if (!(nd instanceof LiteralString)) {
				allWhitespace = false;
				break;
			}
			
			LiteralString strNode = (LiteralString)nd;
			
			if (!isOneOrMoreBlankLines(strNode.value)) {
				allWhitespace = false;
				break;
			}
			
		}
		
		if (allWhitespace)
			return;
		
		Paragraph para = new Paragraph(nodeList.get(fromPos).getInputFile(),
				nodeList.get(fromPos).getLineNum(),
				nodeList.get(fromPos).getColNum()); 
		
		para.values = subList;
		
		trimParagraphWhitespace(para);
		
		if (para.values.size() < 1)
			return;
		
		resultList.add(para);
		
	}
	
	public ArrayList<Node> splitIntoParagraphs(ArrayList<Node> nodeList) {
		
		ArrayList<Node> retVal;
		
		retVal = splitLiteralStringByNewlines(nodeList);
		
		retVal = splitByNLOrEnvironments(retVal);
		
		// TODO: Traverse down to environments and split their contents into paragraphs too?
		//       But only if the environment contains more than one paragraph (based on \n\n or Env nodes)
		
		return retVal;
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		
		Environment document = getDocumentEnvironment(topLevelBlock);
		
		for (int i = 0; i < document.content.values.size(); i++) {
			
			Node nd = document.content.values.get(i);
			
			if (nd instanceof Chapter) {
				
				Chapter chap = (Chapter)nd;
				
				chap.getContent().values = splitIntoParagraphs(chap.getContent().values);
				
			}
			else if (nd instanceof Part) {
				
				Part pt = (Part)nd;
				
				pt.getContent().values = splitIntoParagraphs(pt.getContent().values);
				
			}
			
		}
		
	}

}
