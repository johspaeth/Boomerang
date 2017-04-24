package boomerang.pointsofindirection;


import com.google.common.base.Optional;

import boomerang.accessgraph.AccessGraph;

public interface AllocationSiteHandler {
	public Optional<Alloc> sendForwards();
}
