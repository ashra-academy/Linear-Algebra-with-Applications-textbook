package latex.analyzer;

import java.util.ArrayList;
import java.util.ListIterator;

import latex.node.Chapter;
import latex.node.Command;
import latex.node.Block;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.Node;
import latex.node.Part;
import latex.node.Section;
import latex.node.SubSection;

/**
 * Given a document environment which is just one flat list of Nodes, splits the nodes up into
 * Chapter, Section, and Subsection nodes, applying the remaining nodes as children where appropriate.
 * As this is done, chapters, sections, and subsections are numbered the same way that the Latex
 * compiler would do.
 * 
 */
public class ChapterSectionSplit extends Analyzer {
	
	/**
	 * Iterates through all of the Blocks found by splitBy(), and if the first Command
	 * is a chapter, then it creates a Chapter node.  This automatically numbers the
	 * chapters, with unnumbered chapters set to -1.
	 * 
	 * Anything that does not start with \chapter is returned as-is.
	 * 
	 * The returned list is a mix of Blocks (from splitBy()) and Chapter nodes.
	 */
	private ArrayList<Node> createChapterNodes(ArrayList<Block> rawChapterList, int chapterNumberStart) {
		ArrayList<Node> chapterList = new ArrayList<Node>();
		
		int partNum = 1;
		int chapterNum = chapterNumberStart;
		
		for (Block blk : rawChapterList) {
			
			Node firstNode = blk.values.get(0);
			
			if (firstNode instanceof Command) {
				
				Command cmdNode = (Command)firstNode;
				
				if (cmdNode.name.equals("chapter") || cmdNode.name.equals("chapter*")) {
					
					// Remove the chapter command
					blk.values.remove(0);
					
					int assignedChapter = -1;
					if (!cmdNode.name.endsWith("*")) {
						assignedChapter = chapterNum;
						chapterNum++;
					}
					
					Chapter ch = new Chapter(assignedChapter, cmdNode.getParameter(0), blk,
							cmdNode.getInputFile(), cmdNode.getLineNum(), cmdNode.getColNum());

					// Add Chapter object
					chapterList.add(ch);
					
				}
				else if (cmdNode.name.equals("part") || cmdNode.name.equals("part*")) {
					
					// Remove the part command
					blk.values.remove(0);
					
					int assignedPart = -1;
					if (!cmdNode.name.endsWith("*")) {
						assignedPart = partNum;
						partNum++;
					}
					
					Part ch = new Part(assignedPart, false, cmdNode.getParameter(0), blk,
							cmdNode.getInputFile(), cmdNode.getLineNum(), cmdNode.getColNum());

					// Add Part object
					chapterList.add(ch);
					
					chapterNum = 1;
					
				}
				else if (cmdNode.name.equals("continuouspart") || cmdNode.name.equals("continuouspart*")) {
					
					// Remove the part command
					blk.values.remove(0);
					
					int assignedPart = -1;
					if (!cmdNode.name.endsWith("*")) {
						assignedPart = partNum;
						partNum++;
					}
					
					Part ch = new Part(assignedPart, true, cmdNode.getParameter(0), blk,
							cmdNode.getInputFile(), cmdNode.getLineNum(), cmdNode.getColNum());

					// Add Part object
					chapterList.add(ch);
					
					// Don't reset chapter counter
					
				}
				else {
					chapterList.add(blk);
				}
				
			}
			
		}
		return chapterList;
	}
	
