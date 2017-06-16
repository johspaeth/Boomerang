package boomerang.forward;

import java.util.Set;

import com.google.common.collect.Sets;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IncomingListener;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;

public abstract class AllocationListener implements IncomingListener<Unit, AccessGraph, SootMethod>{

	private Pair<Unit, AccessGraph> sourcePair;
	private BoomerangContext context;
	private Set<Pair<Unit, AccessGraph>> triggered = Sets.newHashSet();

	public AllocationListener(Pair<Unit,AccessGraph> sourcePair,BoomerangContext context) {
		this.sourcePair = sourcePair;
		this.context = context;
		if(sourcePair.getO2().hasAllocationSite())
			discoveredAllocationSite(sourcePair);
	}
	
	@Override
	public void hasIncomingEdge(IPathEdge<Unit, AccessGraph> edge) {
		if(!triggered.add(edge.getStartNode()))
			return;
		System.out.println(edge.getStartNode());
		if(sourcePair.getO2().hasAllocationSite())
			return;
		if(edge.getStartNode().getO2().hasAllocationSite()){
			discoveredAllocationSite(edge.getStartNode());
			return;
		}
		System.out.println(edge);
		context.getForwardSolver().attachIncomingListener(new AllocationListener(edge.getStartNode(),context) {
			@Override
			public void discoveredAllocationSite(Pair<Unit, AccessGraph> allocNode) {
				AllocationListener.this.discoveredAllocationSite(allocNode);
			}
		});
	}

	@Override
	public Pair<Unit, AccessGraph> getSourcePair() {
		return sourcePair;
	}

	public abstract void discoveredAllocationSite(Pair<Unit, AccessGraph> allocNode);
}
