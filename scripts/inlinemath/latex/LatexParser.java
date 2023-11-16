package latex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EmptyStackException;
import java.util.Stack;

import latex.node.Block;
import latex.node.Command;
import latex.node.Comment;
import latex.node.Environment;
import latex.node.LiteralString;
import latex.node.MathMode;
import latex.node.Node;
import latex.node.Tabular;

public class LatexParser {

	private static class EOFException extends IOException {

	}

	/**
	 * The various states or contexts that the parser is in.
	 */
	public static enum State {
		/** We've seen a slash, so now we decide on commands, escapes, etc. */
		SLASH,
		/** We're reading a command name in (possibly a \begin or \end). */
		COMMAND_NAME,
		/** We've seen a \begin{ and now we're reading the name in. */
		BEGIN_ENVIRONMENT_NAME,
		/** We've seen a \end{ and now we're reading the name in. */
		END_ENVIRONMENT_NAME,
		/** We're in the options of the command [...] */
		COMMAND_OPTIONS,
		/**
		 * For a regular \command{...}, this is the part in braces just after the \command part.
		 * For a \begin...\end environment, this is the body of the environment
		 */
		COMMAND_PARAMS_BLOCK,
		/** In a block of text covered by {...}, but not related to a command or environment argument. */
		BLOCK,
		/** In a comment. */
		COMMENT,
		/** Math mode. Acts a bit like BLOCK type but there are different parsing rules */
		MATHMODE,
		/** Like SLASH, but accepts slightly different characters. */
		MATHMODE_SLASH,
		/** tabular environment. */
		TABULAR
	}

	/** The next character in the peek buffer or null if the buffer is empty. */
	private Character nextChar;
	/** The line number where the last character was read from. */
	private int lineNum;
	/** The column number where the last character was read from. */
	private int colNum;
	/** The file that we are parsing. */
	private File inputFile;
	/** The file reader that is open for reading input from. */
	private BufferedReader fileReader;

	// Internal variables for doParse file that we might want to grab on error conditions...

	/** A stack of States, showing the nesting of commands, envinronments, etc. */
	private Stack<State> stateStack;
	/**
	 * A parallel stack to stateStack, holding the actual nodes as they are being parsed.
	 * The result of parsing will be one entry left on the top, a LatexBlock with the top-level
	 * commands of the file.
	 */
	private Stack<Node> nd;
	/** A buffer when reading characters from, such as a command name, literal string, etc. */
	private StringBuffer str;

	/**
	 * Create a new parser to parse the given file.
	 * 
	 * @param inputFile
	 */
	public LatexParser(File inputFile) {
		this.inputFile = inputFile;
		this.fileReader = null;
	}

	/**
	 * Create a new parser to parse the given file.
	 * 
	 * @param inputFile
	 */
	public LatexParser(BufferedReader inputReader) {
		this.inputFile = null;
		this.fileReader = inputReader;
	}

	private char peek() throws IOException {

		if (nextChar != null) {
			return nextChar.charValue();
		}

		int i = fileReader.read();
		if (i == -1)
			throw new EOFException();

		if ((char)i == '\r') {
			i = fileReader.read();
			if (i == -1)
				throw new EOFException();
		}

		nextChar = Character.valueOf((char)i);

		return nextChar.charValue();
	}

	private char next() throws IOException {

		if (nextChar != null) {
			char c = nextChar.charValue();

			if (c == '\n') {
				lineNum++;
				colNum = 1;
			}
			else {
				colNum++;
			}

			nextChar = null;
			return c;
		}

		int i = fileReader.read();
		if (i == -1)
			throw new EOFException();

		if ((char)i == '\r') {
			i = fileReader.read();
			if (i == -1)
				throw new EOFException();
		}

		char c = (char)i;
		if (c == '\n') {
			lineNum++;
			colNum = 1;
		}
		else {
			colNum++;
		}

		return c;
	}

