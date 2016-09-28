package boomerang;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import boomerang.accessgraph.AccessGraph;
import boomerang.bidi.Incomings;
import boomerang.bidi.PathEdgeStore;
import boomerang.bidi.PathEdgeStore.Direction;
import boomerang.cache.Query;
import boomerang.forward.ForwardSolver;
import boomerang.ifdssolver.IPathEdge;
import boomerang.pathconstructor.BackwardsPathConstructor;
import boomerang.pointsofindirection.Alloc;
import boomerang.pointsofindirection.BackwardParameterTurnHandler;
import boomerang.pointsofindirection.Call;
import boomerang.pointsofindirection.ForwardPointOfIndirection;
import boomerang.pointsofindirection.Meeting;
import boomerang.pointsofindirection.PointOfIndirection;
import boomerang.pointsofindirection.Read;
import boomerang.pointsofindirection.Return;
import boomerang.pointsofindirection.Unbalanced;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class SubQueryContext {
	private Query query;
	private Incomings incomings = new Incomings();
	private Set<PointOfIndirection> directProcessedPOI = new HashSet<>();
	private BoomerangContext context;
	private PathEdgeStore FW_PATHEDGES;
	private PathEdgeStore BW_PATHEDGES;
	private LinkedList<PointOfIndirection> worklist = new LinkedList<>();
	private BackwardsPathConstructor<Unit, AccessGraph, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> BW_PATH_CONSTRUCTOR;
  private ForwardSolver forwardSolver;

	SubQueryContext(Query q, BoomerangContext c, SubQueryContext parent) {
		this.query = q;
		this.context = c;

		FW_PATHEDGES = new PathEdgeStore(c, Direction.Forward);
		BW_PATHEDGES = new PathEdgeStore(c, Direction.Backward);
		BW_PATH_CONSTRUCTOR = new BackwardsPathConstructor<>(BW_PATHEDGES);
	}

	public PathEdgeStore getFwEdges() {
		return FW_PATHEDGES;
	}

	public PathEdgeStore getBwEdges() {
		return BW_PATHEDGES;
	}
	public BackwardsPathConstructor<Unit, AccessGraph, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> getPathConstructor() {
		return BW_PATH_CONSTRUCTOR;
	}

	/**
	 * The statement at which the query has been triggered.
	 * 
	 * @return the statement of the query
	 */
	public Unit getStmt() {
		return query.getStmt();
	}

	/**
	 * The {@link AccessGraph} for which the query was started
	 * 
	 * @return the initial access graph of the query.
	 */
	public AccessGraph getAccessPath() {
		return query.getAp();
	}

	/**
	 * Returns the method in which the query was originally started.
	 * 
	 * @return the method of the statement at which the query was triggered.
	 */
	public SootMethod getMethod() {
		return query.getMethod();
	}

	/**
	 * Each SubQuery has its own backward incoming set, such that the forward
	 * analysis only returns along the paths the backward analysis previously
	 * took. Therefore, an incoming edge, {@link IPathEdge}, is added when the
	 * backward analysis enters via a call flow a method.
	 * 
	 * @param callee
	 *            The method being entered
	 * @param factAtCalleeStart
	 *            The fact at the startpoint of the method.
	 * @param callsiteEdge
	 *            The edge at the callsite of the callee (the callsite is stored
	 *            within the edge)
	 */
	public void addBackwardIncoming(SootMethod callee, Pair<Unit, AccessGraph> factAtCalleeStart,
			IPathEdge<Unit, AccessGraph> callsiteEdge) {
		incomings.addIncoming(callee, factAtCalleeStart, callsiteEdge);
	}

	/**
	 * Returns the set of incoming edges {@link IPathEdge} which hold for the
	 * callee method with the appropriate start pair
	 * 
	 * @param startStmtAndFact
	 *            The statement and the fact which enter the call.
	 * @param callee
	 *            The method being entered
	 * @return
	 */
	public Collection<IPathEdge<Unit, AccessGraph>> backwardIncoming(Pair<Unit, AccessGraph> startStmtAndFact,
			SootMethod callee) {
		return incomings.incoming(startStmtAndFact, callee);
	}

	void cleanup() {
		query = null;
		incomings.clear();
		incomings = null;
	}


	/**
	 * Adds a {@link PointOfIndirection} to the worklist. Beforehand a check is
	 * performed, whether this specific POI has already been (globally)
	 * processed before. If so it will not be processed again.
	 * 
	 * @param poi
	 *            The {@link PointOfIndirection} to be added.
	 * @return Returns <code>true</code> if the POI has been added, otherwise
	 *         <code>false</code>.
	 */
	public boolean add(PointOfIndirection poi) {
		return worklist.add(poi);
	}

	public String toString() {
		return query.toString();
	}

	public Type getType() {
		return query.getType();
	}

	public boolean isEmpty() {
		return worklist.isEmpty();
	}

	PointOfIndirection removeFirst() {
		PointOfIndirection out = null;

		for (PointOfIndirection p : worklist) {
			if (p instanceof Alloc) {
				out = p;
				break;
			}
		}
		if (out == null) {
			for (PointOfIndirection p : worklist) {
				if (p instanceof Call) {
					out = p;
					break;
				}
			}
		}
		if (out == null) {
			for (PointOfIndirection p : worklist) {
				if (p instanceof BackwardParameterTurnHandler) {
					out = p;
					break;
				}
			}
		}

		if (out == null) {
			for (PointOfIndirection p : worklist) {
				if (p instanceof Meeting) {
					out = p;
					break;
				}
			}
		}

		if (out == null) {
			for (PointOfIndirection p : worklist) {
				if (p instanceof Return) {
					out = p;
					break;
				}
			}
		}
		if (out == null) {
			for (PointOfIndirection p : worklist) {
				if (p instanceof Read) {
					out = p;
					break;
				}
			}
		}

		if (out == null) {
			for (PointOfIndirection p : worklist) {
				if (p instanceof Unbalanced) {
					out = p;
					break;
				}
			}
		}
		worklist.remove(out);
		return out;
	}

	/**
	 * Forward POI ({@link ForwardPointOfIndirection}) are special and treated
	 * specially, as they are directly processed and NOT put to a worklist. But
	 * still they are added to the global set of processed POI, as they also
	 * should never be computed twice.
	 * 
	 * @param poi
	 *            The {@link ForwardPointOfIndirection}
	 * @return <code>true</code> or <code>false</code> depending whether the POI
	 *         has been added.
	 */
	public boolean addToDirectlyProcessed(ForwardPointOfIndirection poi) {
		return directProcessedPOI.add(poi);
	}

  public ForwardSolver getCurrentForwardSolver() {
    return forwardSolver;
  }

  public void setCurrentForwardSolver(ForwardSolver solver) {
    this.forwardSolver = solver;
  }
}
