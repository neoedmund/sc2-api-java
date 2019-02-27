package neoe.sc2.bot;

import SC2APIProtocol.Common.Race;
import SC2APIProtocol.Sc2Api.Difficulty;
import SC2APIProtocol.Sc2Api.PlayerType;
import neoe.sc2.link.Sc2Client;
import neoe.sc2.link.Setting;
import neoe.util.Log;

public class MainPlay {

	public static void main(String[] args) throws Exception {
		Log.stdout = true;
		Setting setting = new Setting();
		setting.gameDir = "D:/sc2/StarCraft II/";
		setting.port = 5000;
		setting.host = "127.0.0.1";
		setting.realtime = false;

		String map = new String[] { "battlenet:://starcraft/map/1/288793",
				"D:/sc2/StarCraft II/Maps/Ladder2017Season3/AcolyteLE.SC2Map",
				"Ladder2017Season3/AcolyteLE.SC2Map", }[2];

		 
		Sc2Client.startMatch(setting, map , new Object[][] { //
			{ PlayerType.Participant, Race.Zerg, null, new MyZergBot("neoe", setting) }, //
			{ PlayerType.Computer, Race.Random, Difficulty.Easy },//

	});
	}

}
