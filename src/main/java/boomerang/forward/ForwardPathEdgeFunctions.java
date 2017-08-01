package boomerang.forward;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.WrappedSootField;
import boomerang.ifdssolver.DefaultIFDSTabulationProblem.Direction;
import boomerang.ifdssolver.FlowFunctions;
import boomerang.ifdssolver.IFDSSolver.PropagationType;
import boomerang.ifdssolver.IPathEdge;
import boomerang.pointsofindirection.ForwardAliasCallback;
import boomerang.pointsofindirection.PointOfIndirection;
import boomerang.pointsofindirection.StrongUpdateCallback;
import heros.solver.Pair;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;

class ForwardPathEdgeFunctions extends AbstractPathEdgeFunctions {

	private Multimap<Unit,IPathEdge<Unit,AccessGraph>> pausedAtCastStmt = HashMultimap.create();
	private Set<Unit> activatedCasts = Sets.newHashSet();
	ForwardPathEdgeFunctions(FlowFunctions<Unit, AccessGraph, SootMethod> flowFunctions, BoomerangContext c) {
		super(flowFunctions, c, Direction.FORWARD);
	}


	@Override
	public Collection<? extends IPathEdge<Unit, AccessGraph>> unbalancedReturnFunction(
			IPathEdge<Unit, AccessGraph> currEdge, Unit callSite, Unit returnSite, SootMethod callee) {

		// Unbalanced return only occurs when the start statement of the path
		// edge is not the first
		// statement of the method, i.e. a NopStmt
		if (!currEdge.factAtSource().hasAllocationSite()) {
			return Collections.emptySet();
		}
		return super.unbalancedReturnFunction(currEdge, callSite, returnSite, callee);

	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> normalFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, final IPathEdge<Unit, AccessGraph> succEdge) {
		assert prevEdge.getStartNode().equals(succEdge.getStartNode());

		if(context.getOptions().stronglyUpdateFields()){
			Unit curr = prevEdge.getTarget();
			if (curr instanceof AssignStmt && ((AssignStmt) curr).getLeftOp() instanceof InstanceFieldRef) {
				InstanceFieldRef instanceFieldRef = (InstanceFieldRef) ((AssignStmt) curr).getLeftOp();
				Value base = instanceFieldRef.getBase();
				if (prevEdge.factAtTarget().firstFieldMustMatch(instanceFieldRef.getField())) {
					// System.out.println("STRONG UPDATE " + curr);
					StrongUpdateCallback strongUpdateCallback = new StrongUpdateCallback(succEdge, context);
					context.getForwardPathEdges().registerPointOfIndirectionAt(curr,
							new PointOfIndirection(new AccessGraph((Local) base), curr, context),
							strongUpdateCallback);
					context.getForwardPathEdges().registerPointOfIndirectionAt(curr,
							new PointOfIndirection(prevEdge.factAtTarget().dropTail(), curr, context),
							strongUpdateCallback);
					return Collections.emptySet();
				}
			}
		}
		
		if(context.getOptions().typeCheckForCasts()){
			final Unit curr = prevEdge.getTarget();
			if(curr instanceof AssignStmt && ((AssignStmt) curr).getRightOp() instanceof CastExpr){
				final CastExpr cast = (CastExpr)((AssignStmt) curr).getRightOp();
				AccessGraph target = succEdge.factAtTarget();
				if(!target.isStatic() && target.getBase() != null &&  target.getBase().equals(((AssignStmt) curr).getLeftOp())){
					pauseEdgeAtCast(succEdge, curr);
					if(target.getFieldCount() == 0 && !target.hasSetBasedFieldGraph()){
						context.getForwardSolver().attachAllocationListener(succEdge.getStartNode(),context.icfg.getMethodOf(succEdge.getTarget()), new AllocationTypeListener(){
	
							@Override
							public void discoveredAllocationType(Type allocType) {
								if(Scene.v().getOrMakeFastHierarchy().canStoreType(allocType,cast.getCastType())){
									activateCastStmt(curr);
								}
							}
						});
					}
					return Collections.emptySet();
				}
			}
		}
		return Collections.singleton(succEdge);
	}

	protected void activateCastStmt(Unit castStmt) {
		if(!activatedCasts.add(castStmt))
			return;
		for(IPathEdge<Unit,AccessGraph> pausedEdge : pausedAtCastStmt.removeAll(castStmt)){
			context.getForwardSolver().inject(pausedEdge, PropagationType.Normal);
		}
	}


