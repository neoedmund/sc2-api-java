package neoe.sc2.link;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.protobuf.ByteString;

import SC2APIProtocol.Common.Race;
import SC2APIProtocol.Sc2Api.Difficulty;
import SC2APIProtocol.Sc2Api.InterfaceOptions;
import SC2APIProtocol.Sc2Api.LocalMap;
import SC2APIProtocol.Sc2Api.PlayerResult;
import SC2APIProtocol.Sc2Api.PlayerSetup;
import SC2APIProtocol.Sc2Api.PlayerSetup.Builder;
import SC2APIProtocol.Sc2Api.PlayerType;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestCreateGame;
import SC2APIProtocol.Sc2Api.RequestJoinGame;
import SC2APIProtocol.Sc2Api.RequestObservation;
import SC2APIProtocol.Sc2Api.RequestPing;
import SC2APIProtocol.Sc2Api.RequestStartReplay;
import SC2APIProtocol.Sc2Api.Response;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import SC2APIProtocol.Sc2Api.Status;
import neoe.util.FileUtil;
import neoe.util.Log;

public class Sc2Client {
	public class ResponseHandle {

		public synchronized void handle(Response resp) {
			Log.log("st:" + resp.getStatus());
			lastStatus = resp.getStatus();
			if (resp.getStatus().equals(Status.ended)) {
				gameEnd = true;
				gameBad = false;
				realEnd = true;
			}

			switch (resp.getResponseCase()) {
			case PING: {
				System.out.printf("<%s>\n", resp.getPing());
				break;
			}
			case CREATE_GAME: {
				if (resp.getCreateGame().hasError()) {
					Log.log("creategame error:" + resp.getCreateGame().getError());
					gameCreated = false;
				} else {
					gameCreated = true;
				}
				Log.log("gameEnd=" + gameEnd);
				cnt = 0;
				break;
			}
			case JOIN_GAME: {
				if (resp.getJoinGame().hasError()) {
					Log.log("joingame error:" + resp.getJoinGame().getError());
					gameJoined = false;
				} else {
					playerid = resp.getJoinGame().getPlayerId();
					Log.log("playerid:" + playerid);
					gameJoined = true;
				}

				break;
			}
			case OBSERVATION: {
				ResponseObservation rob = resp.getObservation();
				Log.log(resp.getStatus() + ":" + (++cnt));
				if (resp.getStatus().equals(Status.ended)) {
					break;
				}
				// Log.log(rob.getActionsList()); Log.log(rob.getActionErrorsList());
				// Log.log(rob.getPlayerResultList()); Log.log(rob.getChatList());
				if (!rob.getPlayerResultList().isEmpty()) {
					checkWin(rob.getPlayerResultList());
					gameEnd = true;
				} else {
					try {
						List<Request> reqs = new ArrayList<>();
						firstBot.onObservation(rob, reqs);
						botReq = reqs;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// synchronized (obsSync) { obsSync.notifyAll(); }
				break;
			}
			case START_REPLAY: {
				replayStarted = !resp.getStartReplay().hasError();
				if (!replayStarted) {
					Log.log(resp.getStartReplay().getError() + "," + resp.getStartReplay().getErrorDetails());
				}
				cnt = 0;
				break;
			}
			case STEP:
			case DEBUG: {// empty
				break;
			}
			default: {
				Log.log("I cannot handle:" + resp.getResponseCase());
				if (resp.toByteArray().length < 1000) {
					Log.log("dump:" + resp.toString());
				}
			}
			}

		}
	}

	public static class Setting {
		public String dataVersion;
		public boolean fog;
		public String gameDir;
		public String gameVer = "Base58400";
		public String host;
		public boolean isReplay;
		public String map;
		public int port;
		public boolean realtime = true;
	}

	private static final String urlPath = "/sc2api";

	public static void startMatch(Setting setting, String map, Object[][] ags) throws Exception {
		startSC2Windows(setting);
		waitSC2Ready(setting);

		while (true) {
			Sc2Client client = new Sc2Client(setting);
			client.setConfig(map, ags);
			while (true) {
				client.websocketConnect();
				client.waitFinish();
				if (client.realEnd) {
					break;
				}
				client.rejoin = true;
			}
			Log.log("match end.");
			client.gameThread.interrupt();
			client.wsc.close();
			client.wsc = null;
			// next match
		}
	}

	public static void startSC2Windows(Setting setting) throws Exception {

		try {
			Socket so = new Socket(setting.host, setting.port);
			so.getInputStream();
			so.close();
			Log.log("skip starting SC2 because guessing it's already up.");
			return;
		} catch (Exception e) {
		}

		Exec ex = new Exec(setting.gameDir + "/Support64");
		String exe = String.format("%s/Versions/%s/SC2_x64.exe", setting.gameDir, setting.gameVer);
		ex.setCmd(exe);
		ex.addArg("-listen", "127.0.0.1");
		ex.addArg("-port", "" + setting.port);
		ex.addArg("-displayMode", "0");
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

	private IBot[] bots;

	int cnt;

	private boolean dumpOut;
	private IBot firstBot;

	// private Thread listenThread;
	public boolean gameBad;

	private boolean gameCreated, gameJoined;

	public boolean gameEnd = false;
	// Object obsSync = new Object();

	private Thread gameThread;

	private LinkedBlockingQueue<Response> input = new LinkedBlockingQueue<Response>(5000);

	/** adjust later when bot vs bot */
	SC2APIProtocol.Sc2Api.RequestJoinGame.Builder joinGame;

	private IBot lastBot;
	private Status lastStatus;
	private LinkedBlockingQueue<Request> output = new LinkedBlockingQueue<Request>(5000);
	private int playerid;
	public boolean realEnd;
	/**
	 * SC2 will close websocket for unknow reason, but already accepted by its
	 * library
	 */
	public boolean rejoin;

	private boolean replayStarted;

	private SC2APIProtocol.Sc2Api.RequestCreateGame.Builder requestCreateGame;

	private SC2APIProtocol.Sc2Api.RequestStartReplay.Builder requestStartReplay;

	ResponseHandle rh = new ResponseHandle();

	private Setting setting;

	private SC2APIProtocol.Sc2Api.Result win;

	// private void addReq(Request msg) throws InterruptedException {
	// output.put(msg);
	// }

	WebSocketClient wsc;

	public Sc2Client(Setting setting) {
		this.setting = setting;
	}

	public void checkWin(List<PlayerResult> playerResultList) {
		for (PlayerResult res : playerResultList) {
			if (res.getPlayerId() == playerid) {
				win = res.getResult();
			}
		}
		Log.log("Me: " + win);
	}

	private void cmd_createGame() throws Exception {
		sendReq(Request.newBuilder().setCreateGame(requestCreateGame).build());
		if (!gameCreated && !Status.init_game.equals(lastStatus)) {
			U.err("oops");
		}
	}

	public void cmd_joinGame() throws Exception {
		sendReq(Request.newBuilder().setJoinGame(joinGame).build());
		if (!gameJoined) {
			U.err("oops");
		}

	}

	private void cmd_Ob() throws Exception {
		sendReq(Request.newBuilder().setObservation(RequestObservation.newBuilder()).build());
		if (botReq != null) {
			for (Request req : botReq) {
				sendReq(req);
			}
		} else {
			Log.log("[w]botReq=" + botReq);
		}

	}

	private void cmd_pingTest() throws Exception {
		sendReq(Request.newBuilder().setPing(RequestPing.newBuilder()).build());
	}

	private void cmd_startReply() throws Exception {
		sendReq(Request.newBuilder().setStartReplay(requestStartReplay).build());
		if (!replayStarted) {
			U.err("oops");
		}

	}

	private void sendMsg(Request msg) throws Exception {
		Log.log("[O]" + msg.getRequestCase() + (dumpOut ? ("<" + msg + ">") : ""));
		wsc.send(msg.toByteArray());

	}

	private void sendReq(Request req) throws Exception {
		sendMsg(req);
		waitResp();
	}

	public void setConfig(String map, Object[][] ags) throws Exception {
		if (map.endsWith(".SC2Replay")) {
			setConfigForReplay(map, ags);
			return;
		}

		requestCreateGame = RequestCreateGame.newBuilder();
		requestCreateGame.setRealtime(setting.realtime);
		setMap(map);

		setJoinGame(ags);

	}

	private void setConfigForReplay(String map, Object[][] ags) {
		requestStartReplay = RequestStartReplay.newBuilder();
		requestStartReplay.setReplayPath(map);
		SC2APIProtocol.Sc2Api.InterfaceOptions.Builder opts = InterfaceOptions.newBuilder();
		opts.setRaw(true).setScore(true);
		requestStartReplay.setOptions(opts);
		requestStartReplay.setDisableFog(!setting.fog);
		requestStartReplay.setObservedPlayerId(1);

		setJoinGame(ags);

	}

	private void setJoinGame(Object[][] ags) {
		joinGame = RequestJoinGame.newBuilder();
		bots = new IBot[ags.length];
		for (int i = 0; i < ags.length; i++) {
			Object[] row = ags[i];
			Builder ps = PlayerSetup.newBuilder();
			ps.setType((PlayerType) row[0]).setRace((Race) row[1]);
			if (row[2] != null) {
				ps.setDifficulty((Difficulty) row[2]);
			}
			if (!setting.isReplay) {
				requestCreateGame.addPlayerSetup(ps.build());
			}
			if (!PlayerType.Computer.equals(row[0])) {
				bots[i] = (IBot) row[3];
				if (firstBot == null) {
					firstBot = bots[i];
				}
				lastBot = bots[i];
				joinGame.setRace((Race) row[1])
						.setOptions(InterfaceOptions.newBuilder().setRaw(true).setScore(true).build());
			}
		}

	}

	private void setMap(String map) throws Exception {
		if (map.endsWith(".SC2Map")) {
			File f = new File(map);
			if (!f.exists()) {
				f = new File(new File(setting.gameDir, "Maps"), map);
			}
			if (!f.exists()) {
				U.err("cannot find local map:" + map);
			}
			boolean useBin = false;
			if (useBin) {
				byte[] bs = FileUtil.read(new FileInputStream(map));
				Log.log("map size:" + bs.length + " bytes");
				requestCreateGame.setLocalMap(LocalMap.newBuilder().setMapData(ByteString.copyFrom(bs)).build());
			} else {
				requestCreateGame.setLocalMap(LocalMap.newBuilder().setMapPath(map).build());
			}
			setting.map = f.getAbsolutePath();
		} else {
			requestCreateGame.setBattlenetMapName(map);
			setting.map = map;
		}

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
		String uri = "ws://" + host + ":" + port + urlPath; // "ws://echo.websocket.org";
		Log.log(uri);
		gameBad = false;
		gameEnd = false;
		realEnd = false;
		input.clear();
		wsc = new WebSocketClient(new URI(uri)) {

			@Override
			public void onClose(int code, String reason, boolean remote) {
				U.d("onClose:" + reason + "," + code + "," + remote);
				gameEnd = true;
				gameThread.interrupt();
				// listenThread.interrupt();
				// synchronized (obsSync) { obsSync.notifyAll(); }
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
				// listenThread = new Thread() {
				// public void run() {
				// // while (true) {
				// // try {
				// // waitResp();
				// // } catch (Throwable e) {
				// // e.printStackTrace();
				// // }
				// // if (gameEnd) {
				// // Log.log("listenThread exit because gameEnd");
				// // break;
				// // }
				// // }
				// }
				// };
				gameThread = new Thread() {

					public void run() {
						try {

							if (!rejoin) {
								if (setting.isReplay) {
									cmd_startReply();
								} else {
									cmd_createGame();
									cmd_joinGame();
								}
							}
							cmd_pingTest();
							// listenThread.start();
							while (true) {
								cmd_Ob();
								if (gameEnd)
									break;
							}

						} catch (Throwable e) {
							Log.log("gt:", e);
							gameBad = true;
							Log.log("[gameBad]" + e);
						}
						// listenThread.interrupt();
						Log.log("gameThread end.");
					}

				};
				gameThread.start();

			}
		};
		wsc.setTcpNoDelay(true);
		wsc.connect();
	}

}
