/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE. All rights reserved. This
 * program and the accompanying materials are made available under the terms of the GNU Lesser
 * Public License v2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric Bodden, and others.
 ******************************************************************************/
package boomerang.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import heros.BiDiInterproceduralCFG;
import heros.incremental.DefaultUpdatableWrapper;
import heros.incremental.UpdatableInterproceduralCFG;
import heros.incremental.UpdatableWrapper;
import heros.solver.Pair;
import heros.util.Utils;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.SceneDiff;
import soot.jimple.toolkits.ide.icfg.SceneDiff.ClassDiffNode;
import soot.jimple.toolkits.ide.icfg.SceneDiff.DiffType;
import soot.jimple.toolkits.ide.icfg.SceneDiff.MethodDiffNode;
import soot.jimple.toolkits.ide.icfg.SceneDiff.ProgramDiffNode;
import soot.toolkits.graph.DirectedGraph;
import soot.util.Chain;
import soot.util.queue.QueueReader;

/**
 * Interprocedural control-flow graph for the infoflow solver
 * 
 * @author Steven Arzt
 * @author Eric Bodden
 */
public class ExtendedICFG extends AbstractUpdatableExtendedICFG<Unit, SootMethod> {

	private final static boolean DEBUG = true;
	private boolean quickDiff = false;

	protected final SceneDiff sceneDiff = new SceneDiff();

	private static enum StaticFieldUse {
		Unknown, Unused, Read, Write, ReadWrite
	}

	protected final Map<SootMethod, Map<SootField, StaticFieldUse>> staticFieldUses = new ConcurrentHashMap<SootMethod, Map<SootField, StaticFieldUse>>();

	private final Set<SootMethod> METHODS_TO_STRING = new HashSet<>();
	private final Set<SootMethod> METHODS_EQUALS = new HashSet<>();
	private final Set<SootMethod> IGNORED_METHODS = new HashSet<>();

	protected final BiDiInterproceduralCFG<Unit, SootMethod> delegate;


	public ExtendedICFG() {
		this(true);
	}
	
	public ExtendedICFG(boolean exceptionAnalysis) {
		this(new JimpleBasedInterproceduralCFG(exceptionAnalysis));
	}

	public ExtendedICFG(BiDiInterproceduralCFG<Unit, SootMethod> delegate) {
		super(delegate);
		this.sceneDiff.fullBuild();
		this.delegate = delegate;
		preanalysis();
	}
	
	@Override
	protected BiDiInterproceduralCFG<Unit, SootMethod> createBaseCFG() {
		return this.delegate;
	}
	
	@Override
	protected BiDiInterproceduralCFG<Unit, SootMethod> getBaseCFG() {
		return (JimpleBasedInterproceduralCFG) super.getBaseCFG();
	}
	
	public JimpleBasedInterproceduralCFG getBaseECFG() {
		return (JimpleBasedInterproceduralCFG) super.getBaseCFG();
	}

	private void preanalysis() {
		//		preanalysis = new FieldPreanalysis(this);
		selectSpecialMethods();
	}

