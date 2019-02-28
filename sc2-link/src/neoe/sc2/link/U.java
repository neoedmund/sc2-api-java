package neoe.sc2.link;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

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

	public static String toJson(MessageOrBuilder m) throws InvalidProtocolBufferException {
		return JsonFormat.printer().print(m);
	}
}
