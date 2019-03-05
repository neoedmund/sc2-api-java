package neoe.sc2.bot;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import SC2APIProtocol.Common.ImageData;
import SC2APIProtocol.Common.Point;
import SC2APIProtocol.Data.UnitTypeData;
import SC2APIProtocol.Error.ActionResult;
import SC2APIProtocol.Raw.Unit;
import SC2APIProtocol.Sc2Api.Response;
import neoe.sc2.link.Exec;
import neoe.sc2.link.Setting;
import neoe.util.FileUtil;
import neoe.util.Log;

public class U {

	public static void d(Object s) {
		System.out.println(s);
		Log.log(s);
	}

	public static Object debugUnitCount(Bot bot, List<Unit> unitsList) {

		Map<Object, List<Unit>> m = unitsList.stream().collect(Collectors.groupingBy(u -> u.getUnitType()));

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Object o : m.keySet()) {
			int ut = (int) o;
			int cnt = m.get(o).size();
			UnitTypeData u = bot.idMap.get(ut);
			String name;
			if (u == null) {
				name = "" + ut;
			} else {
				name = u.getName();
			}
			sb.append(String.format("%s:%s,", name, cnt));
		}
		sb.append("}");
		return sb.toString();
	}

	public static float distanceSimple(Point p1, Point p2) {
		return (float) Math.sqrt(sqr(p1.getX() - p2.getX()) + sqr(p1.getY() - p2.getY()));
	}

	public static void dumpMap(String mapname, String name, ImageData data) throws IOException {
		int w = data.getSize().getX();
		int h = data.getSize().getY();
		int bufsize = (3 * w + 2) * h;
		StringBuilder sb = new StringBuilder(bufsize);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				sb.append(toHex(data.getData().byteAt(x + y * w))).append(' ');
			}
			sb.append('\n');
		}
		FileUtil.save(sb.toString().getBytes("UTF8"), mapname + "." + name);

	}

	public static void err(String msg) {
		throw new RuntimeException(msg);
	}

	public static boolean isSuccess(Response resp) {
		int x = resp.getAction().getResultCount();
		if (x > 0) {
			if (ActionResult.Success.equals(resp.getAction().getResult(0)))
				return true;
			return false;
		}
		return true;
	}

	public static String json(List list) {
		StringBuilder sb = new StringBuilder();
		sb.append(list.size() + "x[");
		for (Object o : list) {
			sb.append(json((MessageOrBuilder) o)).append(",");
		}
		sb.append("]");
		return sb.toString();
	}

	public static String json(MessageOrBuilder m) {
		try {
			return JsonFormat.printer().omittingInsignificantWhitespace().print(m);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return m.toString();
		}
	}

	public static void nap() {
		sleep(5);
	}

	public static void nap200() {
		sleep(200);
	}

	public static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static float sqr(float f) {
		return f * f;
	}

	public static void startSC2(Setting setting) throws Exception {
		try {
			Socket so = new Socket(setting.host, setting.port);
			so.getInputStream();
			so.close();
			Log.log("Not starting SC2 because it seems already up.");
			return;
		} catch (Exception e) {
		}

		String execdir = setting.gameDir + "/Support64";
		if (!new File(execdir).exists()) {// linux
			execdir = setting.gameDir;
		}
		Exec ex = new Exec(execdir);
		String exe = String.format("%s/Versions/%s/SC2_x64", setting.gameDir, setting.gameVer);
		ex.setCmd(exe);
		ex.addArg("-listen", setting.host);
		ex.addArg("-port", "" + setting.port);
		ex.addArg("-displayMode", "0");
		if (setting.dataVersion != null) {
			ex.addArg("-dataVersion", setting.dataVersion);
		}
		ex.execute();
		Log.log("start " + exe);
		// U.sleep(3000);
	}

	public static String toHex(byte b) {
		String s = Integer.toHexString(b & 0xff);
		if (s.length() == 1)
			s = "0" + s;
		return s;
	}

	public static void waitSC2Ready(String host, int port) {
		int retry = 99;
		for (int i = 0; i < retry; i++) {
			try {
				Socket so = new Socket(host, port);
				so.getInputStream();
				so.close();
				System.out.println("socket check OK");
				break;
			} catch (Exception e) {
				String msg = e.toString();
				if (msg.indexOf("refuse") > 0) {
					msg = "wait SC2 to ready";
				}
				System.out.printf("retry %s/%s : %s\n", (i + 1), retry, msg);
				sleep(1000);
				continue;
			}
		}

	}

}
