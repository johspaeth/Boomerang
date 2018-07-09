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
public class KLimitedFieldSequence implements IFieldGraph {

	final LinkedList<WrappedSootField> fields;
	final boolean isOverapprimated;
	static KLimitedFieldSequence EMPTY_GRAPH = new KLimitedFieldSequence() {
		public String toString() {
			return "EMPTY_GRAPH";
		};
	};

	KLimitedFieldSequence(WrappedSootField[] fields, boolean overapproximated) {
		isOverapprimated = overapproximated;
		assert fields != null && fields.length > 0;
		this.fields = new LinkedList<>(Arrays.asList(fields));
	}

	KLimitedFieldSequence(WrappedSootField f) {
		assert f != null;
		this.fields = new LinkedList<>();
		this.fields.add(f);
		this.isOverapprimated = false;
	}

	private KLimitedFieldSequence(LinkedList<WrappedSootField> fields, boolean overapproximated) {
		this.fields = fields;
		isOverapprimated = overapproximated;
	}

	private KLimitedFieldSequence() {
		this.fields = new LinkedList<>();
		this.isOverapprimated = false;
	}

	/**
	 * 
	 * @return
	 */
	public Set<IFieldGraph> popFirstField() {
		if (fields.isEmpty())
			return new HashSet<>();
		Set<IFieldGraph> out = new HashSet<>();
		LinkedList<WrappedSootField> newFields = new LinkedList<>(fields);
		newFields.removeFirst();
		if(newFields.isEmpty())
			out.add(KLimitedFieldSequence.EMPTY_GRAPH);
		else
			out.add(new KLimitedFieldSequence(newFields, isOverapprimated));
		return out;
	}

	public WrappedSootField[] getFields() {
		return fields.toArray(new WrappedSootField[] {});
	}

	public IFieldGraph prependField(WrappedSootField f) {
		LinkedList<WrappedSootField> newFields = new LinkedList<>(fields);
		newFields.addFirst(f);
		return new KLimitedFieldSequence(newFields, isOverapprimated);
	}

	public Set<IFieldGraph> popLastField() {
		Set<IFieldGraph> out = new HashSet<>();
		if (fields.isEmpty())
			return out;
		LinkedList<WrappedSootField> newFields = new LinkedList<>(fields);
		newFields.removeLast();
		if(newFields.isEmpty())
			out.add(KLimitedFieldSequence.EMPTY_GRAPH);
		else
			out.add(new KLimitedFieldSequence(newFields, isOverapprimated));
		return out;
	}

	public IFieldGraph append(IFieldGraph o) {
		if (o instanceof KLimitedFieldSequence) {
			KLimitedFieldSequence other = (KLimitedFieldSequence) o;
			LinkedList<WrappedSootField> fields2 = other.fields;
			LinkedList<WrappedSootField> newFields = new LinkedList<>(fields);
			newFields.addAll(fields2);
			return new KLimitedFieldSequence(newFields, isOverapprimated || other.isOverapprimated);
		}
		throw new RuntimeException("Not yet implemented!");
	}

	public IFieldGraph appendFields(WrappedSootField[] toAppend) {
		return append(new KLimitedFieldSequence(toAppend, isOverapprimated));
	}

	public Set<WrappedSootField> getEntryNode() {
		Set<WrappedSootField> out = new HashSet<>();
		if(fields.size() > 0) {
			out.add(fields.get(0));
		}
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
		return Collections.singleton(fields.getLast());
	}

	public String toString() {
		String str = "";
		str += fields.toString();
		return str;
	}

	@Override
	public boolean shouldOverApproximate() {
		return this.fields.size() > AccessGraph.KLimiting ;
	}

	@Override
	public IFieldGraph overapproximation() {
		LinkedList<WrappedSootField> sublist = new LinkedList<>(fields.subList(0, AccessGraph.KLimiting));
		return new KLimitedFieldSequence(sublist, true);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + (isOverapprimated ? 1231 : 1237);
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
		KLimitedFieldSequence other = (KLimitedFieldSequence) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (isOverapprimated != other.isOverapprimated)
			return false;
		return true;
	}

}
