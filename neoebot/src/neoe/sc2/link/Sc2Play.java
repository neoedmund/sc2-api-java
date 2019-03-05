package neoe.sc2.link;

import java.io.File;
import java.util.Random;

import SC2APIProtocol.Common.Race;
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
import neoe.sc2.bot.Bot;
import neoe.sc2.bot.C;
import neoe.sc2.bot.MyZergBot;
import neoe.sc2.bot.U;
import neoe.util.FileUtil;
import neoe.util.Log;

/** test on Tool assisted human play */
public class Sc2Play {

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
		setting.gameDir = args[0];
		if (args.length > 1) {
			setting.gameVer = args[1];
		} else {
			setting.gameVer = getLatestVer(setting.gameDir);
		}
		Sc2Play client = new Sc2Play(setting);
		client.run(setting);

	}

	private Bot bot1;

	private Link link;

	Random rand = new Random();

	private Setting setting;

	public Sc2Play(Setting setting) {
		this.setting = setting;
	}

	private void run(Setting setting) throws Exception {

		U.startSC2(setting);
		U.waitSC2Ready(setting.host, setting.port);
		link = new Link();
		link.websocketConnect(setting, new Thread() {
			@Override
			public void run() {
				startGameThread();
			}
		});

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

		}
		{
			SC2APIProtocol.Sc2Api.PlayerSetup.Builder ps = PlayerSetup.newBuilder();
			ps.setType(PlayerType.Computer).setRace(Race.Random).setDifficulty(C.DIFFICULTY);
			requestCreateGame.addPlayerSetup(ps.build());
		}

		//
		joinGame.setRace(Race.Zerg).setOptions(InterfaceOptions.newBuilder().setRaw(true).setScore(true).build());

		//
		link.sendReq(Request.newBuilder().setCreateGame(requestCreateGame).build(), null, null);
		link.sendReq(Request.newBuilder().setJoinGame(joinGame).build(), null, null);

	}

	private void startGameThread() {

		Log.log("gameThread start.");
		try {
			link.sendReq(Request.newBuilder().setPing(RequestPing.newBuilder()).build(), null, null);
			bot1 = new MyZergBot("brood", setting, link);
			String map = C._MAPS[rand.nextInt(C._MAPS.length)];
			System.out.println("map:" + map);
			Log.getLog("long").log0("map:" + map);
			startGame(map);
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
			while (true) {
				link.sendReq(Request.newBuilder().setObservation(RequestObservation.newBuilder()).build(), toBot, null);
				U.sleep(90);
				if (link.gameEnd)
					break;
			}
			Log.log("gameThread cleanup");
			for (Object o : bot1.th) {
				((Thread) o).interrupt();
			}
		} catch (Throwable e) {
			Log.log("[gameBad]", e);
		}
		Log.log("gameThread end.");

	}

}
