package boomerang.backward;

import java.util.List;
import java.util.Set;

import javax.swing.plaf.synth.SynthSliderUI;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IncomingListener;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;

public class PropagationOriginListener implements IncomingListener<Unit, AccessGraph, SootMethod>{

	private Pair<Unit, AccessGraph> sourcePair;
	private BoomerangContext context;
	private Set<Pair<Unit, AccessGraph>> triggered = Sets.newHashSet();
	private Set<Pair<Unit, AccessGraph>> allocNodes = Sets.newHashSet();
	private List<PropagationSourceListener> listeners = Lists.newLinkedList();
	private SootMethod method;

	public PropagationOriginListener(Pair<Unit,AccessGraph> sourcePair,SootMethod m,BoomerangContext context) {
		this.sourcePair = sourcePair;
		this.method = m;
		this.context = context;
		triggered.add(sourcePair);
		if(sourcePair.getO2().isPropagationOrigin())
			discoveredPropagationSource(sourcePair);
	}
	
	@Override
	public void hasIncomingEdge(IPathEdge<Unit, AccessGraph> edge) {
		if(!triggered.add(edge.getStartNode()))
			return;
		if(sourcePair.getO2().isPropagationOrigin())
			return;
		if(edge.getStartNode().getO2().isPropagationOrigin()){
			discoveredPropagationSource(edge.getStartNode());
			return;
		}
		context.getBackwardSolver().attachPropagationOriginListener(edge.getStartNode(), context.icfg.getMethodOf(edge.getTarget()),new PropagationSourceListener() {
			
			@Override
			public void discoveredPropagationSource(Pair<Unit, AccessGraph> origin) {
				PropagationOriginListener.this.discoveredPropagationSource(origin);
				
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

	public void discoveredPropagationSource(Pair<Unit, AccessGraph> allocNode){
		if(!allocNodes.add(allocNode))
			return;
		for(PropagationSourceListener l : Lists.newLinkedList(listeners))
			l.discoveredPropagationSource(allocNode);
	}
	
	public void addListener(PropagationSourceListener l){
		listeners.add(l);
		for(Pair<Unit,AccessGraph> n : Sets.newHashSet(allocNodes))
			l.discoveredPropagationSource(n);
	}
}
