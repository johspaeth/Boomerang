package boomerang.forward;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import boomerang.AliasFinder;
import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.WrappedSootField;
import boomerang.ifdssolver.FlowFunctions;
import boomerang.ifdssolver.IFDSSolver.PropagationType;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.PathEdge;
import boomerang.pointsofindirection.ForwardAliasCallback;
import boomerang.pointsofindirection.PointOfIndirection;
import heros.FlowFunction;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;

public class ForwardFlowFunctions extends AbstractFlowFunctions
		implements FlowFunctions<Unit, AccessGraph, SootMethod> {

	public ForwardFlowFunctions(BoomerangContext c) {
		this.context = c;
	}

	@Override
	public FlowFunction<AccessGraph> getNormalFlowFunction(final IPathEdge<Unit, AccessGraph> edge, final Unit succ) {
		final Unit curr = edge.getTarget();

		final SootMethod method = context.icfg.getMethodOf(curr);
		return new FlowFunction<AccessGraph>() {

			@Override
			public Set<AccessGraph> computeTargets(AccessGraph source) {
				if (AliasFinder.HANDLE_EXCEPTION_FLOW && !source.isStatic() && curr instanceof IdentityStmt) {
					IdentityStmt identityStmt = (IdentityStmt) curr;
					if (identityStmt.getRightOp() instanceof CaughtExceptionRef
							&& identityStmt.getLeftOp() instanceof Local) {
						Local leftOp = (Local) identityStmt.getLeftOp();
						// e = d;
							HashSet<AccessGraph> out = new HashSet<AccessGraph>();
							out.add(source);
							out.add(source.deriveWithNewLocal((Local) leftOp));
							return out;
					}
				}
				assert source.isStatic() || method.getActiveBody().getLocals().contains(source.getBase());

				if (!(curr instanceof AssignStmt)) {
					return Collections.singleton(source);
				}

				AssignStmt as = (AssignStmt) curr;
				Value leftOp = as.getLeftOp();
				Value rightOp = as.getRightOp();

				HashSet<AccessGraph> out = new HashSet<AccessGraph>();
				out.add(source);

				if (rightOp instanceof Constant || rightOp instanceof NewExpr) {
					// a = new || a = 2
					if (leftOp instanceof Local && source.baseMatches(leftOp))
						// source == a.*
						return Collections.emptySet();
					// a.f = new || a.f = 2;
					if (leftOp instanceof InstanceFieldRef) {
						InstanceFieldRef fr = (InstanceFieldRef) leftOp;
						Value base = fr.getBase();
						SootField field = fr.getField();
						// source == a.f.*
						if (source.baseAndFirstFieldMatches(base, field))
							return Collections.emptySet();
					}

				}

				if (leftOp instanceof Local) {
					if (source.baseMatches(leftOp)) {
						if (rightOp instanceof InstanceFieldRef) {
							InstanceFieldRef fr = (InstanceFieldRef) rightOp;
							Value base = fr.getBase();
							SootField field = fr.getField();

							if (source.baseAndFirstFieldMatches(base, field)) {
								Set<AccessGraph> popFirstField = source.popFirstField();
								out.addAll(popFirstField);
							} else {
								return Collections.emptySet();
							}
						} else {
							return Collections.emptySet();
						}
					}
				} else if (leftOp instanceof InstanceFieldRef) {
					InstanceFieldRef fr = (InstanceFieldRef) leftOp;
					Value base = fr.getBase();
					SootField field = fr.getField();
					if (!source.isStatic()) {
						if (source.getBase().equals(base) && source.firstFirstFieldMayMatch(field)) {
							if (rightOp instanceof Local) {
								Local local = (Local) rightOp;
								AccessGraph a = new AccessGraph(local);
								context.getBackwardSolver().inject(new PathEdge<Unit, AccessGraph>(null, a.propagationOrigin(), curr, a),
										PropagationType.Normal);
							}
						}
						// Strong update on fields
						if (source.baseAndFirstFieldMatches(base, field))
							return Collections.emptySet();
					}
				} else if (leftOp instanceof ArrayRef) {
					ArrayRef fr = (ArrayRef) leftOp;
					Value base = fr.getBase();
					if (source.baseMatches(base) && source.firstFirstFieldMayMatch(AliasFinder.ARRAY_FIELD)
							&& rightOp instanceof Local) {
						Local local = (Local) rightOp;
						AccessGraph a = new AccessGraph(local);
						context.getBackwardSolver().inject(new PathEdge<Unit, AccessGraph>(null, a, curr, a),
								PropagationType.Normal);
					}
				}
				if (rightOp instanceof CastExpr) {
					CastExpr castExpr = (CastExpr) rightOp;
					Value op = castExpr.getOp();
					if (op instanceof Local) {
						if (!source.isStatic() && source.baseMatches(op)) {
							out.add(source.deriveWithNewLocal((Local) leftOp));
						}
					}
				}
				if (rightOp instanceof Local && source.baseMatches(rightOp)) {

					if (leftOp instanceof Local) {
						// e = d;
						out.add(source.deriveWithNewLocal((Local) leftOp));
					} else if (leftOp instanceof InstanceFieldRef) {
						// d.f = e;
						InstanceFieldRef fr = (InstanceFieldRef) leftOp;
						Value base = fr.getBase();
						SootField field = fr.getField();

						if (base instanceof Local) {
							Local lBase = (Local) base;
							computeAliasesOnInstanceWrite(curr, succ, source, lBase, field, (Local) rightOp, edge);
						}
					} else if (leftOp instanceof ArrayRef) {
						ArrayRef fr = (ArrayRef) leftOp;
						Value base = fr.getBase();

						if (base instanceof Local) {
							Local lBase = (Local) base;
							computeAliasesOnInstanceWrite(curr, succ, source, lBase, AliasFinder.ARRAY_FIELD,
									(Local) rightOp, edge);

						}
					} else if (leftOp instanceof StaticFieldRef && context.trackStaticFields()) {
						// d.f = e;
						StaticFieldRef fr = (StaticFieldRef) leftOp;
						SootField field = fr.getField();

						AccessGraph newAp = source.prependField(new WrappedSootField(field, curr))
								.makeStatic();

						if (newAp.hasSetBasedFieldGraph()) {
							newAp = source.dropTail()
									.prependField(new WrappedSootField(field, curr)).makeStatic();
							out.add(newAp);
						}
						out.add(newAp);
						return out;
					}
				} else if (rightOp instanceof InstanceFieldRef) {
					InstanceFieldRef fr = (InstanceFieldRef) rightOp;
					Value base = fr.getBase();
					SootField field = fr.getField();

					if (source.baseMatches(base) && source.firstFirstFieldMayMatch(field)) {
						// e = a.f && source == a.f.*
						// replace in source
						if (leftOp instanceof Local && !source.baseMatches(leftOp)) {
							AccessGraph deriveWithNewLocal = source.deriveWithNewLocal((Local) leftOp);
							out.addAll(deriveWithNewLocal.popFirstField());
						}
					}
				} else if (rightOp instanceof ArrayRef) {
					ArrayRef arrayRef = (ArrayRef) rightOp;
					if (source.baseMatches(arrayRef.getBase())
							&& source.firstFirstFieldMayMatch(AliasFinder.ARRAY_FIELD)) {

						Set<AccessGraph> withoutFirstField = source.popFirstField();
						for (AccessGraph a : withoutFirstField) {
							out.add(a.deriveWithNewLocal((Local) leftOp));
						}
					}
				} else if (rightOp instanceof StaticFieldRef && context.trackStaticFields()) {
					StaticFieldRef sfr = (StaticFieldRef) rightOp;
					if (source.isStatic() && source.firstFieldMustMatch(sfr.getField())) {
						if (leftOp instanceof Local) {
							Set<AccessGraph> withoutFirstField = source.popFirstField();
							for (AccessGraph a : withoutFirstField) {
								out.add(a.deriveWithNewLocal((Local) leftOp));
							}
						}
					}
				}

				return out;
			}
		};
	}

	private void computeAliasesOnInstanceWrite(final Unit curr, final Unit succ, final AccessGraph source, Local lBase,
			final SootField field, Local rightLocal, final IPathEdge<Unit, AccessGraph> edge) {
		WrappedSootField[] toAppend;
		if (source.getFieldGraph() == null)
			toAppend = new WrappedSootField[] { new WrappedSootField(field, curr) };
		else
			toAppend = source.getFieldGraph().prependField(new WrappedSootField(field, curr))
					.getFields();
		if (toAppend.length > 0)
			context.registerPOI(curr, new PointOfIndirection(new AccessGraph(lBase), curr, context),
					new ForwardAliasCallback(edge.getStart(), edge.factAtSource(), succ, toAppend, context));
	}

	@Override
	public FlowFunction<AccessGraph> getCallFlowFunction(final IPathEdge<Unit, AccessGraph> edge,
			final SootMethod callee, Unit calleeSp) {
		assert callee != null;
		final Unit callSite = edge.getTarget();
		final Local[] paramLocals = new Local[callee.getParameterCount()];
		for (int i = 0; i < callee.getParameterCount(); i++)
			paramLocals[i] = callee.getActiveBody().getParameterLocal(i);

		final AccessGraph d1 = edge.factAtSource();
		final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
		return new FlowFunction<AccessGraph>() {
			@Override
			public Set<AccessGraph> computeTargets(AccessGraph source) {
				if (context.icfg.isIgnoredMethod(callee))
					Collections.emptySet();
				assert source != null;
				Set<AccessGraph> out = new HashSet<>();
				Stmt is = (Stmt) callSite;
				source = source.hasAllocationSite() ? source.deriveWithoutAllocationSite() : source;
				if (context.trackStaticFields() && source.isStatic()) {
					return Collections.singleton(source);
				}
				if (edge.factAtSource() != null) {
					if (context.icfg.isIgnoredMethod(callee)) {
						return Collections.emptySet();
					}
				}

				if (is.containsInvokeExpr()) {
					final InvokeExpr ie = is.getInvokeExpr();
					for (int i = 0; i < paramLocals.length; i++) {
						Value arg = ie.getArg(i);
						if (arg instanceof Local && source.baseMatches(arg)) {
							out.add(source.deriveWithNewLocal(paramLocals[i]));
						}
					}
					final Value[] callArgs = new Value[ie.getArgCount()];
					for (int i = 0; i < ie.getArgCount(); i++)
						callArgs[i] = ie.getArg(i);
					if (!context.forwardMockHandler.flowInto(callSite, source, ie, callArgs))
						return Collections.emptySet();

					if (!callee.isStatic() && ie instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) is.getInvokeExpr();

						if (source.baseMatches(iIExpr.getBase())) {
							if (d1 != null && d1.hasAllocationSite()
									&& (source.getFieldCount() < 1 && !source.hasSetBasedFieldGraph())) {
								Unit sourceStmt = d1.getSourceStmt();
								if (sourceStmt instanceof AssignStmt) {
									AssignStmt as = (AssignStmt) sourceStmt;
									Value rightOp = as.getRightOp();
									Type type = rightOp.getType();
									if (type instanceof RefType) {
										RefType refType = (RefType) type;
										SootClass typeClass = refType.getSootClass();
										SootClass methodClass = callee.getDeclaringClass();
										if (typeClass != null && methodClass != null && typeClass != methodClass
												&& !typeClass.isInterface()) {
											if (!Scene.v().getOrMakeFastHierarchy().isSubclass(typeClass,
													methodClass)) {
												return Collections.emptySet();
											}
										}
									} else if (type instanceof PrimType) {
										return Collections.emptySet();
									}

								}
							}

							AccessGraph replacedThisValue = source.deriveWithNewLocal(thisLocal);
							if (context.isValidAccessPath(replacedThisValue)) {
								out.add(replacedThisValue);
							}
						}
					}
				}
				return out;
			}
		};
	}

	@Override
	public FlowFunction<AccessGraph> getReturnFlowFunction(IPathEdge<Unit, AccessGraph> edge, final Unit callStmt,
			final SootMethod callee, final Unit returnSite) {
		final Local[] paramLocals = new Local[callee.getParameterCount()];
		for (int i = 0; i < callee.getParameterCount(); i++)
			paramLocals[i] = callee.getActiveBody().getParameterLocal(i);
		final Unit exitStmt = edge.getTarget();
		final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
		return new FlowFunction<AccessGraph>() {
			@Override
			public Set<AccessGraph> computeTargets(AccessGraph source) {
				HashSet<AccessGraph> out = new HashSet<AccessGraph>();
				if (context.icfg.isIgnoredMethod(callee))
					Collections.emptySet();
				// mapping of fields of AccessPath those will be killed in
				// callToReturn
				if (context.trackStaticFields() && source.isStatic())
					return Collections.singleton(source);

				if (callStmt instanceof Stmt) {
					Stmt is = (Stmt) callStmt;

					if (is.containsInvokeExpr()) {
						InvokeExpr ie = is.getInvokeExpr();
						for (int i = 0; i < paramLocals.length; i++) {

							if (paramLocals[i] == source.getBase()) {
								Value arg = ie.getArg(i);
								if (arg instanceof Local) {
									AccessGraph deriveWithNewLocal = source.deriveWithNewLocal((Local) arg);
									out.add(deriveWithNewLocal);
								}

							}
						}
						if (!callee.isStatic() && ie instanceof InstanceInvokeExpr) {
							if (source.baseMatches(thisLocal)) {
								InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) is.getInvokeExpr();
								AccessGraph possibleAccessPath = source.deriveWithNewLocal((Local) iIExpr.getBase());
								out.add(possibleAccessPath);
							}
						}
					}
				}

				if (callStmt instanceof AssignStmt && exitStmt instanceof ReturnStmt) {
					AssignStmt as = (AssignStmt) callStmt;
					Value leftOp = as.getLeftOp();
					// mapping of return value

					ReturnStmt returnStmt = (ReturnStmt) exitStmt;
					Value returns = returnStmt.getOp();
					// d = return out;
					if (leftOp instanceof Local) {
						if (returns instanceof Local && source.getBase() == returns) {
							out.add(source.deriveWithNewLocal((Local) leftOp));
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

				if (context.trackStaticFields() && source.isStatic()) {
					return Collections.emptySet();
				}
				Set<AccessGraph> out = new HashSet<>();
				boolean sourceIsKilled = false;
				if (callSite instanceof AssignStmt) {
					AssignStmt as = (AssignStmt) callSite;
					Value leftOp = as.getLeftOp();
					if (source.getBase() == leftOp) {
						return Collections.emptySet();
					}
				}

				Stmt is = (Stmt) callSite;
				if (is.containsInvokeExpr()) {
					final InvokeExpr ie = is.getInvokeExpr();
					final Value[] callArgs = new Value[ie.getArgCount()];
					for (int i = 0; i < ie.getArgCount(); i++)
						callArgs[i] = ie.getArg(i);

					if (context.forwardMockHandler.handles(callSite, ie, source, callArgs)) {
						return context.forwardMockHandler.computeTargetsOverCall(callSite, ie, source, callArgs, edge);
					}

					if (ie.getMethod().equals(AliasFinder.arrayCopy())) {
						for (Value callVal : callArgs) {
							if (callVal == source.getBase()) {
								// java uses call by value, but fields of
								// complex objects can be changed (and
								// tainted), so use this conservative approach:
								Set<AccessGraph> nativeAbs = context.ncHandler.getForwardValues(is, source, callArgs);
								out.addAll(nativeAbs);
							}
						}
					}

					for (int i = 0; i < ie.getArgCount(); i++) {
						Value arg = ie.getArg(i);
						if (source.getBase() == arg) {
							if (!callees.isEmpty()) {
								sourceIsKilled = true;
							}
						}
					}
					if (ie instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
						if (iie.getBase().equals(source.getBase())) {
							if (!callees.isEmpty()) {
								sourceIsKilled = true;
							}
						}
					}
				}
				if (!sourceIsKilled)
					out.add(source);

				return out;
			}
		};
	}

}
