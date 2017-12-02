package boomerang.forward;

import boomerang.BoomerangContext;
import boomerang.BoomerangTimeoutException;
import boomerang.accessgraph.AccessGraph;
import boomerang.bidi.Incomings;
import boomerang.bidi.PathEdgeStore;
import boomerang.bidi.Summaries;
import boomerang.ifdssolver.DefaultIFDSTabulationProblem.Direction;
import boomerang.ifdssolver.IFDSSolver;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.PathEdge;
import heros.BiDiInterproceduralCFG;
import soot.SootMethod;
import soot.Unit;

public class ForwardSolver extends
    IFDSSolver<Unit, AccessGraph, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> {


  private BoomerangContext context;



  public ForwardSolver(ForwardProblem tabulationProblem, BoomerangContext context) {
    super(tabulationProblem, context.debugger);
    this.context = context;
    this.pathEdges = new PathEdgeStore(context,Direction.FORWARD);
    this.summaries = new Summaries(context);
    this.incomings = new Incomings();
  }


  /**
   * Starts the propagation at the successor of the provided statement. Creates the path edge
   * <stmt,d1>-><succ(stmt),d2> and adds it to the solvers worklist. The backward path edge is used
   * to track the path along which the forward propagation is allowed.
   * 
   * @param stmt The stmt where the path edge will begin (for each succ(stmt) there will be a path
   *        edge created).
   * @param d1 The start fact of the propagated path edge.
   * @param d2 The target fact of the propagated path edge.
   * @param bwedge Provides the path along which the analysis is allowed to propagate
   */
  public void startPropagationAlongPath(final Unit stmt, final AccessGraph d1, final AccessGraph d2,
      final IPathEdge<Unit, AccessGraph> bwedge) {
    for (Unit succStmt : icfg.getSuccsOf(stmt)) {
      PathEdge<Unit, AccessGraph> pathEdge = new PathEdge<Unit, AccessGraph>(stmt, d1, succStmt, d2);
      propagate(pathEdge, PropagationType.Normal);
    }

    awaitExecution();
  }

  @Override
  public void onRegister(IPathEdge<Unit, AccessGraph> edge) {
    context.sanityCheckEdge(edge);
  }

  public String toString() {
    return "FW Solver";
  }

  @Override
  public void cleanup() {
    super.cleanup();
  }


}
