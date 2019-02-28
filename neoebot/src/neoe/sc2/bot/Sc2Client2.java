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

import SC2APIProtocol.Common.Race;
import SC2APIProtocol.Sc2Api.Difficulty;
import SC2APIProtocol.Sc2Api.InterfaceOptions;
import SC2APIProtocol.Sc2Api.LocalMap;
import SC2APIProtocol.Sc2Api.PlayerSetup;
import SC2APIProtocol.Sc2Api.PlayerType;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestCreateGame;
import SC2APIProtocol.Sc2Api.RequestData;
import SC2APIProtocol.Sc2Api.RequestGameInfo;
import SC2APIProtocol.Sc2Api.RequestJoinGame;
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
		Sc2Client2 client = new Sc2Client2(setting);
		client.run(setting);

	}

	private void run(Setting setting) throws Exception {

		startSC2();
		waitSC2Ready();

		websocketConnect();

		waitFinish();

		gameThread.interrupt();
		wsc.close();
		wsc = null;
	}

	private void startGame(String mapName) throws Exception {
		SC2APIProtocol.Sc2Api.RequestCreateGame.Builder requestCreateGame = RequestCreateGame.newBuilder();
		requestCreateGame.setRealtime(true);
		requestCreateGame.setLocalMap(LocalMap.newBuilder().setMapPath(mapName + ".SC2Map").build());

		SC2APIProtocol.Sc2Api.RequestJoinGame.Builder joinGame = RequestJoinGame.newBuilder();

		{
			SC2APIProtocol.Sc2Api.PlayerSetup.Builder ps = PlayerSetup.newBuilder();
			ps.setType(PlayerType.Participant).setRace(Race.Zerg);// .setDifficulty(Difficulty.Medium);
			requestCreateGame.addPlayerSetup(ps.build());
			firstBot = new MyZergBot("brood", setting);
		}
		{
			SC2APIProtocol.Sc2Api.PlayerSetup.Builder ps = PlayerSetup.newBuilder();
			ps.setType(PlayerType.Computer).setRace(Race.Random).setDifficulty(Difficulty.Medium);
			requestCreateGame.addPlayerSetup(ps.build());
		}

		//
		joinGame.setRace(Race.Zerg).setOptions(InterfaceOptions.newBuilder().setRaw(true).setScore(true).build());

		//
		sendReq(Request.newBuilder().setCreateGame(requestCreateGame).build());
		sendReq(Request.newBuilder().setJoinGame(joinGame).build());

	}

	private static String getLatestVer(String gameDir) {
		File versions = new File(gameDir, "Versions");
		if (!versions.exists())
			U.err("cannot find versions path in " + gameDir);
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

			default: {
				ResponseObservation rob = resp.getObservation();
				if (rob != null && !rob.getPlayerResultList().isEmpty()) {
					Log.log("someone win:" + rob.getPlayerResultList());
					gameEnd = true;
				}

				try {
					if (firstBot != null) {
						firstBot.onResponse(resp);
					} else {
						Log.log("[DEF-resp]" + U.toJson(resp));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			}

		}
	}

	private void startSC2() throws Exception {
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
		ex.addArg("-displayMode", "0");
		if (setting.dataVersion != null) {
			ex.addArg("-dataVersion", setting.dataVersion);
		}
		ex.execute();
		Log.log("start " + exe);
		// U.sleep(3000);
	}

	public void waitSC2Ready() {
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

	private IBot firstBot;

	// private Thread listenThread;
	private boolean gameEnd = false;
	private Thread gameThread;

	private LinkedBlockingQueue<Response> input = new LinkedBlockingQueue<Response>(5000);
	private Status lastStatus;
//	private boolean realEnd;

	ResponseHandle rh = new ResponseHandle();

	private Setting setting;

	private WebSocketClient wsc;

	public Sc2Client2(Setting setting) {
		this.setting = setting;
	}

	private void cmd_Ob() throws Exception {
		sendReq(Request.newBuilder().setObservation(RequestObservation.newBuilder()).build());
	}

	private void sendBotReq() throws Exception {
		if (firstBot == null)
			return;
		List<Request> reqs = new ArrayList<>();
		firstBot.pullRequsts(reqs);
		if (!reqs.isEmpty()) {
			Log.log("[w]send botReq cnt:" + reqs.size());
			for (Request req : reqs) {
				sendReq(req);
			}
		}

	}

	private void cmd_pingTest() throws Exception {
		sendReq(Request.newBuilder().setPing(RequestPing.newBuilder()).build());
	}

	private void sendReq(Request msg) throws Exception {
		Log.log("[O]" + msg.getRequestCase() + (C.debugOut ? ("<" + msg + ">") : ""));
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
		String uri = "ws://" + setting.host + ":" + setting.port + C.urlPath;
		Log.log(uri);
		gameEnd = false;
		Log.log("input queue clear:" + input.size());
		input.clear();
		wsc = new WebSocketClient(new URI(uri)) {

			@Override
			public void onClose(int code, String reason, boolean remote) {
				U.d("onClose:" + reason + "," + code + "," + remote);
				gameEnd = true;
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
				if (firstBot == null) {
					Log.log("no bot found, exiting");
					return;
				}
				Log.log("gameThread start.");
				try {
					cmd_pingTest();
					startGame(C.MAP);
					
					sendReq(Request.newBuilder().setData(RequestData.newBuilder()).build());
					sendReq(Request.newBuilder().setGameInfo(RequestGameInfo.newBuilder()).build());
					
					// listenThread.start();
					Delay delay = new Delay();
					while (true) {
						cmd_Ob();
						delay.waitMax(100);
						sendBotReq();
						delay.waitMax(100);
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
