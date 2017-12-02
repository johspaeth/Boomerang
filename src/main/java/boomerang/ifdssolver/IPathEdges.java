package boomerang.ifdssolver;

import heros.BiDiInterproceduralCFG;

public interface IPathEdges<N, D, M, I extends BiDiInterproceduralCFG<N, M>> {
	public boolean hasAlreadyProcessed(IPathEdge<N,D> edge);
	public void register(IPathEdge<N,D> edge);
	public int size();
	public void clear();
	public void printStats();
	public void printTopMethods(int i);
}
