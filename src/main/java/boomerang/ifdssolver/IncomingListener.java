package boomerang.ifdssolver;

import heros.solver.Pair;

public interface IncomingListener<N, D, M> {

	public void hasIncomingEdge(IPathEdge<N, D> edge);

	public Pair<N, D> getSourcePair();

}
