package boomerang.forward;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import heros.solver.Pair;
import soot.Type;
import soot.Unit;

public abstract class AllocationTypeListener implements AllocationSiteListener{

	
	@Override
	public void discoveredAllocationSite(Pair<Unit, AccessGraph> allocNode) {
		discoveredAllocationType(allocNode.getO2().getAllocationType());
	}

	public abstract void discoveredAllocationType(Type type);
}