	/**
	 * Iterates through all of the Blocks found by splitBy(), and if the first Command
	 * is a section, then it creates a Section node.  This does not number the Sections,
	 * but instead sets the value to 0 if a number should be assigned, and -1 for unnumbered
	 * sections.
	 * 
	 * Anything that does not start with \section is returned as-is.
	 * 
	 * The returned list is a mix of Blocks (from splitBy()) and Section nodes.
	 */
	private ArrayList<Node> createSectionNodes(ArrayList<Block> rawSectionList) {
		
		ArrayList<Node> sectionList = new ArrayList<Node>();
		
		for (Block blk : rawSectionList) {
			
			Node firstNode = blk.values.get(0);
			
			if (firstNode instanceof Command) {
				
				Command cmdNode = (Command)firstNode;
				
				if (cmdNode.name.equals("section")) {
					
					// Remove the section command
					blk.values.remove(0);
					
					Section ch = new Section(0, cmdNode.getParameter(0), blk,
							cmdNode.getInputFile(), cmdNode.getLineNum(), cmdNode.getColNum());
					
					sectionList.add(ch);
					
				}
				else if (cmdNode.name.equals("section*")) {
					
					// Remove the section command
					blk.values.remove(0);
					
					Section ch = new Section(-1, cmdNode.getParameter(0), blk,
							cmdNode.getInputFile(), cmdNode.getLineNum(), cmdNode.getColNum());
					
					sectionList.add(ch);
				}
				else {
					sectionList.add(blk);
				}
				
			}
			else {
				sectionList.add(blk);
			}
			
		}
		
		return sectionList;
	}
	
	/**
	 * Iterates through all of the Blocks found by splitBy(), and if the first Command
	 * is a subsection, then it creates a SubSection node.  This does not number the SubSections,
	 * but instead sets the value to 0 if a number should be assigned, and -1 for unnumbered
	 * sections.
	 * 
	 * Anything that does not start with \subsection is returned as-is.
	 * 
	 * The returned list is a mix of Blocks (from splitBy()) and SubSection nodes.
	 */
	private static ArrayList<Node> createSubSectionNodes(ArrayList<Block> rawSubSectionList) {
		
		ArrayList<Node> subSectionList = new ArrayList<Node>();
		
		for (Block blk : rawSubSectionList) {
			
			Node firstNode = blk.values.get(0);
			
			if (firstNode instanceof Command) {
				
				Command cmdNode = (Command)firstNode;
				
				if (cmdNode.name.equals("subsection")) {
					
					// Remove the section command
					blk.values.remove(0);
					
					SubSection ch = new SubSection(0, cmdNode.getParameter(0), blk,
							cmdNode.getInputFile(), cmdNode.getLineNum(), cmdNode.getColNum());
					
					subSectionList.add(ch);
					
				}
				else if (cmdNode.name.equals("subsection*")) {
					
					// Remove the section command
					blk.values.remove(0);
					
					SubSection ch = new SubSection(-1, cmdNode.getParameter(0), blk,
							cmdNode.getInputFile(), cmdNode.getLineNum(), cmdNode.getColNum());
					
					subSectionList.add(ch);
				}
				else {
					subSectionList.add(blk);
				}
				
			}
			else {
				subSectionList.add(blk);
			}
			
		}
		
		return subSectionList;
	}
	
