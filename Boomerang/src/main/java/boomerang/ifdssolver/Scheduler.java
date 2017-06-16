package boomerang.ifdssolver;

import java.util.LinkedList;

import boomerang.BoomerangTimeoutException;

public class Scheduler {
	protected LinkedList<Runnable> worklist = new LinkedList<>();

	public void add(Runnable runnable) {
		worklist.add(runnable);
	}

	public boolean isEmpty() {
		return worklist.isEmpty();
	}

	public Runnable poll() {
		return worklist.poll();
	}

	public void awaitExecution() {
		while (worklist != null && !worklist.isEmpty()) {
			Runnable task = worklist.poll();
			task.run();
		}
	}
}
