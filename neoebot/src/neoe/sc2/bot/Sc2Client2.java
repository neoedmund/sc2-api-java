package neoe.sc2.bot;

import java.io.File;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestObservation;
import SC2APIProtocol.Sc2Api.RequestPing;
import SC2APIProtocol.Sc2Api.Response;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import SC2APIProtocol.Sc2Api.Status;
import neoe.sc2.link.Exec;
import neoe.sc2.link.IBot;
import neoe.sc2.link.Setting;
import neoe.sc2.link.U;
import neoe.util.Log;

/** test on Tool assisted human play */
public class Sc2Client2 {

	public static void main(String[] args) throws Exception {

		Setting setting = new Setting();
		setting.gameDir = args[0];
		if (args.length > 1) {
			setting.gameVer = args[1];
		} else {
			setting.gameVer = getLatestVer(setting.gameDir);
		}
		new Sc2Client2(setting).run();

	}

	private void run() throws Exception {
		startSC2Windows(setting);
		waitSC2Ready(setting);

		while (true) {
			Sc2Client2 client = new Sc2Client2(setting);
			client.firstBot = new MyZergBot("brood", setting);
			while (true) {
				client.websocketConnect();
				client.waitFinish();
				if (client.realEnd) {
					break;
				}
			}
			Log.log("match end.");
			
			client.gameThread.interrupt();
			client.wsc.close();
			client.wsc = null;
			// next match
		}
	}

