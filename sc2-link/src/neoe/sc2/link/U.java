package neoe.sc2.link;

import neoe.util.Log;

public class U {

	public static void err(String msg) {
		throw new RuntimeException(msg);
	}

	public static void d(Object s) {
		Log.log(s);
	}

	public static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
