package boomerang.allocationsitehandler;

import java.util.Collection;

import com.google.common.base.Optional;

import boomerang.AliasFinder;
import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.WrappedSootField;
import boomerang.pointsofindirection.Alloc;
import boomerang.pointsofindirection.AllocationSiteHandler;
import boomerang.pointsofindirection.AllocationSiteHandlers;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.StringConstant;

public class PrimitiveTypeAndReferenceType implements AllocationSiteHandlers {

	private boolean isAllocationValue(Value val) {
		return (val instanceof NewExpr || val instanceof NewArrayExpr || val instanceof NewMultiArrayExpr
				|| val instanceof Constant);
	}

	@Override
	public Optional<AllocationSiteHandler> assignStatement(final AssignStmt stmt, final Value rightOp,
			final AccessGraph source) {
		if (!isAllocationValue(rightOp))
			return Optional.absent();

		if(stmt.getRightOp() instanceof StringConstant){
			if(source.getFieldCount() > 0){
				for(WrappedSootField f : source.getFirstField()){
					if(f.getField().getName().equals("value")){
						return Optional.<AllocationSiteHandler>of(new AllocationSiteHandler() {
							@Override
							public Alloc alloc() {
								return new Alloc(source, stmt, false);
							}
						});
					}
				}
			}
		}
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
			final AccessGraph source, final ReturnStmt returnSite, Value retOp) {
		if (!(retOp instanceof NullConstant) || source.getFieldCount() != 0 || source.hasSetBasedFieldGraph())
			return Optional.absent();
		return Optional.<AllocationSiteHandler>of(new AllocationSiteHandler() {
			@Override
			public Alloc alloc() {
				return new Alloc(source,assignedCallSite, returnSite, retOp instanceof NullConstant);
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

	@Override
	public Optional<AllocationSiteHandler> callToReturnAssign(final AssignStmt callSite, final AccessGraph source,
			Collection<SootMethod> callees) {
		if(!callees.isEmpty())
			return Optional.absent();
		return Optional.<AllocationSiteHandler>of(new AllocationSiteHandler() {
			@Override
			public Alloc alloc() {
				return new Alloc(source, callSite, false);
			}
		});
	}
}
