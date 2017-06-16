/**
 * (c) 2013 Tata Consultancy Services & Ecole Polytechnique de Montreal All rights reserved
 */
package boomerang.cfg;

import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public interface IExtendedICFG extends BiDiInterproceduralCFG<Unit, SootMethod> {

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
