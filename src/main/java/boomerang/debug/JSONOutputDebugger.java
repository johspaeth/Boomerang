package boomerang.debug;

import java.io.File;
import java.util.Collection;

import boomerang.AliasResults;
import boomerang.BoomerangContext;
import boomerang.Query;
import boomerang.accessgraph.AccessGraph;
import boomerang.ifdssolver.DefaultIFDSTabulationProblem.Direction;
import boomerang.ifdssolver.IPathEdge;
import heros.debug.visualization.ExplodedSuperGraph;
import heros.debug.visualization.IDEToJSON;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public class JSONOutputDebugger implements IBoomerangDebugger {
	private BoomerangContext context;
	private IInfoflowCFG icfg;
	private File jsonFile;
	private IDEToJSON<SootMethod, Unit, AccessGraph, Object, IInfoflowCFG> forwardJSON;

	public JSONOutputDebugger(File jsonFile) {
		this.jsonFile = jsonFile;
	}

	@Override
	public void addIncoming(Direction direction, SootMethod callee, Pair<Unit, AccessGraph> pair,
			IPathEdge<Unit, AccessGraph> pe) {
	}

	@Override
	public void addSummary(Direction dir, SootMethod methodToSummary, IPathEdge<Unit, AccessGraph> summary) {
		for (Unit callSite : icfg.getCallersOf(methodToSummary)) {
			ExplodedSuperGraph cfg = forwardJSON.getOrCreateESG(icfg.getMethodOf(callSite), convertDirection(dir));
			for (Unit start : icfg.getStartPointsOf(methodToSummary)) {
				cfg.addSummary(cfg.new ESGNode(start, summary.factAtSource()), cfg.new ESGNode(summary.getTarget(),summary.factAtTarget()));
			}
		}
	}

	@Override
	public void normalFlow(Direction dir, Unit start, AccessGraph startFact, Unit target, AccessGraph targetFact) {
		forwardJSON.getOrCreateESG(icfg.getMethodOf(start), convertDirection(dir)).normalFlow(start, startFact, target, targetFact);
	}

	private heros.debug.visualization.IDEToJSON.Direction convertDirection(Direction dir){
		if(dir == Direction.BACKWARD)
			return heros.debug.visualization.IDEToJSON.Direction.Backward;
		return heros.debug.visualization.IDEToJSON.Direction.Forward;
	}

	@Override
	public void callFlow(Direction dir, Unit start, AccessGraph startFact, Unit target, AccessGraph targetFact) {
		forwardJSON.getOrCreateESG(icfg.getMethodOf(start), convertDirection(dir)).callFlow(start, startFact, target, targetFact);
	}

	@Override
	public void callToReturn(Direction dir, Unit start, AccessGraph startFact, Unit target, AccessGraph targetFact) {
		forwardJSON.getOrCreateESG(icfg.getMethodOf(start), convertDirection(dir)).callToReturn(start, startFact, target, targetFact);
	}

	@Override
	public void returnFlow(Direction dir, Unit start, AccessGraph startFact, Unit target, AccessGraph targetFact) {
		forwardJSON.getOrCreateESG(icfg.getMethodOf(target), convertDirection(dir)).returnFlow(start, startFact, target, targetFact);
	}

	@Override
	public void indirectFlowEdgeAtRead(AccessGraph startFact, Unit start, AccessGraph targetFact, Unit target) {
		ExplodedSuperGraph<SootMethod, Unit, AccessGraph, Object> esg = forwardJSON.getOrCreateESG(icfg.getMethodOf(target),heros.debug.visualization.IDEToJSON.Direction.Backward);
		esg.addEdgeWithLabel(icfg.getMethodOf(start), esg.new ESGNode(start, startFact), esg.new ESGNode(target, targetFact), "indirectReadFlow");
	}

	@Override
	public void indirectFlowEdgeAtWrite(AccessGraph startFact, Unit start, AccessGraph targetFact, Unit target) {
		ExplodedSuperGraph<SootMethod, Unit, AccessGraph, Object>  esg = forwardJSON.getOrCreateESG(icfg.getMethodOf(target),heros.debug.visualization.IDEToJSON.Direction.Forward);
		esg.addEdgeWithLabel(icfg.getMethodOf(start), esg.new ESGNode(start, startFact),esg.new ESGNode(target, targetFact), "indirectWriteFlow");
	}

	@Override
	public void indirectFlowEdgeAtReturn(AccessGraph startFact, Unit start, AccessGraph targetFact, Unit target) {
		ExplodedSuperGraph<SootMethod, Unit, AccessGraph, Object> esg = forwardJSON.getOrCreateESG(icfg.getMethodOf(target),heros.debug.visualization.IDEToJSON.Direction.Forward);
		esg.addEdgeWithLabel(icfg.getMethodOf(start), esg.new ESGNode(start, startFact),esg.new ESGNode(target, targetFact), "indirectReturnFlow");
	}

	@Override
	public void indirectFlowEdgeAtCall(AccessGraph startFact, Unit start, AccessGraph targetFact, Unit target) {
		ExplodedSuperGraph<SootMethod, Unit, AccessGraph, Object>  esg = forwardJSON.getOrCreateESG(icfg.getMethodOf(start),heros.debug.visualization.IDEToJSON.Direction.Backward);
		esg.addEdgeWithLabel(icfg.getMethodOf(start), esg.new ESGNode(start, startFact), esg.new ESGNode(target, targetFact), "indirectCallFlow");
	}
	
	@Override
	public void backwardStart(Direction backward, Unit stmt, AccessGraph d1, Unit s) {
	}

	@Override
	public void onEnterCall(Unit n, Collection<? extends IPathEdge<Unit, AccessGraph>> nextCallEdges,
			IPathEdge<Unit, AccessGraph> incEdge) {
	}

	@Override
	public void onProcessCall(IPathEdge<Unit, AccessGraph> edge) {
	}

	@Override
	public void onProcessExit(IPathEdge<Unit, AccessGraph> edge) {
	}

	@Override
	public void onProcessNormal(IPathEdge<Unit, AccessGraph> edge) {
	}

	@Override
	public void finishedQuery(Query q, AliasResults res) {
	}

	@Override
	public void startQuery(Query q) {
//		mainMethodId = id(icfg.getMethodOf(q.getStmt()));
	}

	@Override
	public void onCurrentlyProcessingRecursiveQuery(Query q) {
	}

	@Override
	public void onLoadingQueryFromCache(Query q, AliasResults aliasResults) {
	}

	@Override
	public void onAllocationSiteReached(AssignStmt as, IPathEdge<Unit, AccessGraph> pe) {
	}

	@Override
	public void onAliasQueryFinished(Query q, AliasResults res) {
		forwardJSON.writeToFile();
	}


	@Override
	public void onAliasTimeout(Query q) {
		forwardJSON.writeToFile();
	}


	private String getShortLabel(Unit u) {
		if (u instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) u;
			if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
				InstanceFieldRef fr = (InstanceFieldRef) assignStmt.getRightOp();
				return assignStmt.getLeftOp() + " = " + fr.getBase() + "." + fr.getField().getName();
			}
			if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
				InstanceFieldRef fr = (InstanceFieldRef) assignStmt.getLeftOp();
				return fr.getBase() + "." + fr.getField().getName() + " = " + assignStmt.getRightOp();
			}
		}
		if (u instanceof Stmt && ((Stmt) u).containsInvokeExpr()) {
			InvokeExpr invokeExpr = ((Stmt) u).getInvokeExpr();
			if (invokeExpr instanceof StaticInvokeExpr)
				return (u instanceof AssignStmt ? ((AssignStmt) u).getLeftOp() + " = " : "")
						+ invokeExpr.getMethod().getName() + "("
						+ invokeExpr.getArgs().toString().replace("[", "").replace("]", "") + ")";
			if (invokeExpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
				return (u instanceof AssignStmt ? ((AssignStmt) u).getLeftOp() + " = " : "") + iie.getBase() + "."
						+ invokeExpr.getMethod().getName() + "("
						+ invokeExpr.getArgs().toString().replace("[", "").replace("]", "") + ")";
			}
		}
		return u.toString();
	}


	@Override
	public void setContext(BoomerangContext boomerangContext) {
		this.context = boomerangContext;
		this.icfg = context.icfg;
		
		forwardJSON = new BoomerangToJSON(jsonFile, icfg);
	
	}
	
	private class BoomerangToJSON extends IDEToJSON<SootMethod, Unit, AccessGraph, Object, IInfoflowCFG>{
		public BoomerangToJSON(File file, IInfoflowCFG icfg) {
			super(file, icfg);
		}

		@Override
		public String getShortLabel(Unit u) {
			return JSONOutputDebugger.this.getShortLabel(u);
		}
		
	}
	
}
