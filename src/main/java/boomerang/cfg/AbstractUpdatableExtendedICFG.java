package boomerang.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;

import heros.BiDiInterproceduralCFG;
import heros.InterproceduralCFG;
import heros.incremental.CFGChangeListener;
import heros.incremental.DefaultUpdatableWrapper;
import heros.incremental.UpdatableInterproceduralCFG;
import heros.incremental.UpdatableWrapper;
import heros.util.Utils;
import soot.SootMethod;
import soot.Unit;

public abstract class AbstractUpdatableExtendedICFG<N, M> implements IExtendedICFG<N, M>{
	private static final int DEFAULT_CAPACITY = 10000;
	private static final boolean BROADCAST_NOTIFICATIONS = true;

	private final LoadingCache<Object, UpdatableWrapper<?>> wrappedObjects;
	private final Map<Object, Set<CFGChangeListener>> objectListeners;
	private final Set<CFGChangeListener> globalListeners = new HashSet<CFGChangeListener>();

	private final BiDiInterproceduralCFG<N, M> baseCFG;

	/*public AbstractUpdatableExtendedICFG() {
		this(DEFAULT_CAPACITY);
	}*/

	public AbstractUpdatableExtendedICFG(BiDiInterproceduralCFG<Unit, SootMethod> baseCFG, int capacity) {
		CacheBuilder<Object, Object> cb = CacheBuilder.newBuilder().concurrencyLevel
				(Runtime.getRuntime().availableProcessors()).initialCapacity(capacity); //.weakKeys();		
		wrappedObjects = cb.build(new CacheLoader<Object, UpdatableWrapper<?>>() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public UpdatableWrapper<?> load(Object key) throws Exception {
				UpdatableWrapper wrapped = new DefaultUpdatableWrapper(key);
				registerListener(wrapped, key);
				return wrapped;
			}

		});

