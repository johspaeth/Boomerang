package boomerang;

import java.util.concurrent.TimeUnit;

import boomerang.accessgraph.AccessGraph;
import boomerang.debug.IBoomerangDebugger;
import boomerang.debug.NullBoomerangDebugger;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IPropagationController;
import soot.Unit;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public abstract class BoomerangOptions {

	public IBoomerangDebugger getDebugger() {
		return new NullBoomerangDebugger();
	}

	public long getTimeBudget() {
		return TimeUnit.SECONDS.toMillis(100);
	}

	public boolean getTrackStaticFields() {
		return true;
	}

	public boolean getTrackStatementsInFields() {
		return false;
	}

	public boolean stronglyUpdateFields(){
		return true;
	}
	public abstract IInfoflowCFG icfg();

	public String toString() {
		String str = "====== Boomerang Options ======";
		str += "\nDebugger:\t\t" + getDebugger();
		str += "\nAnalysisBudget(ms):\t" + getTimeBudget();
		str += "\n====================";
		return str;
	}

	public ContextScheduler getScheduler() {
		return new ContextScheduler();
	}
	
	public IPropagationController<Unit, AccessGraph> propagationController(){
		return new IPropagationController<Unit, AccessGraph>() {
			@Override
			public boolean continuePropagate(IPathEdge<Unit, AccessGraph> edge) {
				return true;
			}
		};
	}
}
