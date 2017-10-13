package boomerang.incremental;

/**
 * Common interface for all sorts of wrappers which decouple arbitrary objects
 * and make their references exchangeable.
 *
 * @param <N> The type of object to wrap.
 */
public interface UpdatableWrapper<N> extends CFGChangeListener {

	
	/**
	 * Gets the object being wrapped.
	 * @return The object inside this wrapper
	 */
	public N getContents();
		
}
