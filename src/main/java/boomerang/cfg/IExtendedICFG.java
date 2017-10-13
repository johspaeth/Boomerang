/**
 * (c) 2013 Tata Consultancy Services & Ecole Polytechnique de Montreal All rights reserved
 */
package boomerang.cfg;

import boomerang.incremental.UpdatableInterproceduralCFG;
import soot.SootField;
import soot.SootMethod;

public interface IExtendedICFG<N, M> extends UpdatableInterproceduralCFG<N, M> {

	/**
	 * Checks whether the given static field is used (read or written) inside
	 * the given method or one of its transitive callees.
	 * 
	 * @param method
	 *            The method to check
	 * @param variable
	 *            The static field to check
	 * @return True if the given static field is used inside the given method,
	 *         otherwise false
	 */
	public boolean isStaticFieldUsed(SootMethod method, SootField variable);

	public boolean isIgnoredMethod(SootMethod callee);

}