	/**
	 * Append nodeToAppend onto the parent as needed.  This is used when we have finished parsing down in
	 * a level of blocks and now need to add that to the parent, but the parent can be one of various types
	 * of nodes.  This checks the type and appends it to the right part of the parent depending on
	 * what parentState tells us.
	 */
	private void appendToParent(Node parentNode, State parentState, Node nodeToAppend) throws LatexParserException {

		if (parentState == State.BLOCK) {
			if (!(parentNode instanceof Block))
				throw new LatexParserException("State was " + parentState + " but node was " + parentNode.getClass().getName(), getInputFile(), getLineNum(), getColNum());
			((Block)parentNode).values.add(nodeToAppend);
		}
		else if (parentState == State.COMMAND_OPTIONS) {
			if (parentNode instanceof Command)
				((Command)parentNode).options.values.add(nodeToAppend);
			else if (parentNode instanceof Environment)
				((Environment)parentNode).options.values.add(nodeToAppend);
			else
				throw new LatexParserException("State was " + parentState + " but node was " + parentNode.getClass().getName(), getInputFile(), getLineNum(), getColNum());
		}
		else if (parentState == State.COMMAND_PARAMS_BLOCK) {

			if (parentNode instanceof Command)
				((Command)parentNode).appendToFirstParameterBlock(nodeToAppend);
			else if (parentNode instanceof Environment)
				((Environment)parentNode).content.values.add(nodeToAppend);
			else if (parentNode instanceof Tabular)
				throw new LatexParserException("State was " + parentState + " but node was " + parentNode.getClass().getName(), getInputFile(), getLineNum(), getColNum());
		}
		else if (parentState == State.TABULAR) {
			if (!(parentNode instanceof Tabular))
				throw new LatexParserException("State was " + parentState + " but node was " + parentNode.getClass().getName(), getInputFile(), getLineNum(), getColNum());
			((Tabular)parentNode).appendToLastCell(nodeToAppend);
		}
		else if (parentState == State.MATHMODE) {
			if (!(parentNode instanceof Block))
				throw new LatexParserException("State was " + parentState + " but node was " + parentNode.getClass().getName(), getInputFile(), getLineNum(), getColNum());
			((Block)parentNode).values.add(nodeToAppend);
		}
		else {
			throw new LatexParserException("Not implemented: " + parentState + " / " + parentNode.getClass().getName() + " / " + nodeToAppend.getClass().getName(),
					getInputFile(), getLineNum(), getColNum());
		}

	}

	// Helper methods where we can do some easy debugging about the parse tree as it is building...

	private void pushState(State st) {
		stateStack.push(st);
	}

	private State popState() {
		State st = stateStack.pop();
		return st;
	}

	private void pushNode(Node ndd) {
		nd.push(ndd);
	}

	private Node popNode() {
		Node ndd = nd.pop();
		return ndd;
	}

