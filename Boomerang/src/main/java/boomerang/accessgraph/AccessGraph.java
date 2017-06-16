package boomerang.accessgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import boomerang.AliasFinder;
import heros.solver.Pair;
import soot.ArrayType;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootField;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;

/**
 * An AccessGraph is represented by a local variable and a {@link FieldGraph}
 * representing multiple field accesses.
 * 
 * @author spaeth
 *
 */
public class AccessGraph {

	/**
	 * The local variable at which the field graph is rooted.
	 */
	private final Local value;


	/**
	 * The {@link FieldGraph} representing the accesses which yield to the
	 * allocation site.
	 */
	private final IFieldGraph fieldGraph;

	private int hashCode = 0;

	/**
	 * The allocation site to which this access graph points-to.
	 */
	private Unit allocationSite;

	private boolean isNullAllocsite;

	/**
	 * Constructs an access graph with empty field graph, but specified base
	 * (local) variable.
	 * 
	 * @param val
	 *            The local to be the base of the access graph.
	 * @param t
	 *            The type of the base
	 */
	public AccessGraph(Local val) {
		this(val,  null, null, false);
	}

	public AccessGraph(Local val, Unit allocsite,boolean isNullAllocsite) {
		this(val, null, allocsite, isNullAllocsite);
	}

	/**
	 * Constructs an access graph with base variable and field graph consisting
	 * of exactly one field (write) access.
	 * 
	 * @param val
	 *            the local base variable
	 * @param t
	 *            the type of the local variable
	 * @param field
	 *            the first field access
	 */
	public AccessGraph(Local val,  WrappedSootField field) {
		this(val, new FieldGraph(field), null,false);
	}

	/**
	 * Constructs an access graph with base variable and field graph consisting
	 * of the sequence of supplied field (write) accesses.
	 * 
	 * @param val
	 *            the local base variable
	 * @param t
	 *            the type of the local variable
	 * @param field
	 *            An array of field accesses
	 */
	public AccessGraph(Local val, WrappedSootField[] f) {
		this(val, (f == null || f.length == 0 ? null : new FieldGraph(f)), null, false);
	}

	protected AccessGraph(Local value, IFieldGraph fieldGraph, Unit sourceStmt, boolean isNullAllocsite) {
		this.value = value;
		this.isNullAllocsite = isNullAllocsite;
//		if(apgs == null){
//			apgs = new LinkedList<IFieldGraph>();
//		}
		if(fieldGraph != null && fieldGraph.equals(FieldGraph.EMPTY_GRAPH))
			fieldGraph = null;
//		int index = apgs.indexOf(fieldGraph);
//		if(index >= 0){
//			this.fieldGraph = apgs.get(index);
//		} else{
//			apgs.add(fieldGraph);
//			System.out.println("APG" + apgs.size());
//			System.out.println(fieldGraph);
			this.fieldGraph = fieldGraph;
//		}
		this.allocationSite = sourceStmt;
	}

	/**
	 * Get the base/local variable of the access graph.
	 * 
	 * @return The local variable at which the graph is rooted.
	 */
	public Local getBase() {
		return value;
	}

	/**
	 * If the access graph is not null, the first field access is returned.
	 * 
	 * @return The first field of the access graph (might return
	 *         <code>null</code>)
	 */
	public Collection<WrappedSootField> getFirstField() {
		if (fieldGraph == null)
			return Collections.emptySet();
		return fieldGraph.getEntryNode();
	}

	/**
	 * Checks if the first field of the access graph matches the given field.
	 * 
	 * @param field
	 *            The field to check against
	 * @return {@link Boolean} whether the field matches or not.
	 */
	public boolean firstFieldMustMatch(SootField field) {
		if (fieldGraph == null)
			return false;
		if(fieldGraph instanceof SetBasedFieldGraph)
			return false;
		for(WrappedSootField f: getFirstField())
			return f.getField().equals(field);
		throw new RuntimeException("Unreachable Code");
	}
	
	public boolean firstFirstFieldMayMatch(SootField field) {
		for(WrappedSootField f: getFirstField())
			if(f.getField().equals(field))
				return true;
		return false;
	}
	/**
	 * Returns the number of field accesses (in cases where the field graph has
	 * loops, the shortest version is picked.)
	 * 
	 * @return The length of the shortest sequence of field accesses.
	 */
	public int getFieldCount() {
		return (fieldGraph == null ? 0 : (getRepresentative() == null ? 0 : getRepresentative().length));
	}

	/**
	 * One representative of the accesses described by this access graph. In
	 * cases of loops, it will only pick the shortest sequence.
	 * 
	 * @return An array of SootField paired with the statement from which they
	 *         originate (the field write statements)
	 */
	public WrappedSootField[] getRepresentative() {
		if (fieldGraph == null)
			return null;

		return fieldGraph.getFields();
	}

