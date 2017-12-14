package boomerang.cfg;

import heros.BiDiInterproceduralCFG;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

/**
 * Inverse interprocedural control-flow graph for the infoflow solver
 * 
 * @author Steven Arzt
 * @author Eric Bodden
 */
public class BackwardsInfoflowCFG extends BackwardsInterproceduralCFG {
	
	private final BiDiInterproceduralCFG<Unit, SootMethod> baseCFG;
	
	public BackwardsInfoflowCFG(BiDiInterproceduralCFG<Unit, SootMethod> baseCFG) {
		super(baseCFG);
		this.baseCFG = baseCFG;
	}
	
	public BiDiInterproceduralCFG<Unit, SootMethod> getBaseCFG() {
		return this.baseCFG;
	}
	
}
