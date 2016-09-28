package boomerang;

import java.util.concurrent.TimeUnit;

import boomerang.debug.IBoomerangDebugger;
import boomerang.debug.NullBoomerangDebugger;

public class BoomerangOptions {
  private boolean trackType = true;
  private boolean trackStaticField = true;
  private boolean trackStatementsInFields = false;
  private long timeBudget = TimeUnit.SECONDS.toMillis(100);
  private IBoomerangDebugger debugger = new NullBoomerangDebugger();

  /**
   * Enables/Disables tracking of types in the access graph. This also includes the types within
   * the fields of the access graph.
   * @param enabled
   */
  public void setTrackType(boolean enabled) {
    this.trackType = enabled;
  }

  /**
   * Set the time budget in milliseconds each query is allowed to consume.
   * @param budget
   */
  public void setQueryBudget(long budget) {
    this.timeBudget = budget;
  }

  /**
   * Enable/Disable tracking of static fields. Public static fields are accessible from anywhere in the program
   * and can be changed at any statement, therefore they must flow to each method. May become a source of inefficieny.  
   * @param enabled
   */
  public void setTrackStaticFields(boolean enabled){
    this.trackStaticField = enabled;
  }
  
  /**
   * Enable/Disable tracking of statements in the fields of an access graph. If disable, to subsequent writes to the same field will
   * end up in a loop within the graph. E.g. b.next = a and c.next = b generates a graph (next->next)* instead of next->next only. 
   * @param enabled
   */
  public void setTrackStatementsInFields(boolean enabled){
	  this.trackStatementsInFields = enabled;
  }

  /**
   * Let's you specify a custom debugger for Boomerang. Can also be used for Logging.
   * @param debugger
   */
  public void setDebugger(IBoomerangDebugger debugger) {
    this.debugger = debugger;
  }

  public IBoomerangDebugger getDebugger() {
    return debugger;
  }

  public long getTimeBudget() {
    return timeBudget;
  }

  public boolean getTrackType() {
    return trackType;
  }

  public boolean getTrackStaticFields() {
    return trackStaticField;
  }
  
  public boolean getTrackStatementsInFields() {
	return trackStatementsInFields;
  }
}