	/**
	 * This method iterates through all elments of subSectionList.  It numbers the SubSections,
	 * and attaches them to secNode. If secNode is unnumbered and a SubSection is numbered, then
	 * this attaches the remaining SubSections to lastNumberedSection, because this how Latex works.
	 * 
	 * Numbering of the SubSections starts from startSubsectionNumber, which the caller will reset
	 * back to 1 for every numbered Section.  Otherwise, this can be used to keep numbering
	 * subsections in the case that they are re-attached to lastNumberedSection.
	 * 
	 * This also flattens out any other Blocks to merge all children back into the main contents of
	 * the Section.
	 * 
	 * @return The next value for startSubsectionNumber, so it can be saved and passed into a new
	 *         call of this method if needed.
	 */
	private static int attachSubSectionsIntoSection(Section secNode, ArrayList<Node> subSectionList,
			Section lastNumberedSection, int startSubsectionNumber) {
		
		int subSectionNumber = startSubsectionNumber;
		
		ArrayList<Node> parentSectionBlock = new ArrayList<Node>();
		
		for (Node nd : subSectionList) {
			
			if (!(nd instanceof SubSection)) {
				// Must be a block
				Block blk = (Block)nd;
				parentSectionBlock.addAll(blk.values);
				continue;
			}
			
			SubSection subSec = (SubSection)nd;
			
			// Numbered subsection in unnumbered section
			if (subSec.getNumber() == 0 && secNode.getNumber() == -1) {
				
				if (lastNumberedSection == null)
					throw new AnalysisException("Numbered subsection but no numbered sections?", subSec);
				
				// Finish up the current Section
				secNode.getContent().values = parentSectionBlock;
				
				// And move back to the previous numbered section to keep going
				secNode = lastNumberedSection;
				parentSectionBlock = secNode.getContent().values;
			}
			
			if (subSec.getNumber() == 0) {
				subSec.setNumber(subSectionNumber);
				subSectionNumber++;
			}
			
			parentSectionBlock.add(subSec);
			
		} // for each node in the section
		
		// Set updated data as parent section's contents
		secNode.getContent().values = parentSectionBlock;
		
		return subSectionNumber;
	}
	
	/**
	 * Goes through all of the sections in the list and splits out SubSections,
	 * numbers them, and rearranges them to match with the correct (numbered) parent
	 * Section as Latex does.
	 */
	private void formatSubSections(ArrayList<Node> sectionList) {
		
		Section lastNumberedSection = null;
		int subSectionNumber = 0;
		
		for (Node nd : sectionList) {
			
			if (!(nd instanceof Section)) {
				continue;
			}
			
			Section secNode = (Section)nd;
			
			// If this is an numbered section, then reset the subsection numbers
			if (secNode.getNumber() == 0) {
				lastNumberedSection = secNode;
				subSectionNumber = 1;
			}
			
			// Split into subsections
			ArrayList<Block> rawSubSectionList = splitBy(secNode.getContent().values, "subsection");
			
			// Create section nodes
			ArrayList<Node> subSectionList = createSubSectionNodes(rawSubSectionList);
			
			// Create a merged list of the SubSection and other Block contents
			// This may merge some of the subSectionList into lastNumberedSection
			subSectionNumber = attachSubSectionsIntoSection(secNode, subSectionList, lastNumberedSection, subSectionNumber);
			
		} // for each node in the chapter
		
	}
	
	/**
	 * This method iterates through all elments of sectionList.  It numbers the Sections,
	 * and attaches them to chNode. If chNode is unnumbered and a Section is numbered, then
	 * this attaches the remaining Sections to lastNumberedChapter, because this how Latex works.
	 * 
	 * Numbering of the Sections starts from startSectionNumber, which the caller will reset
	 * back to 1 for every numbered Chapter.  Otherwise, this can be used to keep numbering
	 * sections in the case that they are re-attached to lastNumberedChapter.
	 * 
	 * This also flattens out any other Blocks to merge all children back into the main contents of
	 * the Chapter.
	 * 
	 * @return The next value for startSectionNumber, so it can be saved and passed into a new
	 *         call of this method if needed.
	 */
	private int attachSectionsIntoChapter(Chapter chNode, ArrayList<Node> sectionList,
			Chapter lastNumberedChapter, int startSectionNumber) {
		
		int sectionNumber = startSectionNumber;
		
		ArrayList<Node> parentChapterBlock = new ArrayList<Node>();
		
		for (Node nd : sectionList) {
			
			if (!(nd instanceof Section)) {
				// Must be a block
				Block blk = (Block)nd;
				parentChapterBlock.addAll(blk.values);
				continue;
			}
			
			Section secNode = (Section)nd;
			
			// Numbered section in unnumbered chapter
			if (secNode.getNumber() == 0 && chNode.getNumber() == -1) {
				
				if (lastNumberedChapter == null)
					throw new AnalysisException("Numbered section but no numbered chapters?", secNode);
				
				// Finish up the current Chapter
				chNode.getContent().values = parentChapterBlock;
				
				// And move back to the previous numbered chapter to keep going
				chNode = lastNumberedChapter;
				parentChapterBlock = chNode.getContent().values;
			}
			
			if (secNode.getNumber() == 0) {
				secNode.setNumber(sectionNumber);
				sectionNumber++;
			}
			
			parentChapterBlock.add(secNode);
			
		} // for each node in the section
		
		// Set updated data as parent section's contents
		chNode.getContent().values = parentChapterBlock;
		
		return sectionNumber;
	}
	
