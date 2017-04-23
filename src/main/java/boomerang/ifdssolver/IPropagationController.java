package boomerang.ifdssolver;

public interface IPropagationController<N,D> {
	boolean continuePropagate(IPathEdge<N, D> edge);
}
