package boomerang.ifdssolver;

import heros.InterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * This is a template for {@link IFDSTabulationProblem}s that automatically caches values that ought
 * to be cached. This class uses the Factory Method design pattern. The {@link InterproceduralCFG}
 * is passed into the constructor so that it can be conveniently reused for solving multiple
 * different {@link IFDSTabulationProblem}s. This class is specific to Soot.
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public abstract class DefaultIFDSTabulationProblem<N, D, M, I extends BiDiInterproceduralCFG<N, M>>
    implements IFDSTabulationProblem<N, D, M, I> {

  private final I icfg;
  private D zeroValue;

  public enum Direction {
    FORWARD, BACKWARD
  }

  public DefaultIFDSTabulationProblem(I icfg) {
    this.icfg = icfg;
  }

  protected abstract D createZeroValue();


  @Override
  public I interproceduralCFG() {
    return icfg;
  }

  @Override
  public final D zeroValue() {
    if (zeroValue == null) {
      zeroValue = createZeroValue();
    }
    return zeroValue;
  }

  @Override
  public boolean followReturnsPastSeeds() {
    return false;
  }

  @Override
  public boolean autoAddZero() {
    return true;
  }

  @Override
  public int numThreads() {
    return Runtime.getRuntime().availableProcessors();
  }

  @Override
  public boolean computeValues() {
    return true;
  }

  public abstract Direction getDirection();

}
