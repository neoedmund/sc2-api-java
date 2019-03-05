package neoe.sc2.bot.task;

import SC2APIProtocol.Sc2Api.Observation;
import neoe.sc2.bot.Bot;
import neoe.sc2.bot.U;
import neoe.util.Log;

public class DebugStat extends Task {

	public DebugStat(Bot bot) {
		super(bot);
	}

	@Override
	public void logic() throws Exception {
		while (true) {
			U.sleep(5000);
			if (bot.link.gameEnd)
				return;
			Observation tob = bot.ob.getObservation();
			Log.log(String.format("[stats]%s\n\t%s\n\t%s", U.json(tob.getPlayerCommon()), U.json(tob.getScore()),
					U.debugUnitCount(bot, tob.getRawData().getUnitsList())));
		}

	}

}