	/**
	 * Parse the file and return the top level LatexBlock with the commands in the file.
	 */
	public Block parse() throws LatexParserException {

		stateStack = new Stack<LatexParser.State>();
		nd = new Stack<Node>();

		// We start with a top-level block
		pushState(State.BLOCK);
		nd.add(new Block(getInputFile(), getLineNum(), getColNum()));

		str = new StringBuffer();

		try {

			if (fileReader == null)
				fileReader = new BufferedReader(new FileReader(getInputFile()));
			
			nextChar = null;
			lineNum = 1;
			colNum = 1;

			try {

				while (true) {

					switch (stateStack.peek()) {

					case COMMAND_OPTIONS:
					case COMMAND_PARAMS_BLOCK:
					case BLOCK:
					case TABULAR:

						stateBlockOrOptions();

						break;

					case SLASH:

						stateSlash();

						break;

					case COMMAND_NAME:

						stateCommandName();

						break;

					case BEGIN_ENVIRONMENT_NAME:

						stateBeginEnvironment();

						break;

					case END_ENVIRONMENT_NAME:

						stateEndEnvironment();

						break;

					case COMMENT:

						stateComment();

						break;

					case MATHMODE:

						stateMathMode();

						break;

					case MATHMODE_SLASH:

						stateMathModeSlash();

						break;

					}

				}

				// Will never get here

			}
			catch (EOFException ex) {

				// There should be one item left: BLOCK state and a LatexBlock node

				if (stateStack.size() > 1) {

					State st = stateStack.pop();
					if (stateStack.size() == 1 && st == State.COMMAND_NAME) {

						// EOF after a command, so simply clean it up
						Command topNd = (Command)popNode();
						topNd.name = str.toString();

						if (topNd.name.length() == 1)
							throw new LatexParserException("Empty name for command", getInputFile(), getLineNum(), getColNum());

						str.delete(0, str.length());

						// popState already done

						appendToParent(nd.peek(), stateStack.peek(), topNd);

					}
					else if (stateStack.size() == 1 && st == State.COMMENT) {
						
						// Comment that is missing newline at end of file
						
						str.delete(0, str.length());
						
						// popState already done
						
						appendToParent(nd.peek(), stateStack.peek(), new Comment(str.toString(), getInputFile(), getLineNum(), getColNum()));

						
					}
					else
						throw new LatexParserException("State stack not empty: " + stateStack.toString(), getInputFile(), getLineNum(), getColNum());

				}

				if (nd.size() > 1)
					throw new LatexParserException("Node stack not empty: " + nd.toString(), getInputFile(), getLineNum(), getColNum());

				// Raw text to add to the end of the block
				if (str.length() > 0) {
					appendToParent(nd.peek(), stateStack.peek(), new LiteralString(str.toString(),
							getInputFile(), getLineNum(), getLineNum()));
					str.delete(0, str.length());
				}

				Node topNode = popNode();

				if (!(topNode instanceof Block))
					throw new LatexParserException("Top-level node is not a LatexBlock: " + topNode.getClass().toString(), getInputFile(), getLineNum(), getColNum());

				return (Block)topNode;
			}
			finally {
				try {
					fileReader.close();
				}
				catch (IOException ex) { }
			}
		}
		catch (IOException ex) {
			throw new LatexParserException("IOException while parsing", ex, getInputFile(), getLineNum(), getColNum());
		}
		catch (EmptyStackException ex) {
			throw new LatexParserException("Empty stack while parsing", ex, getInputFile(), getLineNum(), getColNum());
		}
	}

