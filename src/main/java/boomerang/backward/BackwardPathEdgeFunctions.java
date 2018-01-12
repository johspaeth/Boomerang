package boomerang.backward;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.accessgraph.AccessGraph;
import boomerang.ap.AliasResults;
import boomerang.ap.BoomerangContext;
import boomerang.forward.AbstractPathEdgeFunctions;
import boomerang.ifdssolver.DefaultIFDSTabulationProblem.Direction;
import boomerang.pointsofindirection.Alloc;
import boomerang.ifdssolver.FlowFunctions;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.PathEdge;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
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
		return Collections.singleton(succEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> callFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, final IPathEdge<Unit, AccessGraph> initialSelfLoop,
			SootMethod callee) {

		PathEdge<Unit, AccessGraph> pathEdge = new PathEdge<>(null, initialSelfLoop.factAtSource(), initialSelfLoop.getTarget(),
				initialSelfLoop.factAtTarget());
		if(!context.visitableMethod(callee)){
			context.getBackwardSolver().addMethodToPausedEdge(callee, pathEdge);
			if(!callee.isStatic()){
				Unit callSite = prevEdge.getTarget();
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
						}
					}
				}
			}
			return Collections.emptySet();
		}
		context.addAsVisitedBackwardMethod(callee);
		return Collections.singleton(pathEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> balancedReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> calleeEdge, IPathEdge<Unit, AccessGraph> succEdge,
			IPathEdge<Unit, AccessGraph> incEdge) {

		return Collections.singleton(succEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> call2ReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> succEdge) {
		context.addAsVisitedBackwardMethod(context.icfg.getMethodOf(succEdge.getTarget()));
		return Collections.singleton(succEdge);
	}

	@Override
	protected Collection<? extends IPathEdge<Unit, AccessGraph>> unbalancedReturnFunctionExtendor(
			IPathEdge<Unit, AccessGraph> prevEdge, IPathEdge<Unit, AccessGraph> succEdge, Unit callSite,
			Unit returnSite) {
		if(callSite == null && returnSite == null){
			SootMethod callee = context.icfg.getMethodOf(prevEdge.getTarget());
			if(context.getContextRequester().isEntryPointMethod(callee)){
				Alloc alloc = new Alloc(prevEdge.factAtTarget(), prevEdge.getTarget(),true);
				alloc.execute(context);
			}
			return Collections.emptySet();
		}
		succEdge = new PathEdge<Unit, AccessGraph>(null, succEdge.factAtTarget(), succEdge.getTarget(),
				succEdge.factAtTarget());
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