	/**
	 * Goes through all of the chapters in the list and splits out Sections,
	 * numbers them, and rearranges them to match with the correct (numbered) parent
	 * Chapter as Latex does.
	 */
	private void formatSections(ArrayList<Node> chapterList) {
		
		Chapter lastNumberedChapter = null;
		int sectionNumber = 0;
		
		for (Node nd : chapterList) {
			
			if (!(nd instanceof Chapter)) {
				continue;
			}
			
			Chapter chNode = (Chapter)nd;
			
			// If this is an numbered chapter, then reset the subsection numbers
			if (chNode.getNumber() > 0 || chNode.getTitle().toLatexString().contains("About")) {
				lastNumberedChapter = chNode;
				sectionNumber = 1;
			}
			
			// Split into sections
			ArrayList<Block> rawSectionList = splitBy(chNode.getContent().values, "section");
			
			// Create section nodes
			ArrayList<Node> sectionList = createSectionNodes(rawSectionList);
			
			formatSubSections(sectionList);
			
			sectionNumber = attachSectionsIntoChapter(chNode, sectionList, lastNumberedChapter, sectionNumber);
			
		} // for each node in the chapter
		
	}
	
	@Override
	public void doAnalysis(Block topLevelBlock) {
		
		Environment document = getDocumentEnvironment(topLevelBlock);
		
		
		// Determine starting chapter number
		int chapterNumberStart = 1;
		
		for (ListIterator<Node> ndIt = topLevelBlock.values.listIterator(); ndIt.hasNext(); ) {
			
			Node nd = ndIt.next();
			
			if (!(nd instanceof Command))
				continue;
				
			Command cmdNode = (Command)nd;
			
			if (!cmdNode.name.equals("setcounter"))
				continue;
				
			Block param = cmdNode.getParameter(0);
			
			if (param.values.size() != 1 || !(param.values.get(0) instanceof LiteralString)) {
				continue;
			}
			
			LiteralString strNode = (LiteralString)param.values.get(0);
			
			if (!"chapter".equals(strNode.value))
				continue;
			
			param = cmdNode.getParameter(1);
			
			if (param.values.size() != 1 || !(param.values.get(0) instanceof LiteralString)) {
				continue;
			}
			
			strNode = (LiteralString)param.values.get(0);
			
			// The first chapter number will be 1 more than this value
			chapterNumberStart = Integer.parseInt(strNode.value) + 1;
				
		}
		
		
		// Split into chapters
		ArrayList<Block> rawChapterList = splitBy(document.content.values, "chapter", "part", "continuouspart");
		
		// Create chapter nodes
		// This is a list of Chapter or Blocks
		ArrayList<Node> chapterList = createChapterNodes(rawChapterList, chapterNumberStart);
		
		formatSections(chapterList);
		
		for (int i = 0; i < chapterList.size(); i++) {
			
			Node nd = chapterList.get(i);
			
			if (!(nd instanceof Chapter || nd instanceof Part)) {
				// Merge the contents of the block back into the parent document
				Block blk = (Block)nd;
				chapterList.addAll(i, blk.values);
				i += blk.values.size();
			}
			
			// else, leave the Chapter as it is
			
		} // for each chapter
		
		// Create a new document block with the updated contents
		
		document.content = new Block(document.getInputFile(), document.getLineNum(), document.getColNum());
		document.content.values = chapterList;
	}

}
