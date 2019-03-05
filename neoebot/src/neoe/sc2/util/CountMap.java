package neoe.sc2.util;

import java.util.HashMap;
import java.util.Map;

public class CountMap {
	public Map m;

	public CountMap() {
		m = new HashMap();
	}

	public void add(Object value, int add) {
		Integer cnt = (Integer) m.get(value);
		if (cnt == null) {
			cnt = add;
		} else {
			cnt += add;
		}
		if (cnt == 0) {
			m.remove(value);
		} else {
			m.put(value, cnt);
		}

	}

	public void clear() {
		m.clear();

	}

	public int get(Object value) {
		Integer cnt = (Integer) m.get(value);
		return cnt == null ? 0 : cnt;
	}

}
