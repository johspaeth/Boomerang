package boomerang.forward;

import boomerang.accessgraph.AccessGraph;
import boomerang.ap.BoomerangContext;
import heros.solver.Pair;
import soot.Type;
import soot.Unit;

public abstract class AllocationTypeListener extends AllocationListener{

	public AllocationTypeListener(Pair<Unit, AccessGraph> sourcePair, BoomerangContext context) {
		super(sourcePair, context);
	}
	
	@Override
	public void discoveredAllocationSite(Pair<Unit, AccessGraph> allocNode) {
		if(allocNode.getO2().hasNullAllocationSite())
			return;
		discoveredAllocationType(allocNode.getO2().getAllocationType());
	}

	public abstract void discoveredAllocationType(Type type);
}
