package boomerang.forward;

import boomerang.accessgraph.AccessGraph;
import heros.solver.Pair;
import soot.Unit;

public interface AllocationSiteListener {
	public void discoveredAllocationSite(Pair<Unit,AccessGraph> origin);
}
