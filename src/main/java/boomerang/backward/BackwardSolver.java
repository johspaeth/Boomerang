package boomerang.backward;

import boomerang.AliasFinder;
import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.bidi.Incomings;
import boomerang.bidi.PathEdgeStore;
import boomerang.bidi.Summaries;
import boomerang.ifdssolver.DefaultIFDSTabulationProblem.Direction;
import boomerang.ifdssolver.IFDSSolver;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.PathEdge;
import heros.solver.Pair;
import heros.utilities.DefaultValueMap;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class BackwardSolver
		extends IFDSSolver<Unit, AccessGraph, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> {

	private BoomerangContext context;
	private DefaultValueMap<PairWithMethod, PropagationOriginListener> allocationListenersPerSource = new DefaultValueMap<PairWithMethod, PropagationOriginListener>() {

		@Override
		protected PropagationOriginListener createItem(PairWithMethod key) {
			PropagationOriginListener allocationListener = new PropagationOriginListener(key.sourcePair,key.method, context);
			return allocationListener;
		}
	};

	public BackwardSolver(BackwardProblem tabulationProblem, BoomerangContext context) {
		super(tabulationProblem, context.debugger);
		this.context = context;
		this.pathEdges = new PathEdgeStore(context, Direction.BACKWARD);
		this.summaries = new Summaries(context);
		this.incomings = new Incomings();
	}

	public void startPropagation(AccessGraph d1, Unit stmt) {
		SootMethod m = icfg.getMethodOf(stmt);
		for (Unit s : icfg.getSuccsOf(stmt)) {
			PathEdge<Unit, AccessGraph> edge = new PathEdge<Unit, AccessGraph>(null, d1.propagationOrigin(), s, d1);
			debugger.backwardStart(Direction.BACKWARD, stmt, d1, s);
			if(context.visitableMethod(m))
				propagate(edge, PropagationType.Normal);
			else
				addMethodToPausedEdge(m, edge);
		}
	}

	@Override
	public void onRegister(IPathEdge<Unit, AccessGraph> edge) {
		context.sanityCheckEdge(edge);
		if (edge.getTarget() != null)
			AliasFinder.VISITED_METHODS.add(icfg.getMethodOf(edge.getTarget()));
	}

	public String toString() {
		return "BWSOLVER";
	}

	public void attachPropagationOriginListener(Pair<Unit, AccessGraph> sourcePair, SootMethod m, PropagationSourceListener l) {
		if(!allocationListenersPerSource.containsKey(new PairWithMethod(sourcePair, m))){
			PropagationOriginListener olistener = allocationListenersPerSource.getOrCreate(new PairWithMethod(sourcePair, m));
			context.getBackwardSolver().attachIncomingListener(olistener);
		}
		PropagationOriginListener listeners = allocationListenersPerSource.getOrCreate(new PairWithMethod(sourcePair, m));
		listeners.addListener(l);
	}
	
	
	private class PairWithMethod{
		Pair<Unit, AccessGraph> sourcePair;
		SootMethod method;
		public PairWithMethod(Pair<Unit,AccessGraph> p, SootMethod m) {
			this.sourcePair = p;
			this.method = m;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((method == null) ? 0 : method.hashCode());
			result = prime * result + ((sourcePair == null) ? 0 : sourcePair.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PairWithMethod other = (PairWithMethod) obj;
			if (method == null) {
				if (other.method != null)
					return false;
			} else if (!method.equals(other.method))
				return false;
			if (sourcePair == null) {
				if (other.sourcePair != null)
					return false;
			} else if (!sourcePair.equals(other.sourcePair))
				return false;
			return true;
		}
	}

}
