package boomerang.context;

import boomerang.incremental.UpdatableWrapper;
import soot.SootMethod;
import soot.Unit;

public interface IContextRequester {
	public boolean continueAtCallSite(UpdatableWrapper<Unit> callSite, UpdatableWrapper<SootMethod> callee);
	public boolean isEntryPointMethod(UpdatableWrapper<SootMethod> method);
}