	private void pauseEdgeAtCast(IPathEdge<Unit, AccessGraph> succEdge, Unit castStmt) {
		if(activatedCasts.contains(castStmt)){
			context.getForwardSolver().inject(succEdge, PropagationType.Normal);
		} else{
			pausedAtCastStmt.put(castStmt,succEdge);
		}
	}


	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> call2ReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> succEdge) {
		assert prevEdge.getStartNode().equals(succEdge.getStartNode());
		sanitize(Collections.singleton(succEdge));
		return Collections.singleton(succEdge);
	}

	@Override
	protected Collection<IPathEdge<Unit, AccessGraph>> unbalancedReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, final IPathEdge<Unit, AccessGraph> succEdge, Unit callSite,
			Unit returnSite) {
		context.sanityCheckEdge(succEdge);
		context.sanityCheckEdge(prevEdge);
		SootMethod caller = context.icfg.getMethodOf(succEdge.getTarget());
		AccessGraph d1 = prevEdge.factAtSource();

		AccessGraph targetFact = succEdge.factAtTarget();
		if (d1.hasAllocationSite()) {
			if (targetFact.getFieldCount() > 0) {
				if (targetFact.getLastField() != null && !targetFact.isStatic() && !targetFact.hasSetBasedFieldGraph()) {
					for (final WrappedSootField field : targetFact.getLastField()) {
						Set<AccessGraph> withoutLast = targetFact.popLastField();
						if (withoutLast == null)
							continue;
						for (AccessGraph subgraph : withoutLast) {
							context.registerPOI(callSite, new PointOfIndirection(subgraph, callSite, context),
									new ForwardAliasCallback(callSite, d1, succEdge.getTarget(),
											new WrappedSootField[] { field }, context));
						}
					}
				}
			}
			if(caller != null && !context.visitableMethod(caller)){
				context.getForwardSolver().addMethodToPausedEdge(caller, succEdge);
				return Collections.emptySet();
			}
			return Collections.singleton(succEdge);
		}
		return Collections.emptySet();
	}

	private boolean isOverridenByCall(AccessGraph ap, Unit callSite) {
		if (ap.isStatic())
			return false;
		if (!(callSite instanceof AssignStmt))
			return false;
		if (callSite instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) callSite;
			if (as.getLeftOp().equals(ap.getBase()))
				return true;
		}
		return false;
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> balancedReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> succEdge,
			IPathEdge<Unit, AccessGraph> incEdge) {
		AccessGraph targetFact = succEdge.factAtTarget();
		// For balanced problems we continue with the path edge which actually
		// was incoming!
		assert incEdge.getStartNode().equals(succEdge.getStartNode());
		if (succEdge.factAtTarget().getFieldCount() > 0 && !isIdentityEdge(prevEdge)) {
			sanitize(succEdge);
			createAliasEdgesOnBalanced(incEdge.getTarget(), succEdge);
		}
		return Collections.singleton(succEdge);
	}

	private void createAliasEdgesOnBalanced(Unit callSite, final IPathEdge<Unit, AccessGraph> succEdge) {
		AccessGraph d2 = succEdge.factAtTarget();
		if (isOverridenByCall(d2, callSite))
			return;
		if (d2.getLastField() == null || d2.hasSetBasedFieldGraph() || d2.isStatic())
			return;
		for (final WrappedSootField field : d2.getLastField()) {
			Set<AccessGraph> withoutLast = d2.popLastField();
			if (withoutLast == null)
				continue;
			for (AccessGraph subgraph : withoutLast) {
				context.registerPOI(callSite, new PointOfIndirection(subgraph, callSite, context),
						new ForwardAliasCallback(succEdge.getStart(), succEdge.factAtSource(), succEdge.getTarget(),
								new WrappedSootField[] { field }, context));
			}
		}
	}

	private boolean isIdentityEdge(IPathEdge<Unit, AccessGraph> edge) {
		AccessGraph source = edge.factAtSource();
		AccessGraph target = edge.factAtTarget();
		if (source == null || target == null)
			return false;
		if (source.isStatic() || target.isStatic())
			return false;
		return source.getBase().equals(target.getBase()) && target.getFirstField().equals(source.getFirstField());
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> callFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> initialSelfLoopEdge,
			final SootMethod callee) {
		AccessGraph sourceFact = initialSelfLoopEdge.factAtSource();
		if(!context.getOptions().onTheFlyCallGraphGeneration()){
			context.addVisitableMethod(callee);
		} else if(callee.isStatic()){
			if(!sourceFact.isStatic())	
				context.addVisitableMethod(callee);
		} else{
			if(!sourceFact.isStatic() && callee.getActiveBody().getThisLocal().equals(sourceFact.getBase())){
				if(sourceFact.getFieldCount() == 0 && !sourceFact.hasSetBasedFieldGraph()){
					SootMethod m = context.icfg.getMethodOf(prevEdge.getTarget());
					context.getForwardSolver().attachAllocationListener(prevEdge.getStartNode(),m, new AllocationTypeListener(){
						@Override
						public void discoveredAllocationType(Type allocType) {
							if(Scene.v().getOrMakeFastHierarchy().canStoreType(allocType, callee.getActiveBody().getThisLocal().getType()))
								context.addVisitableMethod(callee);
						}
					});
				}
			} else{
				if(!sourceFact.isStatic())
					context.addVisitableMethod(callee);
			}
		}
		

		if(!context.visitableMethod(callee)){
			context.getForwardSolver().addMethodToPausedEdge(callee, initialSelfLoopEdge);
			return Collections.emptySet();
		}
		sanitize(Collections.singleton(initialSelfLoopEdge));
		return Collections.singleton(initialSelfLoopEdge);
	}


	@Override
	public Collection<? extends IPathEdge<Unit, AccessGraph>> summaryCallback(SootMethod methodThatNeedsSummary,
			IPathEdge<Unit, AccessGraph> edge) {
		return Collections.emptySet();
	}
}
