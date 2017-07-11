package boomerang.pointsofindirection;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;

public class Alloc {

	private Unit target;
	private SootMethod method;
	private AccessGraph factAtTarget;
	private boolean isNullAlloc;
	private Unit allocationSite;

	/**
	 * Creates an allocation POI with the backward path edge reaching it.
	 * 
	 * @param pathEdge
	 */
	public Alloc(AccessGraph factAtTarget, Unit target, boolean isNullAlloc) {
		this.factAtTarget = factAtTarget;
		this.target = target;
		this.isNullAlloc = isNullAlloc;
		this.allocationSite = target;
	}
	public Alloc(AccessGraph factAtTarget, Unit target,Unit allocationSite, boolean isNullAlloc) {
		this.factAtTarget = factAtTarget;
		this.target = target;
		this.allocationSite = allocationSite;
		this.isNullAlloc = isNullAlloc;
	}
	public void execute(BoomerangContext context) {
		context.debugger.onAllocationSiteReached(target, factAtTarget);
		AccessGraph alloc = factAtTarget.deriveWithAllocationSite(allocationSite,isNullAlloc);
		if (target instanceof AssignStmt && ((AssignStmt) target).getRightOp() instanceof NewExpr)
			alloc = alloc.deriveWithNewLocal(alloc.getBase(),
					((NewExpr) ((AssignStmt) target).getRightOp()).getBaseType());
		assert alloc.hasAllocationSite() == true;
		// start forward propagation from the path edge target with the
		// allocation site.
		context.getForwardSolver().startPropagationAlongPath(target, alloc, alloc.deriveWithoutAllocationSite(), null);
	}

	@Override
	public String toString() {
		return "Alloc [" + target + " €" + method + "]";
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
