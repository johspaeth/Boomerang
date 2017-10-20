package boomerang;

import java.util.concurrent.TimeUnit;

import boomerang.accessgraph.AccessGraph;
import boomerang.allocationsitehandler.ReferenceType;
import boomerang.debug.DefaultBoomerangDebugger;
import boomerang.debug.IBoomerangDebugger;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IPropagationController;
import boomerang.pointsofindirection.AllocationSiteHandlers;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public abstract class BoomerangOptions {

	public IBoomerangDebugger getDebugger() {
		return new DefaultBoomerangDebugger();
	}

	public long getTimeBudget() {
		return TimeUnit.SECONDS.toMillis(100);
	}

	public boolean getTrackStaticFields() {
		return true;
	}

	public boolean handleArrays() {
		return true;
	}
	public boolean getTrackStatementsInFields() {
		return false;
	}

	public boolean stronglyUpdateFields() {
		return true;
	}

	public abstract JimpleBasedInterproceduralCFG icfg();

	public String toString() {
		String str = "====== Boomerang Options ======";
		str += "\nDebugger:\t\t" + getDebugger();
		str += "\nAnalysisBudget(ms):\t" + getTimeBudget();
		str += "\nAllocationSiteHandler:\t" + allocationSiteHandlers();
		str += "\n====================";
		return str;
	}

	public ContextScheduler getScheduler() {
		return new ContextScheduler();
	}

	public IPropagationController<Unit, AccessGraph> propagationController() {
		return new IPropagationController<Unit, AccessGraph>() {
			@Override
			public boolean continuePropagate(IPathEdge<Unit, AccessGraph> edge) {
				return true;
			}
		};
	}

	public AllocationSiteHandlers allocationSiteHandlers() {
		return new ReferenceType();
	}

	public boolean typeCheckForCasts() {
		return true;
	}
	
	public boolean onTheFlyCallGraphGeneration(){
		return true;
	}
}
