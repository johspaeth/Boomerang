package boomerang.accessgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;

import soot.Scene;
import soot.Type;

public class SetBasedFieldGraph implements IFieldGraph {
	private int hashCode = 0;
	private Set<WrappedSootField> fields;
	public static Set<WrappedSootField> allFields;

	public SetBasedFieldGraph(Set<WrappedSootField> fields) {
		if (allFields == null)
			allFields = new HashSet<>();
		allFields.addAll(fields);
		this.fields = fields;
	}

	@Override
	public Set<IFieldGraph> popFirstField() {
		Set<IFieldGraph> out = new HashSet<>();
		out.add(this);
		out.add(FieldGraph.EMPTY_GRAPH);
		return out;
	}

	@Override
	public Set<IFieldGraph> popLastField() {
		return popFirstField();
	}

	@Override
	public Collection<WrappedSootField> getEntryNode() {
		return fields;
	}

	@Override
	public WrappedSootField[] getFields() {
		return new WrappedSootField[0];
	}

	@Override
	public IFieldGraph appendFields(WrappedSootField[] toAppend) {
		boolean added = false;
		for (WrappedSootField f : toAppend){
			if(!fields.contains(f))
				added = true;
		}
		if(!added)
			return this;
		Set<WrappedSootField> overapprox = new HashSet<>(fields);
		for (WrappedSootField f : toAppend){
			overapprox.add(f);
		}
		return new SetBasedFieldGraph(overapprox);
	}

	@Override
	public IFieldGraph append(IFieldGraph graph) {
		return appendFields(graph.getFields());
	}

	@Override
	public IFieldGraph prependField(WrappedSootField f) {
		if(fields.contains(f))
			return this;
		Set<WrappedSootField> overapprox = new HashSet<>(fields);
		return new SetBasedFieldGraph(overapprox);
	}

	@Override
	public Collection<WrappedSootField> getExitNode() {
		return fields;
	}

	@Override
	public boolean shouldOverApproximate() {
		return false;
	}

	@Override
	public IFieldGraph overapproximation() {
		return this;
	}

	public String toString() {
		return " {" + Joiner.on(",").join(fields) + "}";
	}
	@Override
	public int hashCode() {
		if(hashCode != 0)
			return hashCode;
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		hashCode = result;
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
		SetBasedFieldGraph other = (SetBasedFieldGraph) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

}