		objectListeners = new MapMaker().concurrencyLevel(Runtime.getRuntime().availableProcessors()).initialCapacity
				(100000).makeMap();
//		baseCFG = createBaseCFG();
		this.baseCFG = (BiDiInterproceduralCFG<N, M>) baseCFG;
	}

	public AbstractUpdatableExtendedICFG(BiDiInterproceduralCFG<Unit, SootMethod> baseCFG) {
		this(baseCFG, DEFAULT_CAPACITY);
	}

	/**
	 * Implementors must override this method to provide the base CFG to be
	 * wrapped
	 * @return The base CFG to be wrapped for allowing incremental updates
	 */
	protected abstract BiDiInterproceduralCFG<N, M> createBaseCFG();

	/**
	 * Returns the base CFG on which this updatable wrapper was created
	 * @return The base CFG on which this updatable wrapper was created
	 */
	protected InterproceduralCFG<N, M> getBaseCFG() {
		return this.baseCFG;
	}

	@Override
	public void registerListener(CFGChangeListener listener, Object reference) {
		if (!BROADCAST_NOTIFICATIONS || listener != reference)
			Utils.addElementToMapSet(objectListeners, reference, listener);			
	}

	@Override
	public void registerListener(CFGChangeListener listener) {
		synchronized (globalListeners) {
			if (!globalListeners.contains(listener))
				globalListeners.add(listener);
		}
	}

	@Override
	public void unregisterListener(CFGChangeListener listener, Object reference) {
		if (!BROADCAST_NOTIFICATIONS || listener != reference)
			Utils.removeElementFromMapSet(objectListeners, reference, listener);
	}

	@Override
	public void unregisterListener(CFGChangeListener listener) {
		synchronized (globalListeners) {
			globalListeners.remove(listener);
		}
	}

	/**
	 * Notifies all registered listeners that an object reference has changed.
	 * @param oldObject The old object that is replaced
	 * @param newObject The object that takes the place of the old one
	 */
	protected void notifyReferenceChanged(Object oldObject, Object newObject) {
		// Avoid spurious notifications
		if (oldObject == newObject)
			return;

		Set<CFGChangeListener> invokedListeners = new HashSet<CFGChangeListener>(1000);

		// Get the wrapper for the old object. If we broadcast notifications, we
		// directly inform this object.
		try {
			UpdatableWrapper<?> wrapper = this.wrappedObjects.get(oldObject);
			if (BROADCAST_NOTIFICATIONS && wrapper != null) {
				wrapper.notifyReferenceChanged(oldObject, newObject);
				invokedListeners.add(wrapper);
			}

			// Notify all explicitly registered object listeners
			Set<CFGChangeListener> objListeners = objectListeners.get(oldObject);
			if (objListeners != null) {
				for (CFGChangeListener listener : objListeners) {
					if (listener != null && invokedListeners.add(listener))
						listener.notifyReferenceChanged(oldObject, newObject);
				}

				// Make sure that we don't loose track of our listeners. Expired
				// listeners for gc'ed objects will automatically be removed by
				// the WeakHashMap.
				objectListeners.put(newObject, objListeners);
			}

			// Notify the global listeners that have not yet been notified as
			// object listeners
			for (CFGChangeListener listener : globalListeners)
				if (!invokedListeners.contains(listener))
					listener.notifyReferenceChanged(oldObject, newObject);

			// We must also update our list of wrapped objects
			this.wrappedObjects.put(newObject, wrapper);
			//			this.wrappedObjects.remove(oldObject);
		} catch (ExecutionException e) {
			System.err.println("Could not wrap object");
			e.printStackTrace();
		}		
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> UpdatableWrapper<X> wrap(X obj) {
		assert obj != null;
		try {
			return (UpdatableWrapper<X>) this.wrappedObjects.get(obj);
		} catch (ExecutionException e) {
			System.err.println("Could not wrap object");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public <X> List<UpdatableWrapper<X>> wrap(Collection<X> list) {
		assert list != null;
		List<UpdatableWrapper<X>> resList = new ArrayList<UpdatableWrapper<X>>(list.size());
		for (X x : list)
			resList.add(wrap(x));
		return resList;
	}

	@Override
	public <X> Set<UpdatableWrapper<X>> wrap(Set<X> set) {
		assert set != null;
		Set<UpdatableWrapper<X>> resSet = new HashSet<UpdatableWrapper<X>>(set.size());
		for (X x : set)
			resSet.add(wrap(x));
		return resSet;
	}

	/*@Override
	public <X> DirectedGraph<UpdatableWrapper<X>> wrap(DirectedGraph<X> obj) {
		// TODO Auto-generated method stub
		return null;
	}*/

	@Override
	public void merge(UpdatableInterproceduralCFG<N, M> otherCfg) {
		if (!(otherCfg instanceof AbstractUpdatableExtendedICFG))
			throw new RuntimeException("Unexpected control flow graph type");

		AbstractUpdatableExtendedICFG<N, M> other =
				(AbstractUpdatableExtendedICFG<N, M>) otherCfg;
		this.wrappedObjects.asMap().putAll(other.wrappedObjects.asMap());
	}

	@Override
	public UpdatableWrapper<M> getMethodOf(UpdatableWrapper<N> n) {
		return wrap(baseCFG.getMethodOf(n.getContents()));
	}

	/*public SootMethod getMethodOf(Unit u) {
		return this.getMethodOf(u);
	}*/

	@Override
	public List<UpdatableWrapper<N>> getSuccsOf(UpdatableWrapper<N> n) {
		return wrap(baseCFG.getSuccsOf(n.getContents()));
	}

	@Override
	public Collection<UpdatableWrapper<M>> getCalleesOfCallAt(UpdatableWrapper<N> n) {
		return wrap(baseCFG.getCalleesOfCallAt(n.getContents()));
	}

	@Override
	public Collection<UpdatableWrapper<N>> getCallersOf(UpdatableWrapper<M> m) {
		return wrap(baseCFG.getCallersOf(m.getContents()));
	}

	@Override
	public Set<UpdatableWrapper<N>> getCallsFromWithin(UpdatableWrapper<M> m) {
		return wrap(baseCFG.getCallsFromWithin(m.getContents()));
	}

	@Override
	public List<UpdatableWrapper<N>> getStartPointsOf(UpdatableWrapper<M> m) {
		return wrap(baseCFG.getStartPointsOf(m.getContents()));
	}

	@Override
	public Collection<UpdatableWrapper<N>> getEndPointsOf(UpdatableWrapper<M> m) {
		return wrap(baseCFG.getEndPointsOf(m.getContents()));
	}

	@Override
	public List<UpdatableWrapper<N>> getReturnSitesOfCallAt(UpdatableWrapper<N> n) {
		return wrap(baseCFG.getReturnSitesOfCallAt(n.getContents()));
	}

	@Override
	public boolean isCallStmt(UpdatableWrapper<N> stmt) {
		return baseCFG.isCallStmt(stmt.getContents());
	}

	@Override
	public boolean isExitStmt(UpdatableWrapper<N> stmt) {
		return baseCFG.isExitStmt(stmt.getContents());
	}

	@Override
	public boolean isStartPoint(UpdatableWrapper<N> stmt) {
		return baseCFG.isStartPoint(stmt.getContents());
	}

	@Override
	public Set<UpdatableWrapper<N>> allNonCallStartNodes() {
		return wrap(baseCFG.allNonCallStartNodes());
	}

	@Override
	public boolean isFallThroughSuccessor(UpdatableWrapper<N> stmt, UpdatableWrapper<N> succ) {
		return baseCFG.isFallThroughSuccessor(stmt.getContents(), stmt.getContents());
	}

	@Override
	public boolean isBranchTarget(UpdatableWrapper<N> stmt, UpdatableWrapper<N> succ) {
		return baseCFG.isBranchTarget(stmt.getContents(), succ.getContents());
	}

	@Override
	public List<UpdatableWrapper<N>> getPredsOf(UpdatableWrapper<N> n) {
		return wrap(baseCFG.getPredsOf(n.getContents()));
	}

	/**
	 * Gets the number of elements in this interprocedural CFG. This can be used
	 * as an indicator for the required capacity of derived CFGs, e.g. when
	 * performing incremental updates.
	 * @return The number of elements in this CFG.
	 */
	public long size() {
		return this.wrappedObjects.size();
	}

	// delegates From ideal
	/*@Override
	public Set<Unit> allNonCallStartNodes() {
		return wrap(baseCFG.allNonCallStartNodes());
	}*/

	/*@Override
	public List<Unit> getPredsOf(Unit u) {
		return baseCFG.getPredsOf(u);
	}*/

	@Override
	public List<UpdatableWrapper<N>> getPredsOfCallAt(UpdatableWrapper<N> n) {
		return wrap(baseCFG.getPredsOf(n.getContents()));
	}

	@Override
	public Set<UpdatableWrapper<N>> allNonCallEndNodes() {
		return wrap(baseCFG.allNonCallEndNodes());
	}

	/*@Override
	public DirectedGraph<UpdatableWrapper<N>> getOrCreateUnitGraph(UpdatableWrapper<M> m) {
		return wrap(baseCFG.getOrCreateUnitGraph(m.getContents()));
	}*/

	@Override
	public Object getParameterRefs(UpdatableWrapper<M> m) {
		return baseCFG.getParameterRefs(m.getContents());
	}

	@Override
	public boolean isReturnSite(UpdatableWrapper<N> n) {
		return baseCFG.isReturnSite(n.getContents());
	}

	@Override
	public boolean isReachable(UpdatableWrapper<N> u) {
		return false;
	}

}
