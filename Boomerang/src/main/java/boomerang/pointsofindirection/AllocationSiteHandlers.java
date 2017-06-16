package boomerang.pointsofindirection;

import com.google.common.base.Optional;

import boomerang.accessgraph.AccessGraph;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;

public interface AllocationSiteHandlers {
	public Optional<AllocationSiteHandler> assignStatement(AssignStmt stmt, Value rightOp, AccessGraph g);

	public Optional<AllocationSiteHandler> arrayStoreStatement(AssignStmt stmt, Value rightOp, AccessGraph source);

	public Optional<AllocationSiteHandler> returnStmtViaCall(AssignStmt assignedCallSite, AccessGraph source,
			Value retOp);

	public Optional<AllocationSiteHandler> fieldStoreStatement(AssignStmt stmt, InstanceFieldRef fieldRef,
			Value rightOp, AccessGraph source);
}
