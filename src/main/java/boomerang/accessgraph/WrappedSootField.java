package boomerang.accessgraph;

import soot.SootField;
import soot.Unit;

/**
 * A wrapped SootField. It can have a more precise type information than the original SootField.
 * 
 * @author spaeth
 *
 */
public class WrappedSootField {
  private SootField field;
  private Unit stmt;
  public static boolean TRACK_STMT = false;

  public WrappedSootField(SootField f, Unit s) {
    this.field = f;
    this.stmt = (TRACK_STMT ? s : null);
  }

  public SootField getField() {
    return field;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((field == null) ? 0 : field.hashCode());
    result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
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
    WrappedSootField other = (WrappedSootField) obj;
    if (field == null) {
      if (other.field != null)
        return false;
    } else if (!field.equals(other.field))
      return false;
    if (stmt == null) {
      if (other.stmt != null)
        return false;
    } else if (!stmt.equals(other.stmt))
      return false;
    return true;
  }

  public String toString() {
    return field.getName().toString();
  }

}
