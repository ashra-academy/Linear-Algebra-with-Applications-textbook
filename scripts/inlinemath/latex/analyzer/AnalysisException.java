package latex.analyzer;

import latex.node.Node;

public class AnalysisException extends RuntimeException {
	
	private Node location;

	public AnalysisException() {
		super();
	}

	public AnalysisException(String message, Throwable cause, Node location) {
		super(message, cause);
		this.location = location;
	}

	public AnalysisException(String message, Node location) {
		super(message);
		this.location = location;
	}

	public AnalysisException(Throwable cause, Node location) {
		super(cause);
		this.location = location;
	}
	
	public Node getLocation() {
		return location;
	}

}
