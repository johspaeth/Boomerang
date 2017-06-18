package boomerang.allocationsitehandler;

import com.google.common.base.Optional;

import boomerang.AliasFinder;
import boomerang.accessgraph.AccessGraph;
import boomerang.pointsofindirection.Alloc;
import boomerang.pointsofindirection.AllocationSiteHandler;
import boomerang.pointsofindirection.AllocationSiteHandlers;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;

public class ReferenceType implements AllocationSiteHandlers {

	private boolean isAllocationValue(Value val) {
		return (val instanceof NewExpr || val instanceof NewArrayExpr || val instanceof NewMultiArrayExpr
				|| val instanceof NullConstant);
	}

	@Override
	public Optional<AllocationSiteHandler> assignStatement(final AssignStmt stmt, final Value rightOp,
			final AccessGraph source) {
		if (!isAllocationValue(rightOp))
			return Optional.absent();
		if(source.hasSetBasedFieldGraph())
			return Optional.absent();
		if (source.getFieldCount() > 0 && !source.firstFieldMustMatch(AliasFinder.ARRAY_FIELD)) {
			return Optional.absent();
		}
		if (source.getFieldCount() > 1 && source.firstFieldMustMatch(AliasFinder.ARRAY_FIELD))
			return Optional.absent();
		return Optional.<AllocationSiteHandler>of(new AllocationSiteHandler() {
			@Override
			public Alloc alloc() {
				return new Alloc(source, stmt, rightOp instanceof NullConstant);
			}
		});
	}

	@Override
	public Optional<AllocationSiteHandler> arrayStoreStatement(final AssignStmt stmt, Value rightOp,
			final AccessGraph source) {
		if (!isAllocationValue(rightOp))
			return Optional.absent();
		return Optional.<AllocationSiteHandler>of(new AllocationSiteHandler() {
			@Override
			public Alloc alloc() {
				return new Alloc(source, stmt, false);
			}
		});
	}

	@Override
	public Optional<AllocationSiteHandler> returnStmtViaCall(final AssignStmt assignedCallSite,
			final AccessGraph source, Value retOp) {
		if (!(retOp instanceof NullConstant) || source.getFieldCount() != 0 || source.hasSetBasedFieldGraph())
			return Optional.absent();
		return Optional.<AllocationSiteHandler>of(new AllocationSiteHandler() {
			@Override
			public Alloc alloc() {
				return new Alloc(source, assignedCallSite, true);
			}
		});
	}

	@Override
	public Optional<AllocationSiteHandler> fieldStoreStatement(final AssignStmt stmt, InstanceFieldRef fieldRef,
			Value rightOp, final AccessGraph source) {
		if (!(rightOp instanceof NullConstant))
			return Optional.absent();
		if (source.getFieldCount() != 1) {
			return Optional.absent();
		}
		return Optional.<AllocationSiteHandler>of(new AllocationSiteHandler() {
			@Override
			public Alloc alloc() {
				return new Alloc(source, stmt, true);
			}
		});
	}
}
