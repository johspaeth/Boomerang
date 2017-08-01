package boomerang.accessgraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import soot.SootField;

/**
 * A field graph represents only the of the access graph field accesses. It is a
 * directed graph. Two nodes of the graph are special, the entry and exit node.
 * One can also see the field graph as a Finite State Machine. The inital state
 * is the entry node and the accepting state is the target node. As the Grph
 * Library represents nodes within the graph as integers, we keep a mapping from
 * fields to integer.
 * 
 * @author spaeth
 *
 */
public class FieldGraph implements IFieldGraph {

	final WrappedSootField[] fields;
	static FieldGraph EMPTY_GRAPH = new FieldGraph() {
		public String toString() {
			return "EMPTY_GRAPH";
		};
	};

	FieldGraph(WrappedSootField[] fields) {
		assert fields != null && fields.length > 0;
		this.fields = fields;
	}

	FieldGraph(WrappedSootField f) {
		assert f != null;
		this.fields = new WrappedSootField[1];
		this.fields[0] = f;
	}


	private FieldGraph() {
		this.fields = new WrappedSootField[0];
	}

	/**
	 * 
	 * @return
	 */
	public Set<IFieldGraph> popFirstField() {
		if (fields.length == 0)
			return new HashSet<>();
		Set<IFieldGraph> out = new HashSet<>();
		if(fields.length == 1)
			out.add(FieldGraph.EMPTY_GRAPH);
		else{
			WrappedSootField[] copy = new WrappedSootField[fields.length - 1];
			System.arraycopy(fields, 1, copy, 0, fields.length - 1);
			out.add(new FieldGraph(copy));
		}
		return out;
	}

	public WrappedSootField[] getFields() {
		return fields;
	}

	public IFieldGraph prependField(WrappedSootField f) {
		WrappedSootField[] copy = new WrappedSootField[fields.length + 1];
		System.arraycopy(fields, 0, copy, 1, fields.length);
		copy[0] = f;
		return new FieldGraph(copy);
	}

	public Set<IFieldGraph> popLastField() {
		Set<IFieldGraph> out = new HashSet<>();
		if (fields.length == 0)
			return out;
		if(fields.length == 1)
			out.add(FieldGraph.EMPTY_GRAPH);
		else{
			WrappedSootField[] copy = new WrappedSootField[fields.length - 1];
			System.arraycopy(fields, 0, copy, 0, fields.length - 1);
			out.add(new FieldGraph(copy));
		}
		return out;
	}

	public IFieldGraph append(IFieldGraph o) {
		if (o instanceof SetBasedFieldGraph) {
			SetBasedFieldGraph setBasedFieldGraph = (SetBasedFieldGraph) o;
			return setBasedFieldGraph.append(this);
		} else if (o instanceof FieldGraph) {
			FieldGraph other = (FieldGraph) o;
			if(other.fields.length == 0)
				return this;
			WrappedSootField[] copy = new WrappedSootField[fields.length + other.fields.length];
			System.arraycopy(fields, 0, copy, 0, fields.length);
			System.arraycopy(other.fields, 0, copy, fields.length, other.fields.length);
			return new FieldGraph(copy);
		}
		throw new RuntimeException("Not yet implemented!");
	}

	public IFieldGraph appendFields(WrappedSootField[] toAppend) {
		return append(new FieldGraph(toAppend));
	}

	public Set<WrappedSootField> getEntryNode() {
		Set<WrappedSootField> out = new HashSet<>();
		if(fields.length > 0)
			out.add(fields[0]);
		return out;
	}

	boolean hasLoops() {
		Set<SootField> sootFields = new HashSet<>();
		for (WrappedSootField f : this.fields) {
			if (sootFields.contains(f.getField()))
				return true;
			sootFields.add(f.getField());
		}
		return false;
	}

	public Collection<WrappedSootField> getExitNode() {
		Set<WrappedSootField> out = new HashSet<>();
		if(fields.length > 0)
			out.add(fields[fields.length - 1]);
		return out;
	}

	public String toString() {
		String str = "";
		str += Arrays.toString(fields);
		return str;
	}

	@Override
	public boolean shouldOverApproximate() {
		return hasLoops();
	}

	@Override
	public IFieldGraph overapproximation() {
		return new SetBasedFieldGraph(new HashSet<>(Arrays.asList(fields)));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(fields);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldGraph other = (FieldGraph) obj;
		if (!Arrays.equals(fields, other.fields))
			return false;
		return true;
	}

	



}
