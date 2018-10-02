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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import boomerang.preanalysis.FieldPreanalysis;
import heros.solver.IDESolver;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.Jimple;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.util.queue.QueueReader;

/**
 * Interprocedural control-flow graph for the infoflow solver
 * 
 * @author Steven Arzt
 * @author Eric Bodden
 */
public class ExtendedICFG implements IExtendedICFG {

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
		this.delegate = delegate;
		preanalysis();
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

	@Override
	public SootMethod getMethodOf(Unit u) {
		return delegate.getMethodOf(u);
	}

	@Override
	public List<Unit> getSuccsOf(Unit u) {
		if(u == null)
			return Lists.newArrayList();
		return delegate.getSuccsOf(u);
	}

	@Override
	public boolean isExitStmt(Unit u) {
		if(u == null)
			return false;
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
		if (u == null)
			return false;
		return delegate.isCallStmt(u);
	}

	@Override
	public Set<Unit> allNonCallStartNodes() {
		return delegate.allNonCallStartNodes();
	}

	@Override
	public Collection<SootMethod> getCalleesOfCallAt(Unit u) {
		if (u == null)
			return Collections.emptyList();
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
	}

	@Override
	public List<Unit> getPredsOf(Unit u) {
		if (u == null)
			return Collections.emptyList();
		return delegate.getPredsOf(u);
	}

	@Override
	public Collection<Unit> getEndPointsOf(SootMethod m) {
		return delegate.getEndPointsOf(m);
	}

	@Override
	public List<Unit> getPredsOfCallAt(Unit u) {
		if (u == null)
			return Collections.emptyList();
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
		if (n == null)
			return false;
		return delegate.isReturnSite(n);
	}

	@Override
	public boolean isStaticFieldUsed(SootMethod method, SootField variable) {
		return isStaticFieldUsed(method, variable, new HashSet<SootMethod>(), false);
	}

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

	@Override
	public boolean isReachable(Unit u) {
		return delegate.isReachable(u);
	}

}
