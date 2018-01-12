package boomerang.debug;

import boomerang.accessgraph.AccessGraph;
import boomerang.ap.AliasResults;
import boomerang.ap.Query;
import soot.Unit;

public class BoomerangDebugger extends DefaultBoomerangDebugger{

  @Override
  public void finishedQuery(Query q, AliasResults res) {
    log("Finished query " + res);
  }

  @Override
  public void startQuery(Query q) {
    log("Start query " + q);
  }

  private void log(Object s) {
    System.out.println(s.toString());
  }

  @Override
  public void onAllocationSiteReached(Unit as, AccessGraph g) {
    log("Allocation site reached " + as);
  }

}
