/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package boomerang.incremental;

import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.toolkits.graph.DirectedGraph;

/**
 * Dynamically updatable version of an interprocedural control flow graph.
 * @author sarzt
 *
 */
public interface UpdatableInterproceduralCFG<N, M>
		extends BiDiInterproceduralCFG<UpdatableWrapper<N>, UpdatableWrapper<M>>, CFGChangeProvider {

	/**
	 * Computes a change set of added and removed edges in the control-flow
	 * graph
	 * @param newCFG The control flow graph after the update
	 * @param expiredEdges A list which receives the edges that are no longer
	 * present in the updated CFG
	 * @param newEdges A list which receives the edges that have been newly
	 * introduced in the updated CFG
	 * @param newNodes A list which receives the nodes that have been newly
	 * introduced in the updated CFG
	 * @param expiredNodes A list which receives the nodes that have been newly
	 * introduced in the updated CFG
	 */
	public void computeCFGChangeset(UpdatableInterproceduralCFG<N, M> newCFG,
			Map<UpdatableWrapper<N>, List<UpdatableWrapper<N>>> expiredEdges,
			Map<UpdatableWrapper<N>, List<UpdatableWrapper<N>>> newEdges,
			Set<UpdatableWrapper<N>> newNodes,
			Set<UpdatableWrapper<N>> expiredNodes);

	/**
	 * Wraps an object and registers a listener so that the reference can be
	 * updated automatically when necessary. Implementations are required to
	 * return stable objects, i.e.:
	 * <ul>
	 * <li>Two calls to wrap(x) must return the same object for the same x</li>
	 * <li>The hash code of the returned wrapper must not depend on the
	 * wrapped object. More specifically, the hash code of y = wrap(x)
	 * must not change when y.notifyReferenceChanged(z) is called.</li>
	 * </ul>
	 * This function creates a weak reference so that both the wrapped object
	 * and the wrapper can be garbage-collected.
	 * @param obj The object to be wrapped
	 * @return The wrapped object
	 */
	public <X> UpdatableWrapper<X> wrap(X obj);

	/**
	 * Wraps an object and registers a listener so that the reference can be
	 * updated automatically when necessary. Implementations are required to
	 * return stable objects, i.e.:
	 * <ul>
	 * <li>Two calls to wrap(x) must return the same object for the same x</li>
	 * <li>The hash code of the returned wrapper must not depend on the
	 * wrapped object. More specifically, the hash code of y = wrap(x)
	 * must not change when y.notifyReferenceChanged(z) is called.</li>
	 * </ul>
	 * This function creates a weak reference so that both the wrapped object
	 * and the wrapper can be garbage-collected.
	 * @param obj The list of objects to be wrapped
	 * @return The list of wrapped objects
	 */
	public <X> List<UpdatableWrapper<X>> wrap(List<X> obj);

	/**
	 * Wraps an object and registers a listener so that the reference can be
	 * updated automatically when necessary. Implementations are required to
	 * return stable objects, i.e.:
	 * <ul>
	 * <li>Two calls to wrap(x) must return the same object for the same x</li>
	 * <li>The hash code of the returned wrapper must not depend on the
	 * wrapped object. More specifically, the hash code of y = wrap(x)
	 * must not change when y.notifyReferenceChanged(z) is called.</li>
	 * </ul>
	 * This function creates a weak reference so that both the wrapped object
	 * and the wrapper can be garbage-collected.
	 * @param obj The set of objects to be wrapped
	 * @return The set of wrapped objects
	 */
	public <X> Set<UpdatableWrapper<X>> wrap(Set<X> obj);
	
	/**
	 * Wraps an object and registers a listener so that the reference can be
	 * updated automatically when necessary. Implementations are required to
	 * return stable objects, i.e.:
	 * <ul>
	 * <li>Two calls to wrap(x) must return the same object for the same x</li>
	 * <li>The hash code of the returned wrapper must not depend on the
	 * wrapped object. More specifically, the hash code of y = wrap(x)
	 * must not change when y.notifyReferenceChanged(z) is called.</li>
	 * </ul>
	 * This function creates a weak reference so that both the wrapped object
	 * and the wrapper can be garbage-collected.
	 * @param obj The set of objects to be wrapped
	 * @return The set of wrapped objects
	 */
	public <X> DirectedGraph<UpdatableWrapper<X>> wrap(DirectedGraph<X> obj);

	/**
	 * Merges the wrappers controlled by this control flow graph with the
	 * ones of another CFG.
	 * <p>
	 * This means that this.wrap(x) must return the same object as b.wrap(x)
	 * after this.merge(b) has been called. If both CFGs originally produced
	 * different wrappers for the same object, implementors may resolve to
	 * either value (or a  completely new one) as long as both objects
	 * afterwards agree on the same wrapper.
	 * </p> 
	 * @param otherCfg The other control flow graph with which to merge
	 * the wrappers. 
	 */
	public void merge(UpdatableInterproceduralCFG<N, M> otherCfg);

	/**
	 * Gets the start point of the outermost loop containing the given
	 * statement. This functions only considers intraprocedural loops.
	 * @param stmt The statement for which to get the loop start point.
	 * @return The start point of the outermost loop containing the given
	 * statement, or NULL if the given statement is not contained in a
	 * loop.
	 */
	public UpdatableWrapper<N> getLoopStartPointFor(UpdatableWrapper<N> stmt);

	/**
	 * Gets all exit nodes that can transfer the control flow to the given
	 * return site.
	 * @param stmt The return site for which to get the exit nodes
	 * @return The set of exit nodes that transfer the control flow to the
	 * given return site.
	 */
	public Set<UpdatableWrapper<N>> getExitNodesForReturnSite(UpdatableWrapper<N> stmt);

	/**
	 * Checks whether the given statement is part of this CFG.
	 * @param u The statement to check
	 * @return True if the given statement is part of this CFG, otherwise
	 * false
	 */
	public boolean containsStmt(UpdatableWrapper<N> u);

	}
