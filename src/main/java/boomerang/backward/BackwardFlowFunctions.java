package boomerang.backward;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;

import boomerang.AliasFinder;
import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.WrappedSootField;
import boomerang.forward.AbstractFlowFunctions;
import boomerang.ifdssolver.FlowFunctions;
import boomerang.ifdssolver.IPathEdge;
import boomerang.pointsofindirection.Alloc;
import boomerang.pointsofindirection.AllocationSiteHandler;
import heros.FlowFunction;
import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;

public class BackwardFlowFunctions extends AbstractFlowFunctions
		implements FlowFunctions<Unit, AccessGraph, SootMethod> {

	public BackwardFlowFunctions(BoomerangContext context) {
		this.context = context;
	}

	@Override
	public FlowFunction<AccessGraph> getNormalFlowFunction(final IPathEdge<Unit, AccessGraph> edge, final Unit succ) {
		final Unit curr = edge.getTarget();
		final SootMethod method = context.icfg.getMethodOf(curr);
		final Local thisLocal = method.isStatic() ? null : method.getActiveBody().getThisLocal();
		return new FlowFunction<AccessGraph>() {
			@Override
			public Set<AccessGraph> computeTargets(final AccessGraph source) {

				if (curr instanceof IdentityStmt) {
					IdentityStmt identityStmt = (IdentityStmt) curr;
					if (identityStmt.getRightOp() instanceof CaughtExceptionRef)
						return Collections.emptySet();
				}
				if (!(curr instanceof AssignStmt)) {
					return Collections.singleton(source);
				}
				if (curr instanceof ThrowStmt)
					return Collections.emptySet();
				AssignStmt as = (AssignStmt) curr;
				Value leftOp = as.getLeftOp();
				Value rightOp = as.getRightOp();
				if (leftOp instanceof Local && source.baseMatches(leftOp)) {
					Optional<AllocationSiteHandler> allocates = context.allocationSiteHandlers().assignStatement(as,
							rightOp, source);
					if (allocates.isPresent()) {
						allocates.get().alloc().execute(context,edge);
						return Collections.emptySet();
					} else if (rightOp instanceof CastExpr) {
						CastExpr castExpr = (CastExpr) rightOp;
						Value op = castExpr.getOp();
						if (op instanceof Local) {
							return Collections
									.singleton(source.deriveWithNewLocal((Local) op));
						}
					} else if (rightOp instanceof Local) {
						return Collections
								.singleton(source.deriveWithNewLocal((Local) rightOp));
					} else if (rightOp instanceof InstanceFieldRef) {
						// d = e.f, source = d.c ;
						final InstanceFieldRef fr = (InstanceFieldRef) rightOp;

						if (fr.getBase() instanceof Local) {
							Local base = (Local) fr.getBase();

							Set<AccessGraph> out = new HashSet<>();
							WrappedSootField newFirstField = new WrappedSootField(fr.getField(),
									curr);
							AccessGraph ap = source.deriveWithNewLocal(base);
							AccessGraph prependField = ap.prependField(newFirstField);
							out.add(prependField);
							return out;
						}
					} else if (rightOp instanceof ArrayRef) {
						ArrayRef arrayRef = (ArrayRef) rightOp;
						Local base = (Local) arrayRef.getBase();

						Set<AccessGraph> out = new HashSet<>();
						AccessGraph prependField = source.prependField(
								new WrappedSootField(AliasFinder.ARRAY_FIELD,  curr));
						AccessGraph ap = prependField.deriveWithNewLocal(base);
						out.add(ap);
						return out;
					} else if (rightOp instanceof StaticFieldRef && context.trackStaticFields()) {
						StaticFieldRef fr = (StaticFieldRef) rightOp;
						AccessGraph ap = source
								.prependField(new WrappedSootField(fr.getField(), curr))
								.makeStatic();

						if (ap.hasSetBasedFieldGraph()) {
							ap = source.dropTail()
									.prependField(new WrappedSootField(fr.getField(), curr))
									.makeStatic();
						}
						return Collections.singleton(ap);
					}
				} else if (leftOp instanceof ArrayRef) {
					ArrayRef leftArrayRef = (ArrayRef) leftOp;
					Local base = (Local) leftArrayRef.getBase();
					if (source.baseAndFirstFieldMatches(base, AliasFinder.ARRAY_FIELD)) {
						Optional<AllocationSiteHandler> handler = context.allocationSiteHandlers()
								.arrayStoreStatement(as, rightOp, source);
						if (handler.isPresent()) {
							handler.get().alloc().execute(context,edge);
							return Collections.emptySet();
						}
						Set<AccessGraph> out = new HashSet<>();
						out.add(source);
						if (rightOp instanceof Local)
							out.addAll(source.deriveWithNewLocal((Local) rightOp).popFirstField());
						return out;
					}
				} else if (leftOp instanceof InstanceFieldRef) {
					InstanceFieldRef fr = (InstanceFieldRef) leftOp;
					Value base = fr.getBase();
					SootField field = fr.getField();
					if (source.baseMatches(base) && source.firstFirstFieldMayMatch(field)) {
						Optional<AllocationSiteHandler> fieldWriteStatements = context.allocationSiteHandlers()
								.fieldStoreStatement(as, fr, rightOp, source);
						if (fieldWriteStatements.isPresent()) {
							fieldWriteStatements.get().alloc().execute(context,edge);
						}
						if (rightOp instanceof NullConstant) {
							if (!source.firstFieldMustMatch(field))
								return Collections.singleton(source);
							return Collections.emptySet();
						}

						if (rightOp instanceof Local) {
							Set<AccessGraph> out = new HashSet<>();
							out.addAll(source.deriveWithNewLocal((Local) rightOp)
										.popFirstField());
							if (!source.firstFieldMustMatch(field))
								out.add(source);

							return out;
						}
					}
					// Strong updates of fields
					// if(source.firstFieldMustMatch(field) && base instanceof
					// Local){
					// AliasFinder dart = new AliasFinder(context);
					// AliasResults res = dart.findAliasAtStmt(new
					// AccessGraph((Local)base, base.getType()), curr);
					// System.out.println("Strong update " + source + " @ "+
					// as);
					// if(res.keySet().size() == 1 &&
					// res.values().contains(source.dropTail()) &&
					// source.getFieldCount() == 1)
					// return Collections.emptySet();
					// }
				} else if (leftOp instanceof StaticFieldRef && context.trackStaticFields()) {
					StaticFieldRef sfr = (StaticFieldRef) leftOp;
					if (source.isStatic() && source.firstFieldMustMatch(sfr.getField())) {
						if (rightOp instanceof Local) {
							Set<AccessGraph> newAp = source.popFirstField();
							Set<AccessGraph> out = new HashSet<>();
							for (AccessGraph a : newAp) {
								out.add(a.deriveWithNewLocal((Local) rightOp));
							}
							return out;
						}
					}

				}

				return Collections.singleton(source);
			}

		};
	}

	@Override
	public FlowFunction<AccessGraph> getCallFlowFunction(final IPathEdge<Unit, AccessGraph> edge,
			final SootMethod callee, final Unit calleeSp) {
		final Unit callStmt = edge.getTarget();
		final Local[] paramLocals = new Local[callee.getParameterCount()];
		for (int i = 0; i < callee.getParameterCount(); i++)
			paramLocals[i] = callee.getActiveBody().getParameterLocal(i);

		final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
		return new FlowFunction<AccessGraph>() {
			@Override
			public Set<AccessGraph> computeTargets(AccessGraph source) {
				if (context.icfg.isIgnoredMethod(callee))
					Collections.emptySet();
				if (calleeSp instanceof ThrowStmt) {
					return Collections.emptySet();
				}
				if (context.trackStaticFields() && source.isStatic()) {
					return Collections.singleton(source);
				}
				if (context.icfg.isIgnoredMethod(callee)) {
					return Collections.emptySet();
				}
				HashSet<AccessGraph> out = new HashSet<AccessGraph>();
				// mapping of fields of AccessPath those will be killed in
				// callToReturn
				if (callStmt instanceof Stmt) {
					Stmt is = (Stmt) callStmt;
					if (is.containsInvokeExpr()) {
						InvokeExpr ie = is.getInvokeExpr();

						final Value[] callArgs = new Value[ie.getArgCount()];
						for (int i = 0; i < ie.getArgCount(); i++)
							callArgs[i] = ie.getArg(i);

						for (int i = 0; i < paramLocals.length; i++) {
							if (ie.getArgs().get(i).equals(source.getBase())
									&& (source.getFieldCount() > 0 || source.hasSetBasedFieldGraph())) {
									out.add(source.deriveWithNewLocal(paramLocals[i]));
							}
						}

						if (!callee.isStatic() && ie instanceof InstanceInvokeExpr) {
							InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) is.getInvokeExpr();
							if (source.baseMatches(iIExpr.getBase())) {
								AccessGraph replacedThisValue = source.deriveWithNewLocal(thisLocal);
								if (context.isValidAccessPath(replacedThisValue)) {
									out.add(replacedThisValue);
								}
							}
						}
					}

				}

				if (callStmt instanceof AssignStmt && calleeSp instanceof ReturnStmt) {
					AssignStmt as = (AssignStmt) callStmt;
					Value leftOp = as.getLeftOp();
					// mapping of return value
					if (leftOp instanceof Local && source.baseMatches(leftOp)) {
						ReturnStmt retSite = (ReturnStmt) calleeSp;
						Value retOp = retSite.getOp();
						if (retOp instanceof Local) {
							AccessGraph possibleAccessPath = source.deriveWithNewLocal((Local) retOp);
							out.add(possibleAccessPath);
						}
						Optional<AllocationSiteHandler> handler = context.allocationSiteHandlers()
								.returnStmtViaCall(as, source, retOp);
						if (handler.isPresent()) {
							handler.get().alloc().execute(context,edge);
							return Collections.emptySet();
						}
					}

				}
				return out;
			}
		};
	}

	@Override
	public FlowFunction<AccessGraph> getReturnFlowFunction(final IPathEdge<Unit, AccessGraph> edge, final Unit callSite,
			final SootMethod callee, final Unit returnSite) {

		final Local[] paramLocals = new Local[callee.getParameterCount()];
		for (int i = 0; i < callee.getParameterCount(); i++)
			paramLocals[i] = callee.getActiveBody().getParameterLocal(i);
		final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
		return new FlowFunction<AccessGraph>() {

			@Override
			public Set<AccessGraph> computeTargets(AccessGraph source) {
				if (context.icfg.isIgnoredMethod(callee))
					Collections.emptySet();
				AccessGraph derivedSource = source;
				Set<AccessGraph> out = new HashSet<>();
//				if (!context.getContextRequester().continueAtCallSite(callSite, callee)) {
//					boolean isParam = false;
//					for (Local l : paramLocals) {
//						if (source.getBase().equals(l)) {
//							isParam = true;
//						}
//					}
//					if (source.isStatic() || isParam || source.getBase().equals(thisLocal)) {
//						Alloc alloc = new Alloc(source, edge.getTarget(), true);
//						alloc.execute(context,edge);
//					}
//					return Collections.emptySet();
//				}

				if (context.trackStaticFields() && source.isStatic())
					return Collections.singleton(source);

				if (callSite instanceof Stmt) {
					Stmt is = (Stmt) callSite;
					if (is.containsInvokeExpr()) {
						final InvokeExpr ie = is.getInvokeExpr();
						for (int i = 0; i < paramLocals.length; i++) {
							if (source.getBase().equivTo(paramLocals[i])) {
								Value arg = ie.getArg(i);
								if (arg instanceof Local) {
									AccessGraph ap = derivedSource.deriveWithNewLocal((Local) arg);
									out.add(ap);
								}
							}
						}
						if (!callee.isStatic() && ie instanceof InstanceInvokeExpr) {
							InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) is.getInvokeExpr();
							if (source.baseMatches(thisLocal)) {
								Local newBase = (Local) iIExpr.getBase();
								AccessGraph ap = derivedSource.deriveWithNewLocal(newBase);
								out.add(ap);

								// Fields that do not have a null assignment
								// must turn around
								if ((source.getFieldCount() == 1)
										&& !source.isStatic()) {
									SootMethod caller = context.icfg.getMethodOf(callSite);
									if (callee.isConstructor() && (!caller.isConstructor()
											|| !caller.getActiveBody().getThisLocal().equals(newBase))) {
										Alloc alloc = new Alloc(source, edge.getTarget(), true);
										alloc.execute(context,edge);
										return Collections.emptySet();
									}
								}

							}
						}
					}
				}
				return out;
			}
		};

	}

	@Override
	public FlowFunction<AccessGraph> getCallToReturnFlowFunction(final IPathEdge<Unit, AccessGraph> edge,
			Unit returnSite, final Collection<SootMethod> callees) {

		final Unit callSite = edge.getTarget();
		return new FlowFunction<AccessGraph>() {
			@Override
			public Set<AccessGraph> computeTargets(AccessGraph source) {
				boolean sourceIsKilled = false;

				if (context.trackStaticFields() && source.isStatic()) {
					return Collections.emptySet();
				}
				Set<AccessGraph> out = new HashSet<>();
				if (callSite instanceof Stmt) {
					Stmt is = (Stmt) callSite;
					if (is.containsInvokeExpr()) {
						final InvokeExpr ie = is.getInvokeExpr();

						final Value[] callArgs = new Value[ie.getArgCount()];
						for (int i = 0; i < ie.getArgCount(); i++)
							callArgs[i] = ie.getArg(i);

						if (source.getFieldCount() > 0) {
							for (Value callVal : callArgs) {
								if (callVal.equals(source.getBase())) {
									sourceIsKilled = true;
								}
							}
							if (ie instanceof InstanceInvokeExpr) {
								InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
								if (source.getBase().equals(iie.getBase()))
									sourceIsKilled = true;
							}
						}
						if (context.backwardMockHandler.handles(callSite, ie, source, callArgs)) {
							return context.backwardMockHandler.computeTargetsOverCall(callSite, ie, source, callArgs,
									edge);
						}
						if (ie.getMethod().equals(AliasFinder.arrayCopy())) {
							for (Value callVal : callArgs) {
								if (callVal.equals(source.getBase())) {
									// java uses call by value, but fields of
									// complex objects can be changed (and
									// tainted), so use this conservative
									// approach:
									Set<AccessGraph> nativeAbs = context.ncHandler.getBackwardValues(is, source,
											callArgs);
									out.addAll(nativeAbs);
								}
							}
							return out;
						}
					}

				}

				if (callSite instanceof AssignStmt) {
					AssignStmt as = (AssignStmt) callSite;
					Value leftOp = as.getLeftOp();
					// mapping of return value
					if (leftOp instanceof Local && !source.isStatic() && source.getBase().equals(leftOp)) {
						sourceIsKilled = true;
						if(callees.isEmpty() && source.getFieldCount() == 0 && !source.hasSetBasedFieldGraph()){
							new Alloc(source, callSite, false).execute(context,edge);
						}
					}
					
				}
				if (!sourceIsKilled)
					out.add(source);

				if (callees.isEmpty()) {
					return Collections.singleton(source);
				}
				return out;
			}

		};
	}
}
