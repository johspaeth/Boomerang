package boomerang.cache;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.AliasFinder;
import boomerang.BoomerangContext;
import boomerang.accessgraph.AccessGraph;
import boomerang.accessgraph.FieldGraph;
import boomerang.accessgraph.WrappedSootField;
import heros.solver.Pair;
import soot.Scene;
import soot.SootField;
import soot.Type;
import soot.Unit;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public class AliasResults extends ForwardingMultimap<Pair<Unit, AccessGraph>, AccessGraph> {
  private Multimap<Pair<Unit, AccessGraph>, AccessGraph> delegate;

  /**
   * Creates a deep copy of with the supplied Multimap of results.
   * 
   * @param res
   */
  public AliasResults(Multimap<Pair<Unit, AccessGraph>, AccessGraph> res) {
    this.delegate = HashMultimap.create();
    for (Pair<Unit, AccessGraph> key : res.keySet()) {
      Pair<Unit, AccessGraph> clonedKey = new Pair<>(key.getO1(), key.getO2().clone());
      Set<AccessGraph> out = new HashSet<>();
      for (AccessGraph value : res.get(key)) {
        out.add(value.clone());
      }
      delegate.putAll(clonedKey, out);
    }
  }

  /**
   * Creates an empty result map.
   */
  public AliasResults() {
    this.delegate = HashMultimap.create();
  }


  @Override
  protected Multimap<Pair<Unit, AccessGraph>, AccessGraph> delegate() {
    return this.delegate;
  }

  /**
   * Appends the fields to the each of the access graph in the collection supplied.
   * 
   * @param in Set of access graph to append the fields to.
   * @param fields Fields to be appended.
   * @param context The general context (needed for type checking.)
   * @return
   */
  public static Set<AccessGraph> appendFields(Collection<AccessGraph> in, WrappedSootField[] fields,
      BoomerangContext context) {
    Set<AccessGraph> out = new HashSet<>();
    for (AccessGraph v : in) {
      if (!canAppend(v, fields[0])) {
        continue;
      };
      AccessGraph newValue = v.appendFields(fields);

      if (context.isValidAccessPath(newValue)) {
        out.add(newValue);
      }
    }
    return out;
  }

  /**
   * Appends a single field to the each of the access graph in the collection supplied.
   * 
   * @param in Set of access graph to append the fields to.
   * @param lastField Fields to be appended.
   * @param context The general context (needed for type checking.)
   * @return
   */
  public static Set<AccessGraph> appendField(Collection<AccessGraph> results,
      WrappedSootField lastField, BoomerangContext context) {
    return appendFields(results, new WrappedSootField[] {lastField}, context);
  }

  /**
   * Returns the may alias set of access graph, that is just all values of the map.
   * 
   * @return The set of aliasing access graphs
   */
  public Set<AccessGraph> mayAliasSet() {
    return new HashSet<>(this.values());
  }

  /**
   * Appends another field graph the each of the access graph in the collection supplied.
   * 
   * @param in Set of access graph to append the fields to.
   * @param graph {@link FieldGraph} to be appended.
   * @param context The general context (needed for type checking.)
   * @return
   */
  public static Set<AccessGraph> appendFields(Set<AccessGraph> in, AccessGraph graph,
      BoomerangContext context) {
    if (graph == null || graph.getFieldGraph() == null)
      return new HashSet<>(in);
    Set<AccessGraph> out = new HashSet<>();
    for (AccessGraph v : in) {
      if (!canAppend(v, graph.getFirstField())) {
        continue;
      };
      AccessGraph newValue = v.appendGraph(graph.getFieldGraph());

      if (context.isValidAccessPath(newValue)) {
        out.add(newValue);
      }
    }
    return out;
  }

  /**
   * Checks if a field can be appended to a single access graph.
   * 
   * @param accessgraph The access graph
   * @param firstField The field to be appended
   * @return <code>true</code> if the field can be appended.
   */
  public static boolean canAppend(AccessGraph accessgraph, WrappedSootField firstField) {
    if (firstField.getField().equals(AliasFinder.ARRAY_FIELD))
      return true;
    SootField field = firstField.getField();
    Type child = field.getDeclaringClass().getType();
    Type parent = null;
    if (accessgraph.getFieldCount() < 1) {
      parent = accessgraph.getBaseType();
    } else {
      SootField lastField = accessgraph.getLastField().getField();
      if (lastField.equals(AliasFinder.ARRAY_FIELD))
        return true;
      parent = lastField.getType();
    }
    return Scene.v().getFastHierarchy().canStoreType(child, parent)
        || Scene.v().getFastHierarchy().canStoreType(parent, child);
  }

  /**
   * Checks if the field can be prepended to a single access graph.
   * 
   * @param accessgraph The access graph
   * @param firstField The field to be appended
   * @return <code>true</code> if the field can be appended.
   */
  public static boolean canPrepend(AccessGraph v, WrappedSootField newFirstField) {
    SootField newFirst = newFirstField.getField();
    if (newFirst.equals(AliasFinder.ARRAY_FIELD))
      return true;

    if (v.getFieldCount() < 1) {
    } else {
      SootField oldFirstField = v.getFirstField().getField();
      if (oldFirstField.equals(AliasFinder.ARRAY_FIELD))
        return true;
    }
    Type child = newFirst.getDeclaringClass().getType();
    Type parent = v.getBaseType();
    boolean res =
        Scene.v().getFastHierarchy().canStoreType(child, parent)
            || Scene.v().getFastHierarchy().canStoreType(parent, child);
    return res;
  }

  public String withMethodOfAllocationSite(IInfoflowCFG cfg) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for (Pair<Unit, AccessGraph> k : keySet()) {
      sb.append(k.getO1() + " in " + cfg.getMethodOf(k.getO1()));
      sb.append("=");
      sb.append(get(k));
    }
    sb.append("}");
    return "AliasResults: " + sb.toString();
  }
}
