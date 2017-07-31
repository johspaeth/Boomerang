package boomerang.backward;

import java.util.Collection;
import java.util.Collections;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.forward.AbstractPathEdgeFunctions;
import boomerang.ifdssolver.DefaultIFDSTabulationProblem.Direction;
import boomerang.ifdssolver.FlowFunctions;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.PathEdge;
import boomerang.pointsofindirection.Alloc;
import heros.solver.Pair;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

class BackwardPathEdgeFunctions extends AbstractPathEdgeFunctions {
	
	
	BackwardPathEdgeFunctions(FlowFunctions<Unit, AccessGraph, SootMethod> flowFunctions, BoomerangContext context) {
		super(flowFunctions, context, Direction.BACKWARD);
	}


	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> normalFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> succEdge) {
		SootMethod m = context.icfg.getMethodOf(succEdge.getTarget());
		if(!context.visitableMethod(m)){
			context.getBackwardSolver().addMethodToPausedEdge(m, succEdge);
			return Collections.emptySet();
		}
		return Collections.singleton(succEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> callFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, final IPathEdge<Unit, AccessGraph> initialSelfLoop,
			SootMethod callee) {

		PathEdge<Unit, AccessGraph> pathEdge = new PathEdge<>(null, initialSelfLoop.factAtSource(), initialSelfLoop.getTarget(),
				initialSelfLoop.factAtTarget());
		Unit callSite = prevEdge.getTarget();
		AccessGraph target = initialSelfLoop.factAtTarget();
		if(!context.getOptions().onTheFlyCallGraphGeneration()){
			context.addVisitableMethod(callee);
		} else if(context.getOptions().onTheFlyCallGraphGeneration() && !callee.isStatic()){
			if(callSite instanceof Stmt){
				Stmt stmt = (Stmt) callSite;
				if(stmt.containsInvokeExpr()){
					InvokeExpr invokeExpr = stmt.getInvokeExpr();
					if(invokeExpr instanceof InstanceInvokeExpr){
						InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
						Value base = iie.getBase();
						if(base instanceof Local){
							context.getBackwardSolver().startPropagation(new AccessGraph((Local) base), callSite);
						}
						if (target.getFieldCount() == 0 && !target.hasSetBasedFieldGraph() && target.baseMatches(base))
							return Collections.emptySet();
					}
				}
			}
		} else if(callee.isStatic()){
			if(!target.isStatic())
				context.addVisitableMethod(callee);
		}
		return Collections.singleton(pathEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> balancedReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> calleeEdge, IPathEdge<Unit, AccessGraph> succEdge,
			IPathEdge<Unit, AccessGraph> incEdge) {
		SootMethod caller = context.icfg.getMethodOf(incEdge.getTarget());
		if(!context.visitableMethod(caller)){
			context.getBackwardSolver().addMethodToPausedEdge(caller, succEdge);
			return Collections.emptySet();
		}
		return Collections.singleton(succEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> call2ReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> succEdge) {
		return Collections.singleton(succEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> unbalancedReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> succEdge, Unit callSite,
			Unit returnSite) {
		SootMethod callee = context.icfg.getMethodOf(prevEdge.getTarget());
		AccessGraph target = prevEdge.factAtTarget();
		boolean isParamOrStatic = (target.isStatic() || (target.getBase() != null && BoomerangContext.isParameterOrThisValue(callee, target.getBase())));
		if(isParamOrStatic){
			if(callSite == null && returnSite == null){
				if(context.getContextRequester().isEntryPointMethod(callee)){
					for(Type type : prevEdge.factAtTarget().getTypes()){
						Alloc alloc = new Alloc(prevEdge.factAtTarget(), prevEdge.getTarget(), type, true);
						alloc.execute(context,prevEdge);
					}
				}
				return Collections.emptySet();
			} else if(!target.isStatic()){
				if(context.isExpandingContext(callee)){
					if(context.getContextRequester().continueAtCallSite(callSite, callee)){
						context.addVisitableMethod(context.icfg.getMethodOf(callSite));
						context.expandContext(context.icfg.getMethodOf(callSite));
					} else{
						for(Type type : prevEdge.factAtTarget().getTypes()){
							Alloc alloc = new Alloc(prevEdge.factAtTarget(), prevEdge.getTarget(), type, true);
							alloc.execute(context,prevEdge);
						}
					}
				}
			}
			
		}
		
		succEdge = new PathEdge<Unit, AccessGraph>(null, succEdge.factAtSource(), succEdge.getTarget(),
				succEdge.factAtTarget());
		
		if(callSite == null)
			return Collections.emptySet();
		if(!context.visitableMethod(context.icfg.getMethodOf(callSite))){
			context.getBackwardSolver().addMethodToPausedEdge(context.icfg.getMethodOf(callSite), succEdge);
			return Collections.emptySet();
		}
		return Collections.singleton(succEdge);
	}

	@Override
	public Collection<? extends IPathEdge<Unit, AccessGraph>> summaryCallback(SootMethod methodThatNeedsSummary,
			IPathEdge<Unit, AccessGraph> edge) {
		return Collections.emptySet();
	}

	@Override
	public void cleanup() {
	}

}
