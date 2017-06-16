package boomerang.pointsofindirection;

import java.util.Arrays;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.WrappedSootField;
import boomerang.ifdssolver.IFDSSolver.PropagationType;
import boomerang.ifdssolver.PathEdge;
import heros.solver.Pair;
import soot.Unit;

public class ForwardAliasCallback extends AliasCallback{
	private Unit sourceStmt;
	private AccessGraph sourceFact;
	private Unit targetStmt;
	private WrappedSootField[] toAppend;
	public ForwardAliasCallback(Unit sourceStmt, AccessGraph sourceFact, Unit targetStmt, WrappedSootField[] toAppend, BoomerangContext context) {
		super(context);
		this.sourceStmt = sourceStmt;
		this.sourceFact = sourceFact;
		this.targetStmt = targetStmt;
		this.toAppend = toAppend;
	}
	public void newAliasEncountered(PointOfIndirection poi,AccessGraph alias, Pair<Unit,AccessGraph> origin){
		if(!executed.add(alias))
			return;
		if(alias.hasSetBasedFieldGraph())
			return;
		PathEdge<Unit, AccessGraph> edge = new PathEdge<Unit,AccessGraph>(sourceStmt,sourceFact,targetStmt,alias.appendFields(toAppend));
		context.getForwardSolver().inject(edge, PropagationType.Normal);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceFact == null) ? 0 : sourceFact.hashCode());
		result = prime * result + ((sourceStmt == null) ? 0 : sourceStmt.hashCode());
		result = prime * result + ((targetStmt == null) ? 0 : targetStmt.hashCode());
		result = prime * result + Arrays.hashCode(toAppend);
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
		ForwardAliasCallback other = (ForwardAliasCallback) obj;
		if (sourceFact == null) {
			if (other.sourceFact != null)
				return false;
		} else if (!sourceFact.equals(other.sourceFact))
			return false;
		if (sourceStmt == null) {
			if (other.sourceStmt != null)
				return false;
		} else if (!sourceStmt.equals(other.sourceStmt))
			return false;
		if (targetStmt == null) {
			if (other.targetStmt != null)
				return false;
		} else if (!targetStmt.equals(other.targetStmt))
			return false;
		if (!Arrays.equals(toAppend, other.toAppend))
			return false;
		return true;
	}
	
}