	private void stateBlockOrOptions() throws IOException, LatexParserException {
		
		char c = next();

		if (c == '%') {

			flushLiteralText();
			pushState(State.COMMENT);

		}
		else if (c == '\\') {
			pushState(State.SLASH);
		}
		else if (c == '$') {

			flushLiteralText();

			pushNode(new MathMode(MathMode.Style.DOLLAR, getInputFile(), getLineNum(), getColNum()));
			pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
			pushState(State.MATHMODE);
		}
		else if (c == '{') {

			flushLiteralText();

			pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
			pushState(State.BLOCK);

		}
		else if (stateStack.peek() == State.TABULAR && c == '&') {

			flushLiteralText();

			((Tabular)nd.peek()).addCell(getInputFile(), getLineNum(), getColNum());

		}
		else if (stateStack.peek() == State.COMMAND_OPTIONS && c == ']') {

			flushLiteralText();

			char cnext = peek();

			Node prevNd = nd.peek();
			if (prevNd instanceof Tabular) {

				popState();
				pushState(State.TABULAR);

			}
			else if (prevNd instanceof Environment) {
				// Params block always comes next.  If there is a {, then it is unrelated to this case
				// TODO: Not quite true, the { could be a parameter of the environment
				// TODO: What we should do is merge this with the if below.  Then add a new state for ENV_CONTENT when we get to that
				popState();

				((Environment)prevNd).content = new Block(getInputFile(), getLineNum(), getColNum());

				pushState(State.COMMAND_PARAMS_BLOCK);
			}
			else if (cnext == '{') {
				next();
				popState();

				if (prevNd instanceof Command) {
					((Command)prevNd).addParameter(new Block(getInputFile(), getLineNum(), getColNum()));
				}

				pushState(State.COMMAND_PARAMS_BLOCK);
			}
			else {
				Node topNd = popNode();

				popState();

				appendToParent(nd.peek(), stateStack.peek(), topNd);
			}

		}
		else if ((stateStack.peek() == State.BLOCK || stateStack.peek() == State.COMMAND_PARAMS_BLOCK) && c == '}') {

			flushLiteralText();

			// TODO: Remove me?
			// Preserve empty blocks "{}"
			if (stateStack.peek() == State.BLOCK && ((Block)nd.peek()).values.size() == 0) {
				appendToParent(nd.peek(), stateStack.peek(), new LiteralString("", getInputFile(), getLineNum(), getColNum()));
			}
			else if (stateStack.peek() == State.COMMAND_PARAMS_BLOCK) {

				Node topNd = nd.peek();

				if (topNd instanceof Command && ((Command)topNd).getParameter(0).values.size() == 0) {
					appendToParent(nd.peek(), stateStack.peek(), new LiteralString("", getInputFile(), getLineNum(), getColNum()));
				}
				else if (topNd instanceof Environment && ((Environment)topNd).content.values.size() == 0) {
					appendToParent(nd.peek(), stateStack.peek(), new LiteralString("", getInputFile(), getLineNum(), getColNum()));
				}

			}

			Node topNd = popNode();
			popState();
			
			// Opensolutionfile seems to be the only command where the options must come after
			// the required parameter.
			if (topNd instanceof Command && ((Command)topNd).name.equals("Opensolutionfile") && peek() == '[') {
				
				next();
				
				pushNode(topNd);
				pushState(State.COMMAND_OPTIONS);
				
				((Command)topNd).options = new Block(getInputFile(), getLineNum(), getColNum());
				
			}
			else {
				appendToParent(nd.peek(), stateStack.peek(), topNd);
			}
			
		}
		else {
			str.append(c);
		}
	}


	private void stateSlash() throws IOException, LatexParserException {
		
		char c = next();

		if (stateStack.get(stateStack.size() - 2) == State.TABULAR && c == '\\') {

			popState(); // Slash

			flushLiteralText();

			((Tabular)nd.peek()).addRow(getInputFile(), getLineNum(), getColNum());

		}
		// \\ ==> forced line break
		// \[ or \] ==> math mode
		// \n ==> ignore whitespace
		// \[space] ==> Appears after . for abbreviations so that they don't act
		//    as end of sentence period.  Keep escaping and will be removed by writer.
		// rest are escaped characters
		// See: http://en.wikibooks.org/wiki/LaTeX/Special_Characters
		else if (c == '\\' || c == '$' || c == '#' || c == '%' || c == '&' || c == '_' || c == '{' || c == '}' || c == '\n' || c == ' ') {

			str.append("\\").append(c);
			popState();

		}
		else if (c == '[') {

			popState(); // Slash

			flushLiteralText();

			pushNode(new MathMode(MathMode.Style.SLASH_SQUARE, getInputFile(), getLineNum(), getColNum()));
			pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
			pushState(State.MATHMODE);
		}
		else if (c == '(') {

			popState(); // Slash

			flushLiteralText();

			pushNode(new MathMode(MathMode.Style.SLASH_PAREN, getInputFile(), getLineNum(), getColNum()));
			pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
			pushState(State.MATHMODE);
		}
		else {

			popState(); // Slash

			flushLiteralText();

			str.append(c);

			pushNode(new Command(getInputFile(), getLineNum(), getColNum()));
			pushState(State.COMMAND_NAME);
		}
	}

