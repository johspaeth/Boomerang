package boomerang.forward;

import java.util.Collection;

import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.WrappedSootField;
import soot.SootMethod;

public abstract class AbstractFlowFunctions {
	protected BoomerangContext context;
	protected boolean isFirstFieldUsedTransitivelyInMethod(AccessGraph source, final SootMethod callee) {
        for(WrappedSootField wrappedField :  source.getFirstField()){
      	  if(context.isStaticFieldUsed(callee, wrappedField.getField()))
      		  return true;
        }
		return false;
	}
	protected boolean isFirstFieldUsedTransitivelyInMethod(AccessGraph source,  Collection<SootMethod> callees) {
        for(SootMethod callee:  callees){
      	  if(isFirstFieldUsedTransitivelyInMethod(source,callee))
      		  return true;
        }
		return false;
	}
}
