package boomerang.accessgraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetBasedFieldGraph implements IFieldGraph {

	private final int[] fields;
	public SetBasedFieldGraph(Set<WrappedSootField> fields) {
		this.fields = new int[fields.size()];
		int i = 0;
		for(WrappedSootField f:fields){
			this.fields[i] = FieldGraph.fieldToInt(f);
			i++;
		}
		Arrays.sort(this.fields);
//		assert fields.size() > 1;
	}
	
	public SetBasedFieldGraph(int[] fields) {
		this.fields = fields;
	}
	@Override
	public Set<IFieldGraph> popFirstField() {
		Set<IFieldGraph> out = new HashSet<>();
		out.add(this);
//		out.add(FieldGraph.EMPTY_GRAPH);
		return out;
	}

	@Override
	public Set<IFieldGraph> popLastField() {
		return popFirstField();
	}

	@Override
	public Collection<WrappedSootField> getEntryNode() {
		return getExitNode();
	}

	@Override
	public WrappedSootField[] getFields() {
		return new WrappedSootField[0];
	}

	@Override
	public IFieldGraph appendFields(WrappedSootField[] toAppend) {
		Set<Integer> newFields = new HashSet<>();
		for(WrappedSootField f : toAppend){
			boolean skip = false;
			int fieldInt = FieldGraph.fieldToInt(f);
			for(int existing : fields){
				if(fieldInt == existing)
					skip = true;
			}
			if(!skip)
				newFields.add(fieldInt);
		}
				
		int[] overapprox = new int[fields.length + newFields.size()];
				
		System.arraycopy(fields, 0, overapprox, 0, fields.length);
		int i = fields.length;
		for(Integer f: newFields){
			overapprox[i] = f;
			i++;
		}
		Arrays.sort(overapprox);
		return new SetBasedFieldGraph(overapprox);
	}

	@Override
	public IFieldGraph append(IFieldGraph graph) {
		return appendFields(graph.getFields());
	}

	@Override
	public IFieldGraph prependField(WrappedSootField f) {
		return appendFields(new WrappedSootField[]{f});
	}

	@Override
	public Collection<WrappedSootField> getExitNode() {
		Set<WrappedSootField> out = new HashSet<>();
		for(int i : fields)
		out.add(FieldGraph.intToField(i));
		return out;
	}

	@Override
	public boolean shouldOverApproximate() {
		return false;
	}

	@Override
	public IFieldGraph overapproximation() {
		throw new RuntimeException("Cannot overapproximate the approxmiation anymore");
	}
	
	public String toString(){
		String str = " **";
		for(int i : fields){
			str += FieldGraph.intToField(i)+",";
		}
		str+= "** ";
		return str;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : Arrays.hashCode(fields));
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
		} else if (!Arrays.equals(fields, other.fields))
			return false;
		return true;
	}

}
