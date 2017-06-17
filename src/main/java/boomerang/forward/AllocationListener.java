package boomerang.forward;

import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IncomingListener;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;

public class AllocationListener implements IncomingListener<Unit, AccessGraph, SootMethod>{

	private Pair<Unit, AccessGraph> sourcePair;
	private BoomerangContext context;
	private Set<Pair<Unit, AccessGraph>> triggered = Sets.newHashSet();
	private Set<Pair<Unit, AccessGraph>> allocNodes = Sets.newHashSet();
	private List<AllocationSiteListener> listeners = Lists.newLinkedList();
	private SootMethod method;

	public AllocationListener(Pair<Unit,AccessGraph> sourcePair,SootMethod m, BoomerangContext context) {
		this.sourcePair = sourcePair;
		this.method = m;
		this.context = context;
		if(sourcePair.getO2().hasAllocationSite())
			discoveredAllocationSite(sourcePair);
	}
	
	@Override
	public void hasIncomingEdge(IPathEdge<Unit, AccessGraph> edge) {
		if(!triggered.add(edge.getStartNode()))
			return;
		if(sourcePair.getO2().hasAllocationSite())
			return;
		if(edge.getStartNode().getO2().hasAllocationSite()){
			discoveredAllocationSite(edge.getStartNode());
			return;
		}
		context.getForwardSolver().attachAllocationListener(edge.getStartNode(), context.icfg.getMethodOf(edge.getTarget()),new AllocationSiteListener() {
			
			@Override
			public void discoveredAllocationSite(Pair<Unit, AccessGraph> origin) {
				AllocationListener.this.discoveredAllocationSite(origin);
			}
		});
	}

	@Override
	public Pair<Unit, AccessGraph> getSourcePair() {
		return sourcePair;
	}

	@Override
	public SootMethod getMethod() {
		return method;
	}
	public void discoveredAllocationSite(Pair<Unit, AccessGraph> allocNode){
		if(!allocNodes.add(allocNode))
			return;
		for(AllocationSiteListener l : Lists.newLinkedList(listeners))
			l.discoveredAllocationSite(allocNode);
	}
	
	public void addListener(AllocationSiteListener l){
		listeners.add(l);
		for(Pair<Unit,AccessGraph> n : Sets.newHashSet(allocNodes))
			l.discoveredAllocationSite(n);
	}
}
