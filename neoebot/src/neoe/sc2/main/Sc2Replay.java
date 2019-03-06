package neoe.sc2.main;

import java.io.File;
import java.util.Random;

import SC2APIProtocol.Sc2Api.InterfaceOptions;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestData;
import SC2APIProtocol.Sc2Api.RequestGameInfo;
import SC2APIProtocol.Sc2Api.RequestObservation;
import SC2APIProtocol.Sc2Api.RequestPing;
import SC2APIProtocol.Sc2Api.RequestStartReplay;
import SC2APIProtocol.Sc2Api.RequestStep;
import SC2APIProtocol.Sc2Api.Response;
import neoe.sc2.bot.Bot;
import neoe.sc2.bot.MyReplayViewer;
import neoe.sc2.bot.U;
import neoe.sc2.link.Handle;
import neoe.sc2.link.Link;
import neoe.sc2.link.Setting;
import neoe.util.FileUtil;
import neoe.util.Log;

/** test on Tool assisted human play */
public class Sc2Replay {

	static boolean realtime = false;

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

	public static void main(String[] args) throws Exception {
		FileUtil.save("".getBytes(), "log-neoe.log");

		Setting setting = new Setting();
		setting.isReplay = true;
		setting.gameDir = args[0];
		String replayName = args[1];
		if (args.length > 2) {
			setting.gameVer = args[2];
		} else {
			setting.gameVer = getLatestVer(setting.gameDir);
		}

		Sc2Replay client = new Sc2Replay(setting);
		client.run(setting, replayName);

	}

	private Bot bot1;

	private Link link;

	Random rand = new Random();

	private Setting setting;

	public Sc2Replay(Setting setting) {
		this.setting = setting;
	}

	private void run(Setting setting, String replayName) throws Exception {

		U.startSC2(setting);
		U.waitSC2Ready(setting.host, setting.port);
		link = new Link();
		link.websocketConnect(setting, new Thread() {
			@Override
			public void run() {
				startReplayThread(replayName);
			}
		});

	}

	private void startReplayThread(String replayName) {

		Log.log("replayThread start.");
		try {
			link.sendReq(Request.newBuilder().setPing(RequestPing.newBuilder()).build(), null, null);
			bot1 = new MyReplayViewer("viewer", setting, link);

			{// start replay
				SC2APIProtocol.Sc2Api.InterfaceOptions.Builder opt = InterfaceOptions.newBuilder().setRaw(true)
						.setScore(true);
				link.sendReq(Request
						.newBuilder().setStartReplay(RequestStartReplay.newBuilder().setReplayPath(replayName)
								.setRealtime(realtime).setOptions(opt).setDisableFog(true).setObservedPlayerId(1))
						.build(), new Handle() {
							@Override
							public void run(Response resp) {
								System.out.println(U.json(resp));
								synchronized (link) {
									link.notify();
								}
							}
						}, new Handle() {

							@Override
							public void run(Response resp) {
								System.out.println(U.json(resp));
								link.gameEnd = true;
								synchronized (link) {
									link.notify();
								}
							}
						});

			}
			synchronized (link) {
				link.wait();
			}
			if (link.gameEnd)
				return;
			Handle toBot = new Handle() {
				@Override
				public void run(Response resp) {
					try {
						bot1.onResponse(resp);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			link.sendReq(Request.newBuilder().setData(RequestData.newBuilder().setAbilityId(true).setUnitTypeId(true)
					.setUpgradeId(true).setBuffId(true).setEffectId(true)).build(), toBot, null);
			link.sendReq(Request.newBuilder().setGameInfo(RequestGameInfo.newBuilder()).build(), toBot, null);

			// listenThread.start();
//			Delay delay = new Delay();
			final Object sth = new Object();
			while (true) {
				link.sendReq(Request.newBuilder().setStep(RequestStep.newBuilder()).build(), new Handle() {
					@Override
					public void run(Response resp) {
						synchronized (sth) {
							sth.notify();
						}
					}
				}, new Handle() {
					@Override
					public void run(Response resp) {
						synchronized (sth) {
							sth.notify();
						}
					}
				});
				synchronized (sth) {
					sth.wait();
				}
				link.sendReq(Request.newBuilder()
						.setObservation(RequestObservation.newBuilder().setDisableFog(true) ).build(), toBot,
						null);
				if (link.gameEnd)
					break;

			}
			Log.log("replayThread cleanup");
			for (Object o : bot1.th) {
				((Thread) o).interrupt();
			}
		} catch (Throwable e) {
			Log.log("[gameBad]", e);
		}
		Log.log("replayThread end.");

	}

}
