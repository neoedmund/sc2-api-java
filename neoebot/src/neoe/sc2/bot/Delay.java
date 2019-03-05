package neoe.sc2.bot;

/** not work as expected? */
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
		int x = (int) (i - t1);
		if (x > 0) {
			U.sleep(Math.min(SAFE, x));
		}
		t = System.currentTimeMillis();
	}

}
