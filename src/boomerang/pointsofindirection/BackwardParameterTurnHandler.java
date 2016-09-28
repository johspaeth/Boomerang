package boomerang.pointsofindirection;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.forward.ForwardSolver;
import boomerang.ifdssolver.IPathEdge;
import soot.Unit;

public class BackwardParameterTurnHandler implements BackwardForwardHandler {
  private IPathEdge<Unit, AccessGraph> pathedge;
  private AccessGraph factAtTarget;
  private Unit target;

  public BackwardParameterTurnHandler(IPathEdge<Unit, AccessGraph> pathedge) {
    this.pathedge = pathedge;
    target = pathedge.getTarget();
    factAtTarget = pathedge.factAtTarget().clone();
  }

  @Override
  public void execute(ForwardSolver ptsSolver, BoomerangContext context) {
	  context.debugger.onProcessingParamPOI(this);
      ptsSolver.startPropagationAlongPath(target, factAtTarget, factAtTarget, pathedge);
  }

  @Override
  public String toString() {
    return "ParameterAPOI [pathedge=" + pathedge + "]";
  }

  @Override
  public Unit getStmt() {
    return pathedge.getTarget();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((factAtTarget == null) ? 0 : factAtTarget.hashCode());
    result = prime * result + ((target == null) ? 0 : target.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    BackwardParameterTurnHandler other = (BackwardParameterTurnHandler) obj;
    if (factAtTarget == null) {
      if (other.factAtTarget != null)
        return false;
    } else if (!factAtTarget.equals(other.factAtTarget))
      return false;
    if (target == null) {
      if (other.target != null)
        return false;
    } else if (!target.equals(other.target))
      return false;
    return true;
  }
}