	private void stateCommandName() throws IOException, LatexParserException {
		
		char c = peek();
		
		// TODO: How to handle "\ " as a command, other non-letters.  Right
		// now "\ foo" creates this as a command name, but "\ " is the command.
		
		if (Character.isLetter(c) || c == '*') {
			next();
			str.append(c);
		}
		else if (c == '[') {

			next();

			Command curCommand = (Command)nd.peek();

			curCommand.name = str.toString();
			str.delete(0, str.length());

			curCommand.options = new Block(getInputFile(), getLineNum(), getColNum());

			popState();
			pushState(State.COMMAND_OPTIONS);

		}
		else if (c == '{') {

			next();

			Command curCommand = (Command)nd.peek();

			curCommand.name = str.toString();
			str.delete(0, str.length());

			if (curCommand.name.equals("begin")) {

				Environment curEnv = new Environment(curCommand.getInputFile(), curCommand.getLineNum(), curCommand.getColNum());

				popNode();
				pushNode(curEnv);

				popState();
				pushState(State.BEGIN_ENVIRONMENT_NAME);
			}
			else if (curCommand.name.equals("end")) {

				popNode(); // Remove the \end{...} from the stack to be handled later

				popState();
				pushState(State.END_ENVIRONMENT_NAME);

			}
			else {

				curCommand.addParameter(new Block(getInputFile(), getLineNum(), getColNum()));

				popState();
				pushState(State.COMMAND_PARAMS_BLOCK);
			}

		}
		else {

			Command topNd = (Command)popNode();
			topNd.name = str.toString();

			if (topNd.name.length() == 0)
				throw new LatexParserException("Empty name for command", getInputFile(), getLineNum(), getColNum());

			str.delete(0, str.length());

			popState();

			if (stateStack.peek() == State.TABULAR && (topNd.name.equals("endhead") || topNd.name.equals("endfirsthead"))) {
				// This is actually a macro to end a header row in longtabu...
				((Tabular)nd.peek()).addRow(getInputFile(), getLineNum(), getColNum());
			}
			else {
				appendToParent(nd.peek(), stateStack.peek(), topNd);
			}

		}
	}
	
	private void stateBeginEnvironment() throws IOException, LatexParserException {
		
		char c = next();

		if (Character.isLetter(c) || c == '*') {
			str.append(c);
		}
		else if (c == '}') {

			char cnext = peek();

			Environment curEnv = (Environment)nd.peek();

			curEnv.name = str.toString();
			str.delete(0, str.length());

			if (curEnv.name.equals("tabular") || curEnv.name.equals("tabularx") || curEnv.name.equals("longtable")
					|| curEnv.name.equals("tabu") || curEnv.name.equals("longtabu")) {

				nd.pop();

				Tabular curTab = new Tabular(curEnv.getInputFile(), curEnv.getLineNum(), curEnv.getColNum());
				curTab.name = curEnv.name;

				pushNode(curTab);

				if (cnext == '[') {

					next();
					popState();

					curTab.options = new Block(getInputFile(), getLineNum(), getColNum());

					pushState(State.COMMAND_OPTIONS);

				}
				else {

					popState();
					pushState(State.TABULAR);

				}

			}
			else if (curEnv.name.equals("equation") || curEnv.name.equals("equation*") ||
					curEnv.name.equals("gather") || curEnv.name.equals("gather*") ||
					curEnv.name.equals("align") || curEnv.name.equals("align*") ||
					curEnv.name.equals("eqnarray") || curEnv.name.equals("eqnarray*")) {
				
				nd.pop();
				
				MathMode.Style sty = null;
				if (curEnv.name.equals("equation") || curEnv.name.equals("equation*") ||
					curEnv.name.equals("gather") || curEnv.name.equals("gather*")) {
					sty = MathMode.Style.GATHER;
				}
				else if (curEnv.name.equals("align") || curEnv.name.equals("align*") ||
						curEnv.name.equals("eqnarray") || curEnv.name.equals("eqnarray*")) {
					sty = MathMode.Style.ALIGN;
				}
				else
					throw new RuntimeException("???");

				pushNode(new MathMode(curEnv.name, sty, curEnv.getInputFile(), curEnv.getLineNum(), curEnv.getColNum()));
				pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
				
				popState();
				pushState(State.MATHMODE);
				
			}
			else {
				if (cnext == '[') {

					next();
					popState();

					curEnv.options = new Block(getInputFile(), getLineNum(), getColNum());

					pushState(State.COMMAND_OPTIONS);

				}
				else {
					// TODO: This would be a place to choose between PARAMS_BLOCK and ENV_CONTENT

					curEnv.content = new Block(getInputFile(), getLineNum(), getColNum());

					popState();
					pushState(State.COMMAND_PARAMS_BLOCK);
				}
			}

		}
		else {
			throw new LatexParserException("Unknown char after " + str, getInputFile(), getLineNum(), getColNum());
		}
	}