	@Override
	public String toString() {
		String str = "";
		if (value != null){
			str += value.toString() ;
		}
		if (fieldGraph != null) {
			
			 str += fieldGraph.toString();
		}
		if (allocationSite != null) {
			str += " at " +allocationSite.toString();
		}
		return str;
	}

	/**
	 * Keeps the accesses as they are, and changes the local variable with the
	 * given type.
	 * 
	 * @param local
	 *            The local variable of the returned access graph
	 * @param type
	 *            The new type to be used.
	 * @return The access graph
	 */
	public AccessGraph deriveWithNewLocal(Local local) {
		return new AccessGraph(local, fieldGraph, allocationSite,isNullAllocsite);
	}

	/**
	 * Appends a sequence of SootFields (wrapped inside {@link WrappedSootField}
	 * ) to the current access graph.
	 * 
	 * @param toAppend
	 *            Sequence of fields to append.
	 * @return the access graph derived with the appended fields.
	 */
	public AccessGraph appendFields(WrappedSootField[] toAppend) {
		IFieldGraph newapg = (fieldGraph != null ? fieldGraph.appendFields(toAppend) : new FieldGraph(toAppend));
		if(newapg.shouldOverApproximate()){
			newapg = newapg.overapproximation();
		}
		return new AccessGraph(value,  newapg, allocationSite,isNullAllocsite);
	}

	/**
	 * Appends a complete field graph to the current access graph.
	 * 
	 * @param toAppend
	 *            The field graph to append
	 * @return the access graph derived with the appended fields.
	 */
	public AccessGraph appendGraph(IFieldGraph graph) {
		if (graph == null)
			return this;
		IFieldGraph newapg = (fieldGraph != null ? fieldGraph.append(graph) : graph);
		if(newapg.shouldOverApproximate()){
			newapg = newapg.overapproximation();
		}
		return new AccessGraph(value,  newapg, allocationSite,isNullAllocsite);
	}
	
	/**
	 * Add the provided field to the beginning of the field graph. This is
	 * typically called at field write statements.
	 * 
	 * @param f
	 *            The field to prepend
	 * @return A copy of the current access graph with the field appended
	 */
	public AccessGraph prependField(WrappedSootField f) {
		IFieldGraph newapg = (fieldGraph != null ? fieldGraph.prependField(f) : new FieldGraph(f));
		if(newapg.shouldOverApproximate()){
			newapg = newapg.overapproximation();
		}
		return new AccessGraph(value, newapg, allocationSite,isNullAllocsite);
	}

	/**
	 * Checks if the base of this access graph matches the local given as
	 * argument.
	 * 
	 * @param local
	 *            The value to check against.
	 * @return {@link Boolean} depending if the base matches the argument.
	 */
	public boolean baseMatches(Value local) {
		assert local != null;
		return value == local;
	}

	/**
	 * Checks if the base variable and the first field matches the given
	 * argument.
	 * 
	 * @param local
	 *            The base variable to check against.
	 * @param field
	 *            The first field to check.
	 * @return {@link Boolean} depending if it matches.
	 */
	public boolean baseAndFirstFieldMatches(Value local, SootField field) {
		if (!baseMatches(local)) {
			return false;
		}
		return firstFieldMustMatch(field);
	}

	/**
	 * Removes the first field from this access graph. (Typically invoked at
	 * field read statements.) As the the first field access might have multiple
	 * successors, the output of this method is a set, and not a single access
	 * graph.
	 * 
	 * @return A set of access graph which are derived by the removal of the
	 *         first field of the current graph.
	 */
	public Set<AccessGraph> popFirstField() {
		if (fieldGraph == null)
			throw new RuntimeException("Try to remove the first field from an access graph which has no field" + this);

		Set<IFieldGraph> newapg = fieldGraph.popFirstField();
		if (newapg.isEmpty())
			return Collections.singleton(new AccessGraph(value, null, allocationSite,isNullAllocsite));
		Set<AccessGraph> out = new HashSet<>();
		for (IFieldGraph a : newapg) {
				out.add(new AccessGraph(value, a, allocationSite,isNullAllocsite));
		}
		return out;
	}

	/**
	 * Similar to {@link #popFirstField()} but instead removes the last field.
	 * As the last field might have multiple predecessors, a set of access graph
	 * is returned.
	 * 
	 * @return Set of graphs without the last access.
	 */
	public Set<AccessGraph> popLastField() {
		if (fieldGraph == null)
			throw new RuntimeException("Try to remove the first field from an access graph which has no field" + this);

		Set<IFieldGraph> newapg = fieldGraph.popLastField();

		Set<AccessGraph> out = new HashSet<>();
		if (newapg.isEmpty())
			return Collections.singleton(new AccessGraph(value, null, allocationSite,isNullAllocsite));
		for (IFieldGraph a : newapg) {
			out.add(new AccessGraph(value,  a, allocationSite,isNullAllocsite));
		}
		return out;
	}