	private void selectSpecialMethods() {
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			SootMethod method = listener.next().method();
			if (method.getSubSignature().equals("java.lang.String toString()")) {
				METHODS_TO_STRING.add(method);
				IGNORED_METHODS.add(method);
			}
			if (method.getSubSignature().equals("boolean equals(java.lang.Object)")) {
				METHODS_EQUALS.add(method);
				IGNORED_METHODS.add(method);
			}
			if (method.getName().equals("hashCode")) {
				IGNORED_METHODS.add(method);
			}
		}
	}


	@Override
	public boolean isIgnoredMethod(SootMethod method) {
		return IGNORED_METHODS.contains(method);
	}


	// delegate methods follow

	/*@Override
	public  UpdatableWrapper<M> getMethodOf(UpdatableWrapper<N> n) {
		return delegate.getMethodOf(u);
	}

	@Override
	public List<Unit> getSuccsOf(Unit u) {
		return delegate.getSuccsOf(u);
	}

	@Override
	public boolean isExitStmt(Unit u) {
		return delegate.isExitStmt(u);
	}

	@Override
	public boolean isStartPoint(Unit u) {
		return delegate.isStartPoint(u);
	}

	@Override
	public boolean isFallThroughSuccessor(Unit u, Unit succ) {
		return delegate.isFallThroughSuccessor(u, succ);
	}

	@Override
	public boolean isBranchTarget(Unit u, Unit succ) {
		return delegate.isBranchTarget(u, succ);
	}

	@Override
	public Collection<Unit> getStartPointsOf(SootMethod m) {
		return delegate.getStartPointsOf(m);
	}

	@Override
	public boolean isCallStmt(Unit u) {
		return delegate.isCallStmt(u);
	}*/

	/*@Override
	public Collection<SootMethod> getCalleesOfCallAt(Unit u) {
		return delegate.getCalleesOfCallAt(u);
	}

	@Override
	public Collection<Unit> getCallersOf(SootMethod m) {
		return delegate.getCallersOf(m);
	}

	@Override
	public Collection<Unit> getReturnSitesOfCallAt(Unit u) {
		return delegate.getReturnSitesOfCallAt(u);
	}

	@Override
	public Set<Unit> getCallsFromWithin(SootMethod m) {
		return delegate.getCallsFromWithin(m);
	}*/

	/*@Override
	public Collection<Unit> getEndPointsOf(SootMethod m) {
		return delegate.getEndPointsOf(m);
	}*/

	//	Delegates added to AbstractUpdatableExtendedICFG
	/*@Override
	public Set<Unit> allNonCallStartNodes() {
		return delegate.allNonCallStartNodes();
	}

	@Override
	public List<Unit> getPredsOf(Unit u) {
		return delegate.getPredsOf(u);
	}

	@Override
	public List<Unit> getPredsOfCallAt(Unit u) {
		return delegate.getPredsOf(u);
	}

	@Override
	public Set<Unit> allNonCallEndNodes() {
		return delegate.allNonCallEndNodes();
	}

	@Override
	public DirectedGraph<Unit> getOrCreateUnitGraph(SootMethod m) {
		return delegate.getOrCreateUnitGraph(m);
	}

	@Override
	public List<Value> getParameterRefs(SootMethod m) {
		return delegate.getParameterRefs(m);
	}

	@Override
	public boolean isReturnSite(Unit n) {
		return delegate.isReturnSite(n);
	}


	@Override
	public boolean isStaticFieldUsed(SootMethod method, SootField variable) {
		return isStaticFieldUsed(method, variable, new HashSet<SootMethod>(), false);
	}

	@Override
	public boolean isReachable(Unit u) {
		return false;
	}*/

	private boolean isStaticFieldUsed(SootMethod method, SootField variable, Set<SootMethod> runList,
			boolean readOnly) {
		// Without a body, we cannot say much
		if (!method.hasActiveBody())
			return false;

		// Do not process the same method twice
		if (!runList.add(method))
			return false;

		// Do we already have an entry?
		Map<SootField, StaticFieldUse> entry = staticFieldUses.get(method);
		if (entry != null) {
			StaticFieldUse b = entry.get(variable);
			if (b != null && b != StaticFieldUse.Unknown) {
				if (readOnly)
					return b == StaticFieldUse.Read || b == StaticFieldUse.ReadWrite;
				else
					return b != StaticFieldUse.Unused;
			}
		}

		// Scan for references to this variable
		for (Unit u : method.getActiveBody().getUnits()) {
			if (u instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) u;

				if (assign.getLeftOp() instanceof StaticFieldRef) {
					SootField sf = ((StaticFieldRef) assign.getLeftOp()).getField();
					registerStaticVariableUse(method, sf, StaticFieldUse.Write);
					if (!readOnly && variable.equals(sf))
						return true;
				}

				if (assign.getRightOp() instanceof StaticFieldRef) {
					SootField sf = ((StaticFieldRef) assign.getRightOp()).getField();
					registerStaticVariableUse(method, sf, StaticFieldUse.Read);
					if (variable.equals(sf))
						return true;
				}
			}

			if (((Stmt) u).containsInvokeExpr())
				for (Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(u); edgeIt.hasNext();) {
					Edge e = edgeIt.next();
					if (isStaticFieldUsed(e.getTgt().method(), variable, runList, readOnly))
						return true;
				}
		}

		// Variable is not read
		registerStaticVariableUse(method, variable, StaticFieldUse.Unused);
		return false;
	}

	private void registerStaticVariableUse(SootMethod method, SootField variable, StaticFieldUse fieldUse) {
		Map<SootField, StaticFieldUse> entry = staticFieldUses.get(method);
		StaticFieldUse oldUse;
		synchronized (staticFieldUses) {
			if (entry == null) {
				entry = new ConcurrentHashMap<SootField, StaticFieldUse>();
				staticFieldUses.put(method, entry);
				entry.put(variable, fieldUse);
				return;
			}

			oldUse = entry.get(variable);
			if (oldUse == null) {
				entry.put(variable, fieldUse);
				return;
			}
		}

		// This part is monotonic, so no need for synchronization
		StaticFieldUse newUse;
		switch (oldUse) {
		case Unknown:
		case Unused:
		case ReadWrite:
			newUse = fieldUse;
			break;
		case Read:
			newUse = (fieldUse == StaticFieldUse.Read) ? oldUse : StaticFieldUse.ReadWrite;
			break;
		case Write:
			newUse = (fieldUse == StaticFieldUse.Write) ? oldUse : StaticFieldUse.ReadWrite;
			break;
		default:
			throw new RuntimeException("Invalid field use");
		}
		entry.put(variable, newUse);
	}

	//	newly added
	@Override
	public boolean isStaticFieldUsed(SootMethod method, SootField variable) {
		return isStaticFieldUsed(method, variable, new HashSet<SootMethod>(), false);
	}

	private void quickDiffMethod(
			ExtendedICFG newJimpleCFG,
			SootMethod oldMethod,
			SootMethod newMethod,
			Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> expiredEdges,
			Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> newEdges,
			Set<UpdatableWrapper<Unit>> newNodes,
			Set<UpdatableWrapper<Unit>> expiredNodes) {
		long beforeQuickDiff = System.nanoTime();

		List<UpdatableWrapper<Unit>> oldSps = getStartPointsOf(wrap(oldMethod));
		List<UpdatableWrapper<Unit>> newSps = newJimpleCFG.getStartPointsOf(newJimpleCFG.wrap(newMethod));

		// We need to match the references of the start points
		Map<Unit, Unit> refChanges = new HashMap<Unit, Unit>();
		boolean hasSeed = true;
		for (UpdatableWrapper<Unit> spOld : oldSps) {
			boolean found = false;
			for (UpdatableWrapper<Unit> spNew : newSps)
				if (spOld.getContents().toString().equals(spNew.getContents().toString())) {
					refChanges.put(spOld.getContents(), spNew.getContents());
					found = true;
					break;
				}

			// If we cannot match the start points, there is no seed in the
			// current method
			if (!found)
				hasSeed = false;
		}

		for (Unit oldUnit : oldMethod.getActiveBody().getUnits()) {
			UpdatableWrapper<Unit> wOldUnit = wrap(oldUnit);
			if (!hasSeed || !oldSps.contains(wOldUnit))
				expiredNodes.add(wOldUnit);
			for (UpdatableWrapper<Unit> succ : getSuccsOf(wOldUnit))
				Utils.addElementToMapList(expiredEdges, wOldUnit, succ);
		}
		for (Unit newUnit : newMethod.getActiveBody().getUnits()) {
			UpdatableWrapper<Unit> wNewUnit = newJimpleCFG.wrap(newUnit);
			if (!hasSeed || !newSps.contains(wNewUnit))
				newNodes.add(wNewUnit);
			for (UpdatableWrapper<Unit> succ : newJimpleCFG.getSuccsOf(wNewUnit))
				Utils.addElementToMapList(newEdges, wNewUnit, succ);
		}

		for (Entry<Unit, Unit> entry : refChanges.entrySet())
			notifyReferenceChanged(entry.getKey(), entry.getValue());

		System.out.println("Quick diff took " + (System.nanoTime() - beforeQuickDiff) / 1E9
				+ " seconds.");
	}

	private class ComputeMethodChangesetTask implements Runnable {

		private final ExtendedICFG newCFG;
		private final SootMethod oldMethod;
		private final SootMethod newMethod;
		private final Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> expiredEdges;
		private final Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> newEdges;
		private final Set<UpdatableWrapper<Unit>> newNodes;
		private final Set<UpdatableWrapper<Unit>> expiredNodes;

		public ComputeMethodChangesetTask
		(ExtendedICFG newCFG,
				SootMethod oldMethod,
				SootMethod newMethod,
				Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> expiredEdges,
				Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> newEdges,
				Set<UpdatableWrapper<Unit>> newNodes,
				Set<UpdatableWrapper<Unit>> expiredNodes) {
			this.newCFG = newCFG;
			this.oldMethod = oldMethod;
			this.newMethod = newMethod;
			this.expiredEdges = expiredEdges;
			this.newEdges = newEdges;
			this.newNodes = newNodes;
			this.expiredNodes = expiredNodes;
		}

		@Override
		public void run() {
			boolean reallyChanged = computeMethodChangeset(newCFG, oldMethod,
					newMethod, expiredEdges, newEdges, newNodes, expiredNodes);
			if (DEBUG && reallyChanged)
				System.out.println("Changed method: " + newMethod.getSignature());
		}

	}

	/**
	 * Updates the statement references for an unchanged method. All updateable
	 * references to statements in the old method are changed to point to
	 * their respective counterparts in the new method.
	 * @param oldMethod The old method before the update
	 * @param newMethod The new method after the update
	 */
	private void updateUnchangedMethodPointers(SootMethod oldMethod, SootMethod newMethod) {
		// If one of the two methods has no body, we cannot match anything
		if (oldMethod == null || !oldMethod.hasActiveBody()
				|| newMethod == null || !newMethod.hasActiveBody()
				|| oldMethod == newMethod)
			return;
		
		// As we expect the method to be unchanged, there should be the same
		// number of statements in both the old and the new version
		assert oldMethod.getActiveBody().getUnits().size() ==
				newMethod.getActiveBody().getUnits().size();
		
		// Update the statement references
		updatePointsFromChain(oldMethod.getActiveBody().getUnits(),
			newMethod.getActiveBody().getUnits());
		updatePointsFromChain(oldMethod.getActiveBody().getLocals(),
			newMethod.getActiveBody().getLocals());
	}
	
	/**
	 * Updates the wrapper references for two equivalent chains. The wrapper for
	 * the n-th element in the old chain is changed to point to the n-nth
	 * element in the new chain.
	 * @param oldChain The old chain
	 * @param newChain The new chain
	 */
	private <X> void updatePointsFromChain(Chain<X> oldChain, Chain<X> newChain) {
		if (oldChain.isEmpty() || newChain.isEmpty())
			return;
		
		X uold = oldChain.getFirst();
		X unew = newChain.getFirst();
		while (uold != null && unew != null) {
			assert uold.toString().contains("tmp$") || uold.toString().contains("access$")
				|| uold.toString().equals(unew.toString());
			if (uold instanceof Stmt && unew instanceof Stmt)
				updateReferences((Stmt) uold, (Stmt) unew);
			else
				notifyReferenceChanged(uold, unew);
			uold = oldChain.getSuccOf(uold);
			unew = newChain.getSuccOf(unew);
		}
	}
	
	/**
	 * Checks whether the given statement is part of this CFG.
	 * @param u The statement to check
	 * @return True if the given statement is part of this CFG, otherwise
	 * false
	 */
	public boolean containsStmt(Unit u) {
		return this.getBaseECFG().containsStmt(u);
	}

	/**
	 * Checks whether the given statement is part of this CFG.
	 * @param u The statement to check
	 * @return True if the given statement is part of this CFG, otherwise
	 * false
	 */
	@Override
	public boolean containsStmt(UpdatableWrapper<Unit> u) {
		return this.getBaseECFG().containsStmt(u.getContents());
	}
	
	/**
	 * Gets all classes that contain methods in this cfg
	 * @return All classes that can contain methods in this cfg
	 */
	public Collection<SootClass> getAllClasses() {
		return this.getBaseECFG().getAllClasses();
	}
	
	/**
	 * Computes the edge differences between two Soot methods.
	 * @param newCFG The new program graph. The current object is assumed to
	 * hold the old program graph.
	 * @param oldMethod The method before the changes were made
	 * @param newMethod The method after the changes have been made
	 * @param expiredEdges The map that receives the expired edge targets. If
	 * two edges a->b and a->c have expired, the entry (a,(b,c)) will be put in
	 * the map.
	 * @param newEdges The map that receives the new edge targets.
	 * @param newNodes The list that receives all nodes which have been added to
	 * the method 
	 * @param expiredNodes The list that receives all nodes which have been
	 * deleted from the method
	 * @param nodeReplaceSet The map that receives the mapping between old nodes
	 * and new ones. If a statement is left unchanged by the modifications to the
	 * method, it may nevertheless be represented by a new object in the program
	 * graph. Use this map to update any references you might hold to the old
	 * objects.
	 * @return True if the two methods were actually different, otherwise false.
	 */
	private boolean computeMethodChangeset
	(ExtendedICFG newCFG,
			SootMethod oldMethod,
			SootMethod newMethod,
			Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> expiredEdges,
			Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> newEdges,
			Set<UpdatableWrapper<Unit>> newNodes,
			Set<UpdatableWrapper<Unit>> expiredNodes) {

		assert newCFG != null;
		assert oldMethod != null;
		assert newMethod != null;
		assert expiredEdges != null;
		assert newEdges != null;
		assert newNodes != null;

		// Map the locals that have been retained
		for (Local oldLocal : oldMethod.getActiveBody().getLocals()) {
			for (Local newLocal : newMethod.getActiveBody().getLocals())
				if (oldLocal.getName().equals(newLocal.getName())
						&& oldLocal.getType().toString().equals(newLocal.getType().toString())) {
					notifyReferenceChanged(oldLocal, newLocal);
					break;
				}
		}

		// Delay reference changes until the end for not having to cope with
		// changing references inside our analysis
		Map<Unit, Unit> refChanges = new HashMap<Unit, Unit>();

		// For all entry points of the new method, try to find the corresponding
		// statements in the old method. If we don't a corresponding statement,
		// we record a NULL value.
		boolean reallyChanged = false;
		List<Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>>> workQueue =
				new ArrayList<Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>>>();
		Set<UpdatableWrapper<Unit>> doneList = new HashSet<UpdatableWrapper<Unit>>(10000);
		for (UpdatableWrapper<Unit> spNew : newCFG.getStartPointsOf
				(new DefaultUpdatableWrapper<SootMethod>(newMethod))) {
			UpdatableWrapper<Unit> spOld = findStatement
					(new DefaultUpdatableWrapper<SootMethod>(oldMethod), spNew);
			workQueue.add(new Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>>(spNew, spOld));
			if (spOld == null) {
				assert newCFG.containsStmt(spNew);
				newNodes.add(spNew);
			}
			else
				refChanges.put(spOld.getContents(), spNew.getContents());
		}

		while (!workQueue.isEmpty()) {
			// Dequeue the current element and make sure we don't run in circles
			Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>> ns = workQueue.remove(0);
			UpdatableWrapper<Unit> newStmt = ns.getO1();
			UpdatableWrapper<Unit> oldStmt = ns.getO2();
			if (!doneList.add(newStmt))
				continue;

			// If the current point is unreachable, we skip the remainder of the method
			assert newCFG.containsStmt(newStmt.getContents());

			// Find the outgoing edges and check whether they are new
			boolean isNewStmt = newNodes.contains(newStmt);
			for (UpdatableWrapper<Unit> newSucc : newCFG.getSuccsOf(newStmt)) {
				UpdatableWrapper<Unit> oldSucc = oldStmt == null ? null : findStatement(getSuccsOf(oldStmt), newSucc);
				if (oldSucc == null || !getSuccsOf(oldStmt).contains(oldSucc) || isNewStmt) {
					Utils.addElementToMapList(newEdges, oldStmt == null ? newStmt : oldStmt,
							oldSucc == null ? newSucc : oldSucc);
					reallyChanged = true;
				}
				if (oldSucc == null)
					newNodes.add(newSucc);
				workQueue.add(new Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>>
				(newSucc, oldSucc == null ? oldStmt : oldSucc));

				if (oldSucc != null)
					refChanges.put(oldSucc.getContents(), newSucc.getContents());
			}
		}

		// For all entry points of the old method, check whether we can reach a
		// statement that is no longer present in the new method.
		doneList.clear();
		for (UpdatableWrapper<Unit> spOld : getStartPointsOf(new DefaultUpdatableWrapper<SootMethod>(oldMethod))) {
			UpdatableWrapper<Unit> spNew = newCFG.findStatement(new DefaultUpdatableWrapper<SootMethod>(newMethod), spOld);
			workQueue.add(new Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>>(spNew, spOld));
			if (spNew == null)
				expiredNodes.add(spOld);
		}

		while (!workQueue.isEmpty()) {
			// Dequeue the current element and make sure we don't run in circles
			Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>> ns = workQueue.remove(0);
			UpdatableWrapper<Unit> newStmt = ns.getO1();
			UpdatableWrapper<Unit> oldStmt = ns.getO2();
			if (!doneList.add(oldStmt))
				continue;

			// If the current point is unreachable, we skip the remainder of the method
			assert this.containsStmt(oldStmt.getContents());

			// Find the outgoing edges and check whether they are expired
			boolean isExpiredStmt = expiredNodes.contains(oldStmt);
			for (UpdatableWrapper<Unit> oldSucc : getSuccsOf(oldStmt)) {
				UpdatableWrapper<Unit> newSucc = newStmt == null ? null : newCFG.findStatement
						(newCFG.getSuccsOf(newStmt), oldSucc);
				if (newSucc == null || !newCFG.getSuccsOf(newStmt).contains(newSucc) || isExpiredStmt) {
					Utils.addElementToMapList(expiredEdges, oldStmt, oldSucc);
					reallyChanged = true;
				}
				if (newSucc == null) {
					expiredNodes.add(oldSucc);
					assert this.containsStmt(oldSucc);
				}
				workQueue.add(new Pair<UpdatableWrapper<Unit>,UpdatableWrapper<Unit>>
				(newSucc == null ? newStmt : newSucc, oldSucc));

				if (newSucc != null)
					refChanges.put(oldSucc.getContents(), newSucc.getContents());
			}
		}

		// Make sure that every statement is either added or removed or remapped
		if (DEBUG) {
			doneList.clear();
			List<UpdatableWrapper<Unit>> checkQueue = new ArrayList<UpdatableWrapper<Unit>>();
			checkQueue.addAll(this.getStartPointsOf(wrap(oldMethod)));
			while (!checkQueue.isEmpty()) {
				UpdatableWrapper<Unit> curUnit = checkQueue.remove(0);
				if (!doneList.add(curUnit))
					continue;
				checkQueue.addAll(this.getSuccsOf(curUnit));
				assert expiredNodes.contains(curUnit)
				|| newNodes.contains(curUnit)
				|| refChanges.containsKey(curUnit.getContents());
			}
		}

		for (Entry<Unit,Unit> entry : refChanges.entrySet())
			updateReferences(entry.getKey(), entry.getValue());
		return reallyChanged;
	}
	
	private void updateReferences(Unit oldUnit, Unit newUnit) {
		notifyReferenceChanged(oldUnit, newUnit);

		Stmt oldStmt = (Stmt) oldUnit;
		Stmt newStmt = (Stmt) newUnit;
		if (oldStmt.containsFieldRef())
			notifyReferenceChanged(oldStmt.getFieldRef(), newStmt.getFieldRef());
		if (oldStmt.containsArrayRef())
			notifyReferenceChanged(oldStmt.getArrayRef(), newStmt.getArrayRef());
		
		assert this.containsStmt(oldUnit);
//		assert newCFG.containsStmt(newUnit);	
	}

	private UpdatableWrapper<Unit> findStatement
	(UpdatableWrapper<SootMethod> oldMethod,
			UpdatableWrapper<Unit> newStmt) {
		return findStatement(getStartPointsOf(oldMethod), newStmt);
	}

	private UpdatableWrapper<Unit> findStatement
	(Iterable<UpdatableWrapper<Unit>> oldMethod,
			UpdatableWrapper<Unit> newStmt) {
		List<UpdatableWrapper<Unit>> doneList = new ArrayList<UpdatableWrapper<Unit>>();
		return findStatement(oldMethod, newStmt, doneList);
	}

	private UpdatableWrapper<Unit> findStatement
	(Iterable<UpdatableWrapper<Unit>> oldMethod,
			UpdatableWrapper<Unit> newStmt,
			List<UpdatableWrapper<Unit>> doneList) {
		List<UpdatableWrapper<Unit>> workList = new ArrayList<UpdatableWrapper<Unit>>();
		for (UpdatableWrapper<Unit> u : oldMethod)
			workList.add(u);

		while (!workList.isEmpty()) {
			UpdatableWrapper<Unit> sp = workList.remove(0);
			if (doneList.contains(sp))
				continue;
			doneList.add(sp);

			if (sp == newStmt || sp.equals(newStmt) || sp.toString().equals
					(newStmt.toString()))
				return sp;
			workList.addAll(getSuccsOf(sp));
		}
		return null;
	}

	@Override
	public UpdatableWrapper<Unit> getLoopStartPointFor(UpdatableWrapper<Unit> stmt) {
		Unit u = getBaseECFG().getLoopStartPointFor(stmt.getContents());
		return u == null ? null : wrap(u);
	}

	@Override
	public Set<UpdatableWrapper<Unit>> getExitNodesForReturnSite(UpdatableWrapper<Unit> stmt) {
		return this.wrap(getBaseECFG().getExitNodesForReturnSite(stmt.getContents()));
	}

	@Override
	public UpdatableWrapper<SootMethod> getMethodOf(UpdatableWrapper<Unit> n) {
		UpdatableWrapper<SootMethod> method = super.getMethodOf(n); 
		assert this.sceneDiff.containsReachableMethod(method.getContents());
		return method;
	}
	
	/*@Override
	public <X> DirectedGraph<UpdatableWrapper<X>> wrap(DirectedGraph<X> obj) {
		// TODO Auto-generated method stub
		return null;
	}*/

	/**
	 * Sets whether quick diffing shall be used. Quick diffing only scans for
	 * structural changes when updating the CFG and then exchanges all
	 * statements in methods that are not preserved as-is.
	 * @param quickDiff True if quick diffing shall be enabled, otherwise false.
	 */
	public void setQuickDiff(boolean quickDiff) {
		this.quickDiff = quickDiff;
	}
	
	/**
	 * Gets whether quick diffing is used.  Quick diffing only scans for
	 * structural changes when updating the CFG and then exchanges all
	 * statements in methods that are not preserved as-is.
	 * @return True if quick diffing is enabled, otherwise false.
	 */
	public boolean getQuickDiff() {
		return this.quickDiff;
	}
	
	@Override
	public void computeCFGChangeset(UpdatableInterproceduralCFG<Unit, SootMethod> newCFG,
			Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> expiredEdges,
			Map<UpdatableWrapper<Unit>, List<UpdatableWrapper<Unit>>> newEdges, Set<UpdatableWrapper<Unit>> newNodes,
			Set<UpdatableWrapper<Unit>> expiredNodes) {
		if (!(newCFG instanceof ExtendedICFG))
			throw new RuntimeException("Cannot compare graphs of different type");
		ExtendedICFG newJimpleCFG =	(ExtendedICFG) newCFG;
		
		long beforeSceneDiff = System.nanoTime();
		System.out.println("Computing code diff...");
		ProgramDiffNode diffRoot = sceneDiff.incrementalBuild();
		if (diffRoot.isEmpty())
			System.out.println("Program is still unchanged");
		System.out.println("Incremental build done in "
				+ (System.nanoTime() - beforeSceneDiff) / 1E9 + " seconds.");

		/*
		CountingThreadPoolExecutor executor = new CountingThreadPoolExecutor
				(1, Runtime.getRuntime().availableProcessors(),
				30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		*/

		// Check for removed classes. All statements in all methods in all
		// removed classes are automatically expired
		Set<SootClass> changedClasses = new HashSet<SootClass>();
		for (ClassDiffNode cd : diffRoot) {
			changedClasses.add(cd.getNewClass());
			if (cd.getDiffType() == DiffType.REMOVED) {
				if (DEBUG)
					System.out.println("Removed class: " + cd.getOldClass().getName());
				for (MethodDiffNode md : cd.getMethodDiffs()) {
					assert md.getDiffType() == DiffType.REMOVED;
					assert md.getOldMethod() != null;
					for (Unit u : md.getOldMethod().retrieveActiveBody().getUnits()) {
						// Since we're running on the old CFG, we must still
						// have the statement
						assert this.containsStmt(u);
						expiredNodes.add(wrap(u));
					}
				}
			}
			else if (cd.getDiffType() == DiffType.ADDED) {
				if (DEBUG)
					System.out.println("Added class: " + cd.getNewClass().getName());
				for (MethodDiffNode md : cd.getMethodDiffs()) {
					assert md.getDiffType() == DiffType.ADDED;
					assert md.getNewMethod() != null;
					for (Unit u : md.getNewMethod().retrieveActiveBody().getUnits()) {
						UpdatableWrapper<Unit> wrapper = wrap(u);
						newNodes.add(wrapper);
						assert newJimpleCFG.containsStmt(u);
					}
				}
			}
			else if (cd.getDiffType() == DiffType.CHANGED) {
				// This class has been changed. All statements in new methods
				// are automatically new.
				for (SootMethod newMethod : cd.getNewClass().getMethods())
					if (newMethod.isConcrete()) {
						MethodDiffNode methodDiff = cd.getMethodDiff(newMethod);
						if (methodDiff != null && methodDiff.getDiffType() == DiffType.ADDED) {
							if (DEBUG)
								System.out.println("Added method: " + newMethod.getSignature());
							for (Unit u : newMethod.retrieveActiveBody().getUnits()) {
								UpdatableWrapper<Unit> wrapper = wrap(u);
								newNodes.add(wrapper);
								assert newJimpleCFG.containsStmt(u);
							}
						}
						else if (methodDiff != null && methodDiff.getDiffType() == DiffType.REMOVED)
							throw new RuntimeException("Invalid diff mode, new method cannot be removed");
					}
				
				// All statements in removed methods are automatically expired.
				for (SootMethod oldMethod : cd.getOldClass().getMethods())
					if (oldMethod.isConcrete()) {
						MethodDiffNode methodDiff = cd.getMethodDiff(oldMethod);
						if (methodDiff != null && methodDiff.getDiffType() == DiffType.REMOVED) {
							if (DEBUG)
								System.out.println("Removed method: " + oldMethod.getSignature());
							for (Unit u : oldMethod.retrieveActiveBody().getUnits()) {
								assert this.containsStmt(u);
								expiredNodes.add(wrap(u));
							}
						}
						else if (methodDiff != null && methodDiff.getDiffType() == DiffType.CHANGED) {
							// This method has been changed
							if (quickDiff) {
								quickDiffMethod(newJimpleCFG, oldMethod, methodDiff.getNewMethod(),
										expiredEdges, newEdges, newNodes, expiredNodes);
							}
							else {
								boolean reallyChanged = computeMethodChangeset(newJimpleCFG, oldMethod,
										methodDiff.getNewMethod(), expiredEdges, newEdges, newNodes, expiredNodes);
								if (DEBUG && reallyChanged)
									System.out.println("Changed method: " + methodDiff.getNewMethod().getSignature());
								/*
								executor.execute(new ComputeMethodChangesetTask(newJimpleCFG, oldMethod,
										methodDiff.getNewMethod(), expiredEdges, newEdges, newNodes, expiredNodes));
								*/
							}
						}
						else if (methodDiff != null && methodDiff.getDiffType() == DiffType.ADDED)
							throw new RuntimeException("Invalid diff mode, old method cannot be added");
						else if (methodDiff == null && cd.getNewClass().declaresMethod(oldMethod.getSubSignature())) {
							// This is an unchanged method in a modified class.
							// Fix the wrappers
							updateUnchangedMethodPointers(oldMethod, Scene.v().getMethod(oldMethod.getSignature()));
						}
					}
			}
			else
				throw new RuntimeException("Unknown change type: " + cd.getDiffType());
		}
		
		// Update the wrappers for classes that have not changed
		long beforePointerUpdate = System.nanoTime();
		for (SootClass newClass : newJimpleCFG.getAllClasses()) {
			SootClass oldClass = diffRoot.getOldClassFor(newClass);
			if (oldClass != null && !changedClasses.contains(newClass))
				for (SootMethod newMethod : newClass.getMethods()) {
					SootMethod oldMethod = diffRoot.getOldMethodFor(newMethod);
					updateUnchangedMethodPointers(oldMethod, newMethod);
				}
		}
		System.out.println("Unchanged method pointers updated in "
				+ (System.nanoTime() - beforePointerUpdate) / 1E9 + " seconds.");
		
		/*
		// Wait for the method diff threads to finish and shut down the executor
		try {
			executor.awaitCompletion();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		*/
	}
}
