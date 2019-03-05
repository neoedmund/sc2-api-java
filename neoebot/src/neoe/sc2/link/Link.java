package neoe.sc2.link;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.Response;
import neoe.sc2.bot.C;
import neoe.sc2.bot.U;
import neoe.util.Log;

public class Link {
	public boolean gameEnd = false;
	Thread msgThread;
	private LinkedBlockingQueue<Object[]> reqQueue = new LinkedBlockingQueue<>(5000);

	private LinkedBlockingQueue<Object[]> sentQueue = new LinkedBlockingQueue<>(5000);

	private boolean showOB = false;
	private boolean showStep = false;
	private WebSocketClient wsc;

	public Link() {
		msgThread = new Thread() {
			public void run() {
				Log.log("msgThread start.");
//				List todo = new ArrayList();
				while (true) {
					U.nap();
					int size = reqQueue.size();
					boolean obd = false;
					int skip = 0;
					for (int i = 0; i < size; i++) {
						Object[] row = null;
						try {
							row = reqQueue.take();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Request req = (Request) row[0];
						if (req.hasObservation()) {
							if (obd) {
								skip++;
								continue;
							} else
								obd = true;
						}
						sentQueue.add(row);
						_send(req);
						while (!sentQueue.isEmpty()) {
							U.nap();
						}
					}
					if (skip > 0)
						Log.log("skip OB x " + skip);
					if (gameEnd)
						break;
				}
				Log.log("msgThread end.");
			}
		};
		msgThread.start();
	}

	protected void _send(Request msg) {
		wsc.send(msg.toByteArray());
	}

	public void sendReq(Request msg, Handle succ, Handle fail) {
		if ((showOB || !msg.hasObservation()) && (showStep || !msg.hasStep()))
			Log.log("[O]" + U.json(msg));
		reqQueue.add(new Object[] { msg, succ, fail });
	}

	public void sendReq2(Request msg) {
		sendReq(msg, null, null);
	}

	public void websocketConnect(Setting setting, Thread gameStart) throws Exception {
		String uri = "ws://" + setting.host + ":" + setting.port + C.urlPath;
		Log.log(uri);
		gameEnd = false;
		wsc = new WebSocketClient(new URI(uri)) {

			@Override
			public void onClose(int code, String reason, boolean remote) {
				U.d("onClose:" + reason + "," + code + "," + remote);
				if (!gameEnd)
					new Thread() {
						public void run() {
							try {// restart
								websocketConnect(setting, gameStart);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}.start();
			}

			@Override
			public void onError(Exception arg0) {
				U.d("onError:" + arg0);
				gameEnd = true;
			}

			public void onMessage(ByteBuffer bb) {
				try {
					Response resp = Response.parseFrom(bb.array());
					if (resp.getErrorCount() > 0) {
						Log.log("[skip msg]" + U.json(resp));
						return;
					}
					Object[] o = sentQueue.take();
					Request req = (Request) o[0];
					Handle succ = (Handle) o[1];
					Handle fail = (Handle) o[2];
					if (U.isSuccess(resp)) {
						if (succ != null) {
							if ((showOB || !resp.hasObservation()) && (showStep || !resp.hasStep()))
								Log.log(String.format("[succ]%s for %s", U.json(resp), U.json(req)));
							succ.run(resp);
						} else {
							Log.log("[ignore succ]" + U.json(resp) + " for " + U.json(req));
						}
					} else {
						if (fail != null) {
							Log.log("[fail]" + U.json(resp) + " for " + U.json(req));
							fail.run(resp);
						} else {
							Log.log("[ignore fail]" + U.json(resp) + " for " + U.json(req));
						}
					}
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
				U.d("open:ws");
				reqQueue.clear();
				sentQueue.clear();
				if (!gameStart.isAlive())
					gameStart.start();
			}
		};
		wsc.setTcpNoDelay(true);
		wsc.connect();
	}
}