	/**
	 * Returns the allocation site of this access graph. Might be null.
	 * 
	 * @return The allocation site, this access graph points to. Can be null, if
	 *         the base variable is a parameter of the current method.
	 */
	public Unit getSourceStmt() {
		return allocationSite;
	}

	/**
	 * Derives an access graph with the given statement as allocation site to
	 * that access graph.
	 * 
	 * @param stmt
	 *            The statement, typically the allocation site.
	 * @return The derived access graph
	 */
	public AccessGraph deriveWithAllocationSite(Unit stmt, boolean isNullAllocsite) {
		return new AccessGraph(value, fieldGraph, stmt, isNullAllocsite);
	}

	/**
	 * Check whether this access graph points-to an allocation site.
	 * 
	 * @return <code>true</code> if it has an allocation site associated.
	 */
	public boolean hasAllocationSite() {
		return allocationSite != null;
	}

	/**
	 * Sets the allocation to null. This is called whenever a flow enters a call
	 * with an allocation site. Now the allocation site is removed, as it does
	 * not hold within the method. In that way we receive more reusable
	 * summaries.
	 * 
	 * @return The derived access graph
	 */
	public AccessGraph deriveWithoutAllocationSite() {
		return new AccessGraph(value, fieldGraph, null, false);
	}

	/**
	 * Removes the complete field graph from the access graph.
	 * 
	 * @return The derived access graph
	 */
	public AccessGraph dropTail() {
		return new AccessGraph(value, null, allocationSite,isNullAllocsite);
	}

	/**
	 * Derives a static access graph. A static access graph has
	 * <code>null</code> as local variable. The first field automatically
	 * determines the base class of the static field of the access graph.
	 * 
	 * @return The derived access graph.
	 */
	public AccessGraph makeStatic() {
		return new AccessGraph(null, fieldGraph, allocationSite,isNullAllocsite);
	}

	/**
	 * Checks if this access graph represents a static field.
	 * 
	 * @return <code>true</code> if it is static.
	 */
	public boolean isStatic() {
		return value == null && (getFieldCount() > 0);
	}

	/**
	 * Retrieve the last field access of the graph.
	 * 
	 * @return Last field, might be null.
	 */
	public Collection<WrappedSootField> getLastField() {
		if (fieldGraph == null)
			return null;

		return fieldGraph.getExitNode();
	}

	/**
	 * Retrieve a clone of the field graph of that current access graph.
	 * 
	 * @return The field graph.
	 */
	public IFieldGraph getFieldGraph() {
		return fieldGraph;
	}
	

	@Override
	public int hashCode() {
//		if (hashCode != 0)
//			return hashCode;

		final int prime = 31;
		int result = 1;
		result = prime * result + ((fieldGraph == null) ? 0 : fieldGraph.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((allocationSite == null) ? 0 : allocationSite.hashCode());
		this.hashCode = result;

		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this || super.equals(obj))
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		AccessGraph other = (AccessGraph) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (allocationSite == null) {
			if (other.allocationSite != null)
				return false;
		} else if (!allocationSite.equals(other.allocationSite))
			return false;
		if (fieldGraph == null) {
			if (other.fieldGraph != null)
				return false;
		} else if (!fieldGraph.equals(other.fieldGraph))
			return false;
		assert this.hashCode() == obj.hashCode();
		return true;
	}

	public boolean hasSetBasedFieldGraph() {
		return fieldGraph instanceof SetBasedFieldGraph;
	}

	public AccessGraph overApproximate() {
		return new AccessGraph(value,fieldGraph == null ? null : fieldGraph.overapproximation(), allocationSite,isNullAllocsite);
	}


	public boolean hasNullAllocationSite() {
		return isNullAllocsite;
	}
	

	public Type getAllocationType() {
		if(!hasAllocationSite())
			throw new RuntimeException("Wrong state");
		if(allocationSite instanceof AssignStmt){
			AssignStmt as = (AssignStmt) allocationSite;
			Value rightOp = as.getRightOp();
			if(rightOp instanceof NewExpr){
				NewExpr newExpr = (NewExpr) rightOp;
				return newExpr.getBaseType();
			}
			Value leftOp = as.getLeftOp();
			return leftOp.getType();
		}
		throw new RuntimeException("Allocation site not an Assign Stmt" + allocationSite);
	}
}