	private void stateEndEnvironment() throws IOException, LatexParserException {
		
		char c = next();

		if (Character.isLetter(c) || c == '*') {
			str.append(c);
		}
		else if (c == '}') {

			String name = str.toString();
			str.delete(0, str.length());
			
			Node parentNode = nd.peek();
			if (parentNode instanceof Environment) {

				Environment currEnv = ((Environment)parentNode);
	
				if (!currEnv.name.equals(name)) {
					throw new LatexParserException("begin/end commands don't match: " + currEnv.name + " vs " + name, getInputFile(), getLineNum(), getColNum());
				}
				
				if (currEnv instanceof Tabular) {
					((Tabular)currEnv).flushTempMatrix();
				}
	
				Node topNd = popNode();
				popState(); // END_ENVIRONMENT_NAME
				popState(); // implicit COMMAND_PARAMS_BLOCK because of begin/end
	
				appendToParent(nd.peek(), stateStack.peek(), topNd);
			}
			else if (parentNode instanceof Block) {
				
				// Check for math mode
				
				parentNode = ((MathMode)nd.get(nd.size() - 2));
				
				if (parentNode instanceof MathMode) {
				
					MathMode currMM = (MathMode)parentNode;
					
					if (!currMM.getName().equals(name)) {
						throw new LatexParserException("begin/end commands don't match: " + currMM.getName() + " vs " + name, getInputFile(), getLineNum(), getColNum());
					}
					
					Block mathBlock = (Block)popNode();
					currMM = (MathMode)popNode();
					
					currMM.addEquation(mathBlock);
					
					popState(); // END_ENVIRONMENT_NAME
					popState(); // MATHMODE
		
					appendToParent(nd.peek(), stateStack.peek(), currMM);
					
				}
				else {
					throw new LatexParserException("Unknown matching end environment", getInputFile(), getLineNum(), getColNum());
				}
				
			}
			else {
				throw new LatexParserException("Unknown matching end environment", getInputFile(), getLineNum(), getColNum());
			}
			
		}
		else {
			throw new LatexParserException("Unknown char after " + str, getInputFile(), getLineNum(), getColNum());
		}
	}

	private void stateComment() throws IOException, LatexParserException {
		
		char c = next();

		if (c == '\n') {

			popState();

			appendToParent(nd.peek(), stateStack.peek(), new Comment(str.toString(), getInputFile(), getLineNum(), getColNum()));

			str.delete(0, str.length());
		}
		else {
			str.append(c);
		}
	}

