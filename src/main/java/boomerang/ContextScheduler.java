package boomerang;

import boomerang.ifdssolver.Scheduler;

public class ContextScheduler extends Scheduler {
	private int propagationCount;
	private BoomerangContext context;

	public ContextScheduler() {
	}
	
	public void setContext(BoomerangContext context){
		this.context = context;
	}

	@Override
	public void awaitExecution() {
		while (!worklist.isEmpty()) {
			Runnable task = worklist.poll();
			propagationCount++;
			if (propagationCount % 1000 == 0) {
				if (context.isOutOfBudget()) {
					throw new BoomerangTimeoutException();
				}
			}
			task.run();
		}
	}
}
