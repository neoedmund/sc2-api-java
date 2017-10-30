package neoe.sc2.bot;

import SC2APIProtocol.Common.Race;
import SC2APIProtocol.Sc2Api.PlayerType;
import neoe.sc2.link.Sc2Client;
import neoe.sc2.link.Sc2Client.Setting;
import neoe.util.Log;

public class MainReplay {

	public static void main(String[] args) throws Exception {
		Log.stdout = true;
		Setting setting = new Setting();
		setting.gameDir = "D:/sc2/StarCraft II/";
		setting.port = 5000;
		setting.host = "127.0.0.1";
		setting.realtime = false;
		setting.isReplay = true;
		setting.gameVer = "Base55958";
		setting.dataVersion = "5BD7C31B44525DAB46E64C4602A81DC2";
		setting.fog = true;
		Sc2Client client = new Sc2Client(setting);
		Sc2Client.startSC2Windows(setting);
		Sc2Client.waitSC2Ready(setting);
		String[] maps = new String[] {
				"E:/11h/3.16.1-Pack_1-fix/Replays/0000e057beefc9b1e9da959ed921b24b9f0a31c63fedb8d94a1db78b58cf92c5.SC2Replay", //
				// "E:/11h/3.16.1-Pack_1-fix/Replays/000a4ab29a10c7db1e2e7d0dcde9aad01fb297a703417c03e4a5137c0fb2af0d.SC2Replay",
				// //
		};
		// Replay is from a different version. Relaunching client into the correct
		// version..
		for (String map : maps) {
			client.setConfig(map, new Object[][] { //
					{ PlayerType.Observer, Race.Zerg, null, new MyZergBot("neoe", setting) }, //
			});
			while (true) {
				client.websocketConnect();
				client.waitFinish();
				if (client.realEnd) {
					break;
				}
				client.rejoin = true;
			}
		}
	}

}