	private void stateMathMode() throws IOException, LatexParserException {
		
		/*
		 * TODO: Math mode doesn't quite work yet.  We need to hook into the stateCommandName
		 * method, and return to stateMathMode when inside a command's parameter if that command
		 * is a math command (Ex: \frac{}{}).
		 * 
		 * For commands that are not math (Ex: \mbox{}), we are okay to leave math mode, but
		 * then all of the the checks like this while loop below might need to be more intelligent
		 * to know when we should or should not be in math mode.
		 */
		
		Block mmBlock = ((Block)nd.peek());
		
		// Find the math mode that we are in...
		int mmNodePos = nd.size() - 2;
		Node parentNd = nd.get(mmNodePos);
		while (parentNd instanceof Block) {
			
			mmNodePos--;
			if (mmNodePos < 0) {
				throw new LatexParserException("Unable to find parent MathMode object", getInputFile(), getLineNum(), getColNum());
			}
			parentNd = nd.get(mmNodePos);
		}
		
		if (!(parentNd instanceof MathMode)) {
			throw new LatexParserException("Unexpected block type in math mode: " + parentNd.getClass(), getInputFile(), getLineNum(), getColNum());
		}
		
		MathMode mm = (MathMode)parentNd;

		char c = next();

		if (mm.getStyle() == MathMode.Style.DOLLAR && c == '$' && mmBlock.values.size() == 0 && str.length() == 0) {
			// Actually block mode $$
			mm.setStyle(MathMode.Style.DOLLAR_DOLLAR);
		}
		else if (c == '\\') {
			pushState(State.MATHMODE_SLASH);
		}
		else if (c == '$') {

			boolean doEnd = false;
			if (mm.getStyle() == MathMode.Style.DOLLAR) {
				doEnd = true;
			}
			else if (mm.getStyle() == MathMode.Style.DOLLAR_DOLLAR) {
				if (str.length() > 0 && str.charAt(str.length() - 1) == '$') {
					doEnd = true;
					str.deleteCharAt(str.length() - 1);
				}
			}

			if (doEnd) {
				
				// Check that the stack has one Block and one MathMode on top
				if (mmNodePos != nd.size() - 2)
					throw new LatexParserException("Tried to leave math mode, but there were still open blocks", getInputFile(), getLineNum(), getColNum());

				flushLiteralText();
				
				// Leave math mode
				popState();
				mmBlock = (Block)popNode();
				mm = (MathMode)popNode();
				
				mm.addEquation(mmBlock);
				
				appendToParent(nd.peek(), stateStack.peek(), mm);

			}
			else {
				str.append(c);
			}

		}
		else if (c == '&') {
			
			if (mm.getStyle() != MathMode.Style.ALIGN)
				throw new LatexParserException("Invalid & character in math mode", getInputFile(), getLineNum(), getColNum());
			
			// Check that the stack has one Block and one MathMode on top
			if (mmNodePos != nd.size() - 2)
				throw new LatexParserException("Tried move to right side of align, but there were still open blocks", getInputFile(), getLineNum(), getColNum());
			
			flushLiteralText();
			
			mmBlock = (Block)popNode();
			mm.addEquation(mmBlock);
			
			pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
			
		}
		else if (c == '{') {
			
			flushLiteralText();

			pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
			pushState(State.MATHMODE);
			
		}
		else if (c == '}') {
			
			flushLiteralText();

			Node topNd = popNode();
			popState();

			appendToParent(nd.peek(), stateStack.peek(), topNd);
			
		}
		else if (c == '%') {
			
			flushLiteralText();
			pushState(State.COMMENT);
			
		}
		else {
			str.append(c);
		}
		
	}

