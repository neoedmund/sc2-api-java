package neoe.sc2.bot.task;

import neoe.sc2.bot.Bot;
import neoe.util.Log;

public abstract class Task {

	protected Bot bot;

	Thread thread;

	public Task(Bot bot) {
		this.bot = bot;
	}

	abstract public void logic() throws Exception;

	public void start() {
		thread = new Thread() {
			public void run() {
				try {
					logic();
				} catch (Exception e) {
					Log.log("task fail!", e);
					e.printStackTrace();
				}
				bot.th.remove(thread);
			}
		};
		thread.start();
		bot.th.add(thread);
	}

	public void waitDone() throws InterruptedException {
		if (thread != null) {
			thread.join();
		}
	}

}