	private static String getLatestVer(String gameDir) {
		File versions = new File(gameDir, "Versions");
		U.err("cannot find exec versions in " + gameDir);
		String[] subs = versions.list();
		int max = 0;
		// like Base58400
		for (String sub : subs) {
			try {
				if (sub.startsWith("Base")) {
					int v = Integer.parseInt(sub.substring(4));
					if (v > max) {
						max = v;
					}
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		if (max > 0)
			return "Base" + max;
		else {
			U.err("cannot find exec versions in " + gameDir);
			return null;
		}
	}

	public class ResponseHandle {

		public synchronized void handle(Response resp) {
			Status st = resp.getStatus();
			if (!st.equals(lastStatus)) {
				Log.log(String.format("game status changed from %s to %s", lastStatus, st));
				lastStatus = resp.getStatus();
			}

			switch (resp.getResponseCase()) {
			case PING: {
				System.out.printf("<%s>\n", resp.getPing());
				break;
			}

			case OBSERVATION: {
				ResponseObservation rob = resp.getObservation();
				Log.log("OB:" + resp.getStatus() + ":" + (++cnt));
				if (resp.getStatus().equals(Status.ended)) {
					break;
				}
				if (!rob.getPlayerResultList().isEmpty()) {
					Log.log("someone win:" + rob.getPlayerResultList());
					gameEnd = true;
				} else {
					try {
						if (firstBot != null) {
							List<Request> reqs = new ArrayList<>();
							firstBot.onObservation(resp, reqs);
							botReq = reqs;
						} else {
							Log.log("[OB]" + rob);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// synchronized (obsSync) { obsSync.notifyAll(); }
				break;
			}

			default: {
				Log.log("Not handle:" + resp.getResponseCase());
				if (resp.toByteArray().length < 1000) {
					Log.log("dump:" + resp.toString());
				}
			}
			}

		}
	}

	private static final String urlPath = "/sc2api";

	private static void startSC2Windows(Setting setting) throws Exception {
		try {
			Socket so = new Socket(setting.host, setting.port);
			so.getInputStream();
			so.close();
			Log.log("Not starting SC2 because it seems already up.");
			return;
		} catch (Exception e) {
		}

		Exec ex = new Exec(setting.gameDir + "/Support64");
		String exe = String.format("%s/Versions/%s/SC2_x64.exe", setting.gameDir, setting.gameVer);
		ex.setCmd(exe);
		ex.addArg("-listen", setting.host);
		ex.addArg("-port", "" + setting.port);
		// ex.addArg("-displayMode", "0");
		if (setting.dataVersion != null) {
			ex.addArg("-dataVersion", setting.dataVersion);
		}
		ex.execute();
		Log.log("start " + exe);
		// U.sleep(3000);
	}

	public static void waitSC2Ready(Setting setting) {
		int retry = 99;
		for (int i = 0; i < retry; i++) {
			try {
				Socket so = new Socket(setting.host, setting.port);
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
				U.sleep(1000);
				continue;
			}
		}

	}

	private List<Request> botReq;

	private int cnt;

	private boolean debugOut = false;
	private IBot firstBot;

	// private Thread listenThread;
	private boolean gameEnd = false;
	private Thread gameThread;

	private LinkedBlockingQueue<Response> input = new LinkedBlockingQueue<Response>(5000);
	private Status lastStatus;
	private boolean realEnd;

	ResponseHandle rh = new ResponseHandle();

	private Setting setting;

	private WebSocketClient wsc;

	public Sc2Client2(Setting setting) {
		this.setting = setting;
	}

	private void cmd_Ob() throws Exception {
		sendReq(Request.newBuilder().setObservation(RequestObservation.newBuilder()).build());
		if (botReq != null) {
			Log.log("[w]send botReq=" + botReq.size());
			for (Request req : botReq) {
				sendReq(req);
			}
		}
	}

	private void cmd_pingTest() throws Exception {
		sendReq(Request.newBuilder().setPing(RequestPing.newBuilder()).build());
	}

	private void sendReq(Request msg) throws Exception {
		Log.log("[O]" + msg.getRequestCase() + (debugOut ? ("<" + msg + ">") : ""));
		wsc.send(msg.toByteArray());
		waitResp();
	}

	public void waitFinish() {
		while (true) {
			if (gameEnd) {
				Log.log("waitFinish exit");
				break;
			}
			U.sleep(1000);
		}
	}

	private void waitResp() throws Exception {
		{
			Response resp = input.take();
			rh.handle(resp);
		}
		while (true) {
			int size = input.size();
			if (size > 0) {
				Log.log("[w]in size:" + size);
				for (int i = 0; i < size; i++) {
					Response resp = input.take();
					rh.handle(resp);
				}
			} else {
				break;
			}
		}
	}

	public void websocketConnect() throws Exception {
		String host = setting.host;
		int port = setting.port;
		String uri = "ws://" + host + ":" + port + urlPath;
		Log.log(uri);
		gameEnd = false;
		realEnd = false;
		Log.log("input queue clear:" + input.size());
		input.clear();
		wsc = new WebSocketClient(new URI(uri)) {

			@Override
			public void onClose(int code, String reason, boolean remote) {
				U.d("onClose:" + reason + "," + code + "," + remote);
			}

			@Override
			public void onError(Exception arg0) {
				U.d("onError:" + arg0);
			}

			public void onMessage(ByteBuffer bb) {
				try {
					Response resp = Response.parseFrom(bb.array());
					U.d("[I]" + resp.getResponseCase() + ":" + bb.capacity());
					input.put(resp);
				} catch (Exception e) {
					Log.log("onMessage", e);
				}
			}

			@Override
			public void onMessage(String arg0) {
				U.d("msg:" + arg0);
			}

			@Override
			public void onOpen(ServerHandshake arg0) {
				U.d("open:" + arg0);
				startGameThread();
			}
		};
		wsc.setTcpNoDelay(true);
		wsc.connect();

	}

	private void startGameThread() {
		if (gameThread != null) {
			gameThread.interrupt();
			gameThread = null;
		}
		gameThread = new Thread() {
			public void run() {
				Log.log("gameThread start.");
				try {
					cmd_pingTest();
					// listenThread.start();
					while (true) {
						Thread.sleep(200);// relax
						cmd_Ob();
						if (gameEnd)
							break;
					}
				} catch (Throwable e) {
					Log.log("[gameBad]", e);
				}
				Log.log("gameThread end.");
			}
		};
		gameThread.start();
	}

}
