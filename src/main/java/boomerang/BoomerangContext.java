package boomerang;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.lang.model.type.PrimitiveType;

import com.google.common.base.Stopwatch;

import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.WrappedSootField;
import boomerang.backward.BackwardProblem;
import boomerang.backward.BackwardSolver;
import boomerang.bidi.PathEdgeStore;
import boomerang.cfg.BackwardsInfoflowCFG;
import boomerang.context.IContextRequester;
import boomerang.debug.IBoomerangDebugger;
import boomerang.debug.JSONOutputDebugger;
import boomerang.forward.ForwardFlowFunctions;
import boomerang.forward.ForwardProblem;
import boomerang.forward.ForwardSolver;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IPropagationController;
import boomerang.ifdssolver.PathEdge;
import boomerang.mock.DefaultBackwardDataFlowMocker;
import boomerang.mock.DefaultForwardDataFlowMocker;
import boomerang.mock.DefaultNativeCallHandler;
import boomerang.mock.MockedDataFlow;
import boomerang.mock.NativeCallHandler;
import boomerang.pointsofindirection.AliasCallback;
import boomerang.pointsofindirection.AllocationSiteHandlers;
import boomerang.pointsofindirection.PointOfIndirection;
import heros.FlowFunction;
import heros.solver.Pair;
import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ReturnStmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class BoomerangContext {

	/**
	 * The inter-procedural control flow graph to be used.
	 */
	public BiDiInterproceduralCFG<Unit, SootMethod> icfg;

	public IBoomerangDebugger debugger;

	/**
	 * The inter-procedural backward control flow graph to be used.
	 */
	public BiDiInterproceduralCFG<Unit, SootMethod> bwicfg;

	/**
	 * Native call handler, defines how aliases flow at native call sites.
	 */
	public NativeCallHandler ncHandler = new DefaultNativeCallHandler();

	/**
	 * Can be used to specify forward flow function for certain functions.
	 */
	public MockedDataFlow forwardMockHandler = new DefaultForwardDataFlowMocker(this);

	/**
	 * Can be used to specify backward flow function for certain functions.
	 */
	public MockedDataFlow backwardMockHandler = new DefaultBackwardDataFlowMocker(this);

	Stopwatch startTime;

	private Set<SootMethod> backwardVisitedMethods = new HashSet<>();
	private Set<SootMethod> visitableMethods = new HashSet<>();

	public ContextScheduler scheduler;

	private BoomerangOptions options;

	public BoomerangContext(BoomerangOptions options) {
		this.options = options;
		this.icfg = options.icfg();
		this.bwicfg = new BackwardsInfoflowCFG(icfg);
		this.debugger = options.getDebugger();
		if (debugger instanceof JSONOutputDebugger)
			System.err.println("WARNING: Using JSON output slows down performance");
		this.debugger.setContext(this);
		WrappedSootField.TRACK_STMT = options.getTrackStatementsInFields();
		this.scheduler = options.getScheduler();
		this.scheduler.setContext(this);
		this.propagationController = options.propagationController();

	}

	public boolean isValidAccessPath(AccessGraph a) {
		return true;
	}

	public boolean isIgnoredMethod(SootMethod method) {
		return false;
	}
	
	public boolean isStaticFieldUsed(SootMethod method, SootField sootField) {
		return true;
	}
	
	public boolean isParameterOrThisValue(Unit stmtInMethod, Local local) {
		SootMethod method = bwicfg.getMethodOf(stmtInMethod);
		return isParameterOrThisValue(method, local);
	}

	public static boolean isParameterOrThisValue(SootMethod method, Local local) {
		boolean isParameter = method.getActiveBody().getParameterLocals().contains(local);
		if (isParameter)
			return true;
		return isThisValue(method, local);
	}

	public static boolean isThisValue(SootMethod method, Local local) {
		if (!method.isStatic()) {
			return method.getActiveBody().getThisLocal().equals(local);
		}
		return false;
	}

	public void sanityCheckEdge(IPathEdge<Unit, AccessGraph> edge) {
		if (edge.getStart() == null)
			return;
		SootMethod m1 = icfg.getMethodOf(edge.getStart());
		SootMethod m2 = icfg.getMethodOf(edge.getTarget());
		assert m1 == m2 : "The path edge " + edge + "contains statements of two different method: " + m1.toString()
				+ " and " + m2.toString();
		;
	}

	public boolean isReturnValue(SootMethod method, Local base) {
		Collection<Unit> endPointsOf = icfg.getEndPointsOf(method);

		for (Unit eP : endPointsOf) {
			if (eP instanceof ReturnStmt) {
				ReturnStmt returnStmt = (ReturnStmt) eP;
				Value op = returnStmt.getOp();
				if (op.equals(base))
					return true;
			}
		}
		return false;
	}

	public boolean isValidQuery(AccessGraph ap, Unit stmt) {
		SootMethod m = bwicfg.getMethodOf(stmt);
		if (!ap.isStatic() && !m.getActiveBody().getLocals().contains(ap.getBase())) {
			return false;
		}

		if (ap.getBase() instanceof PrimitiveType) {
			return false;
		}
		return true;
	};

	private ForwardSolver forwardSolver;

	private BackwardSolver backwardSolver;

	private IContextRequester contextRequester;

	public IPropagationController<Unit, AccessGraph> propagationController;

	public boolean isOutOfBudget() {
		if (startTime.elapsed(TimeUnit.MILLISECONDS) > options.getTimeBudget())
			return true;
		return false;
	}

	public void validateInput(AccessGraph ap, Unit stmt) {
		SootMethod m = bwicfg.getMethodOf(stmt);
		if (!ap.isStatic() && !m.getActiveBody().getLocals().contains(ap.getBase())) {
			throw new IllegalArgumentException(
					"Base value of access path " + ap + " is not a local of the Method at which the Query was asked!");
		}
		if (stmt == null)
			throw new IllegalArgumentException("Statment must not be null");

		if (ap.getBase() instanceof PrimitiveType) {
			throw new IllegalArgumentException("The queried variable is not of pointer type");
		}
	}

	public boolean trackStaticFields() {
		return options.getTrackStaticFields();
	}

	public boolean visitedBackwardMethod(SootMethod m) {
		return backwardVisitedMethods.contains(m);
	}

	public void addAsVisitedBackwardMethod(SootMethod m) {
		backwardVisitedMethods.add(m);
	}

	public ForwardSolver getForwardSolver() {
		if (forwardSolver == null) {
			ForwardProblem forwardProblem = new ForwardProblem(this);
			forwardSolver = new ForwardSolver(forwardProblem, this);
		}
		return forwardSolver;
	}

	public BackwardSolver getBackwardSolver() {
		if (backwardSolver == null) {
			BackwardProblem problem = new BackwardProblem(this);
			backwardSolver = new BackwardSolver(problem, this);
		}
		return backwardSolver;
	}

	public PathEdgeStore getForwardPathEdges() {
		return (PathEdgeStore) getForwardSolver().getPathEdges();
	}

	public Set<? extends IPathEdge<Unit, AccessGraph>> getForwardIncomings(Pair<Unit, AccessGraph> startNode) {
		return getForwardSolver().incoming(startNode, icfg.getMethodOf(startNode.getO1()));
	}

	public void registerPOI(Unit stmt, PointOfIndirection poi, AliasCallback cb) {
		getForwardPathEdges().registerPointOfIndirectionAt(stmt, poi, cb);
	}

	public void setContextRequester(IContextRequester req) {
		this.contextRequester = req;
	}

	public IContextRequester getContextRequester() {
		return contextRequester;
	}

	public Set<AccessGraph> getForwardTargetsFor(AccessGraph d2, Unit callSite, SootMethod callee) {
		Collection<Unit> calleeSps = this.icfg.getStartPointsOf(callee);
		Set<AccessGraph> factsInCallee = new HashSet<>();
		ForwardFlowFunctions ptsFunction = new ForwardFlowFunctions(this);
		for (Unit calleeSp : calleeSps) {
			FlowFunction<AccessGraph> callFlowFunction = ptsFunction.getCallFlowFunction(
					new PathEdge<Unit, AccessGraph>(callSite, null, callSite, null), callee, calleeSp);
			Set<AccessGraph> targets = callFlowFunction.computeTargets(d2);
			factsInCallee.addAll(targets);
		}
		return factsInCallee;
	}
	
	public BoomerangOptions getOptions() {
		return options;
	}

	public AllocationSiteHandlers allocationSiteHandlers() {
		return options.allocationSiteHandlers();
	}

	public boolean visitableMethod(SootMethod callee) {
		return visitableMethods.contains(callee);
	}
	
	public void addVisitableMethod(SootMethod m){
		visitableMethods.add(m);
		getBackwardSolver().setVisitable(m);
		getForwardSolver().setVisitable(m);
	}

}
