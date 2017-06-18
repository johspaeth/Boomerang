package boomerang.backward;

import boomerang.accessgraph.AccessGraph;
import heros.solver.Pair;
import soot.Unit;

public interface PropagationSourceListener {
	public void discoveredPropagationSource(Pair<Unit,AccessGraph> origin);
}
