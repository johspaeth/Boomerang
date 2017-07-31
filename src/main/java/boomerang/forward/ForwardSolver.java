package boomerang.forward;

import boomerang.AliasFinder;
import boomerang.BoomerangContext;
import boomerang.BoomerangTimeoutException;
import boomerang.accessgraph.AccessGraph;
import boomerang.bidi.Incomings;
import boomerang.bidi.PathEdgeStore;
import boomerang.bidi.Summaries;
import boomerang.ifdssolver.DefaultIFDSTabulationProblem.Direction;
import heros.solver.Pair;
import heros.utilities.DefaultValueMap;
import boomerang.ifdssolver.IFDSSolver;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class ForwardSolver extends IFDSSolver<Unit, AccessGraph, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> {

	private BoomerangContext context;
	private DefaultValueMap<PairWithMethod, AllocationListener> allocationListenersPerSource = new DefaultValueMap<PairWithMethod, AllocationListener>() {

		@Override
		protected AllocationListener createItem(PairWithMethod key) {
			AllocationListener allocationListener = new AllocationListener(key.sourcePair, key.method, context);
			return allocationListener;
		}
	};

	public ForwardSolver(ForwardProblem tabulationProblem, BoomerangContext context) {
		super(tabulationProblem, context.debugger);
		this.context = context;
		this.pathEdges = new PathEdgeStore(context, Direction.FORWARD);
		this.summaries = new Summaries(context);
		this.incomings = new Incomings();
	}

	/**
	 * Starts the propagation at the successor of the provided statement.
	 * Creates the path edge <stmt,d1>-><succ(stmt),d2> and adds it to the
	 * solvers worklist. The backward path edge is used to track the path along
	 * which the forward propagation is allowed.
	 * 
	 * @param stmt
	 *            The stmt where the path edge will begin (for each succ(stmt)
	 *            there will be a path edge created).
	 * @param d1
	 *            The start fact of the propagated path edge.
	 * @param d2
	 *            The target fact of the propagated path edge.
	 * @param bwedge
	 *            Provides the path along which the analysis is allowed to
	 *            propagate
	 */
	public void startPropagationAlongPath(final Unit stmt, final AccessGraph d1, final AccessGraph d2) {
		SootMethod m = icfg.getMethodOf(stmt);
		for (Unit succStmt : icfg.getSuccsOf(stmt)) {
			PathEdge<Unit, AccessGraph> pathEdge = new PathEdge<Unit, AccessGraph>(stmt, d1, succStmt, d2);
			if (context.visitableMethod(m))
				propagate(pathEdge, PropagationType.Normal);
			else
				addMethodToPausedEdge(m, pathEdge);
		}

	}

	@Override
	public void onRegister(IPathEdge<Unit, AccessGraph> edge) {
		context.sanityCheckEdge(edge);
		if (edge.getTarget() != null)
			AliasFinder.VISITED_METHODS.add(icfg.getMethodOf(edge.getTarget()));
	}

	@Override
	public void cleanup() {
		super.cleanup();
	}

	public void attachAllocationListener(Pair<Unit, AccessGraph> sourcePair, SootMethod m, AllocationSiteListener l) {
		if (!allocationListenersPerSource.containsKey(new PairWithMethod(sourcePair, m))) {
			AllocationListener allocationListener = allocationListenersPerSource
					.getOrCreate(new PairWithMethod(sourcePair, m));
			context.getForwardSolver().attachIncomingListener(allocationListener);
		}
		AllocationListener listeners = allocationListenersPerSource.getOrCreate(new PairWithMethod(sourcePair, m));
		listeners.addListener(l);
	}

	private class PairWithMethod {
		Pair<Unit, AccessGraph> sourcePair;
		SootMethod method;

		public PairWithMethod(Pair<Unit, AccessGraph> p, SootMethod m) {
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
