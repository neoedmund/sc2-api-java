package neoe.sc2.bot;

import neoe.sc2.link.U;

public class Delay {
	private static final int SAFE = 1000;
	long t;

	public Delay() {
		set();
	}

	public void set() {
		t = System.currentTimeMillis();

	}

	public void waitMax(int i) {
		long t2 = System.currentTimeMillis();
		long t1 = t2 - t;
		if (t1 < i) {
			U.sleep(Math.min(SAFE, (int) (i - t1)));
		}
		t = t2;
	}

}
