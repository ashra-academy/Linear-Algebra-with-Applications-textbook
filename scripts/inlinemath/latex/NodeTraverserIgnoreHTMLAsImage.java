package latex;

/**
 * NodeTraverser type used by Analyzers, so that they can ignore
 * \begin{htmlasimage} environments.
 */
public abstract class NodeTraverserIgnoreHTMLAsImage extends NodeTraverser {

	@Override
	protected boolean shouldDescendHTMLAsImage() {
		return false;
	}
	
}
