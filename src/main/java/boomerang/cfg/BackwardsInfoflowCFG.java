package boomerang.cfg;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;

/**
 * Inverse interprocedural control-flow graph for the infoflow solver
 * 
 * @author Steven Arzt
 * @author Eric Bodden
 */
public class BackwardsInfoflowCFG extends ExtendedICFG {
	
	private final IExtendedICFG baseCFG;
	
	public BackwardsInfoflowCFG(IExtendedICFG baseCFG) {
		super(new BackwardsInterproceduralCFG(baseCFG));
		this.baseCFG = baseCFG;
	}
	
	public IExtendedICFG getBaseCFG() {
		return this.baseCFG;
	}
	
}