	private void stateMathModeSlash() throws IOException, LatexParserException {
		
		Block mmBlock = ((Block)nd.peek());
		
		// Find the math mode that we are in...
		int mmNodePos = nd.size() - 2;
		Node parentNd = nd.get(mmNodePos);
		while (parentNd instanceof Block) {
			
			mmNodePos--;
			if (mmNodePos < 0) {
				throw new LatexParserException("Unable to find parent MathMode object", getInputFile(), getLineNum(), getColNum());
			}
			parentNd = nd.get(mmNodePos);
		}
		
		if (!(parentNd instanceof MathMode)) {
			throw new LatexParserException("Unexpected block type in math mode: " + parentNd.getClass(), getInputFile(), getLineNum(), getColNum());
		}
		
		MathMode mm = (MathMode)parentNd;
		
		char c = next();

		// \] or \) to end
		if ((mm.getStyle() == MathMode.Style.SLASH_SQUARE && c == ']') ||
				(mm.getStyle() == MathMode.Style.SLASH_PAREN && c == ')')) {

			popState(); // Math Mode Slash
			
			flushLiteralText();
			
			// Leave math mode
			popState();
			mmBlock = (Block)popNode();
			mm = (MathMode)popNode();
			
			mm.addEquation(mmBlock);
			
			appendToParent(nd.peek(), stateStack.peek(), mm);
			
		}
		else if (c == '\\') {
			
			// TODO: Need to handle parboxes better, but for now this hack allows newlines to appear
			
			if (nd.size() > 2) {
				// Grandparent is a block
				Node ndParent = nd.get(nd.size() - 2);
				if (ndParent instanceof Block) {
					
					// Last thing added to grandparent was a parbox (so this is really the second param of the parbox)
					ndParent = ((Block)ndParent).values.get(((Block)ndParent).values.size() - 1);
					
					if (ndParent instanceof Command && ((Command)ndParent).name.equals("parbox")) {
						str.append("\\").append(c);
						return;
					}
					
				}
				
				
			}
			
			if (mm.getStyle() != MathMode.Style.ALIGN && mm.getStyle() != MathMode.Style.GATHER)
				throw new LatexParserException("Command \\\\ math mode has no real effect.  Did you mean to put this equation in a gather environment?", getInputFile(), getLineNum(), getColNum());
			
			// Check that the stack has one Block and one MathMode on top
			if (mmNodePos != nd.size() - 2)
				throw new LatexParserException("Tried move to right side of align, but there were still open blocks", getInputFile(), getLineNum(), getColNum());
			
			// Check that there was more than 2 cells in the row
			if (mm.getStyle() == MathMode.Style.ALIGN && mm.getEquations().get(mm.getEquations().size() - 1).size() == 0)
				throw new LatexParserException("Missing & character in align environment", getInputFile(), getLineNum(), getColNum());
			
			popState(); // Math Mode Slash

			flushLiteralText();
			
			mmBlock = (Block)popNode();
			
			mm.addEquation(mmBlock);
			
			mm.addEquationRow();
			
			pushNode(new Block(getInputFile(), getLineNum(), getColNum()));
			
		}
		else { // A command
			
			popState(); // Math Mode Slash

			flushLiteralText();
			
			str.append(c);

			pushNode(new Command(getInputFile(), getLineNum(), getColNum()));
			pushState(State.COMMAND_NAME);
			
		}
	}

	private void flushLiteralText() throws LatexParserException {
		if (str.length() > 0) {
			
			String finalStr = str.toString();
			
			int lineCount = 0;
			for (char c : finalStr.toCharArray()) {
				if (c == '\n')
					lineCount++;
			}
			
			appendToParent(nd.peek(), stateStack.peek(), new LiteralString(finalStr, getInputFile(), getLineNum() - lineCount, getColNum()));
			str.delete(0, str.length());
		}
	}

	public Stack<Node> getNodeStack() {
		return nd;
	}

	public String getReadBuffer() {
		return str.toString();
	}

	public Stack<State> getStateStack() {
		return stateStack;
	}
	
	public File getInputFile() {
		return inputFile;
	}

	public int getLineNum() {
		// TODO: Account for peek across newline?
		return lineNum;
	}

	public int getColNum() {
		if (nextChar != null)
			return colNum - 1;
		return colNum;
	}
	
}
