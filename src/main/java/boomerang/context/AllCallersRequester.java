package boomerang.context;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;

/**
 * In contrast to the {@link NoContextRequestor}, upon reaching the method start point of the
 * triggered query this requester yields the analysis to continue at ALL callsites.
 * 
 * @author Johannes Spaeth
 *
 */
public class AllCallersRequester implements
    IContextRequester {

	@Override
	public boolean continueAtCallSite(Unit callSite, SootMethod callee) {
		return true;
	}

	@Override
	public boolean isEntryPointMethod(SootMethod method) {
		return Scene.v().getEntryPoints().contains(method);
	}
}
