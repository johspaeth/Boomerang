package boomerang.pointsofindirection;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.backward.PropagationSourceListener;
import boomerang.ifdssolver.IPathEdge;
import heros.solver.Pair;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;

public class Alloc {

	private Unit target;
	private SootMethod method;
	private AccessGraph factAtTarget;
	private boolean isNullAlloc;

	/**
	 * Creates an allocation POI with the backward path edge reaching it.
	 * 
	 * @param pathEdge
	 */
	public Alloc(AccessGraph factAtTarget, Unit target, boolean isNullAlloc) {
		this.factAtTarget = factAtTarget;
		this.target = target;
		this.isNullAlloc = isNullAlloc;
	}

	public void execute(final BoomerangContext context, IPathEdge<Unit, AccessGraph> edge) {
		context.getBackwardSolver().attachPropagationOriginListener(edge.getStartNode(), context.icfg.getMethodOf(edge.getTarget()), new PropagationSourceListener() {
			
			@Override
			public void discoveredPropagationSource(Pair<Unit, AccessGraph> origin) {
				if(factAtTarget.getFieldCount() == 0 && origin.getO2().getFieldCount() == 0){
					if(Scene.v().getOrMakeFastHierarchy().canStoreType(factAtTarget.getBase().getType(),origin.getO2().getBase().getType())){
						sendForward(context);
					}
				} else{
					sendForward(context);
				}
			}
		});
	}

	private void sendForward(BoomerangContext context) {
		context.debugger.onAllocationSiteReached(target, factAtTarget);
		AccessGraph alloc = factAtTarget.deriveWithAllocationSite(target,isNullAlloc);
		if (target instanceof AssignStmt && ((AssignStmt) target).getRightOp() instanceof NewExpr)
			alloc = alloc.deriveWithNewLocal(alloc.getBase());
		assert alloc.hasAllocationSite() == true;
		// start forward propagation from the path edge target with the
		// allocation site.
		this.method = context.icfg.getMethodOf(target);
		System.out.println(this);
		context.getForwardSolver().startPropagationAlongPath(target, alloc, alloc.deriveWithoutAllocationSite());
	}

	@Override
	public String toString() {
		return "Alloc [" + factAtTarget + " "+ target + " €" + method + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((factAtTarget == null) ? 0 : factAtTarget.hashCode());
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
		Alloc other = (Alloc) obj;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (factAtTarget == null) {
			if (other.factAtTarget != null)
				return false;
		} else if (!factAtTarget.equals(other.factAtTarget))
			return false;
		return true;
	}
}
