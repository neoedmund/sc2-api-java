package neoe.sc2.bot.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import SC2APIProtocol.Raw.ActionRawUnitCommand;
import SC2APIProtocol.Raw.Alliance;
import SC2APIProtocol.Raw.Unit;
import SC2APIProtocol.Sc2Api.PlayerCommon;
import neoe.sc2.bot.Bot;
import neoe.sc2.bot.U;

public class QueenSpawnLarvaTask extends Task {

	protected static final float CastEnergy = 25;

	public QueenSpawnLarvaTask(Bot bot) {
		super(bot);
	}

	@Override
	public void logic() throws Exception {
		while (true) {
			U.nap200();
			if (bot.link.gameEnd)
				return;
			if (!bot.ob.hasObservation())
				continue;
			List<Unit> us = bot.getAllUnits();

			List<Unit> bases = bot.findUnitsByUnitNames(us, new String[] { "Hive", "Lair", "Hatchery" }, Alliance.Self);
			bases.removeIf(u -> u.getBuffIdsList().contains(11));
			if (bases.isEmpty())
				continue;
			List<Unit> queens = bot.findUnitsByUnitid(us, bot.getUnit("Queen").getUnitId(), Alliance.Self);

			if (queens.size() < bases.size()) { // need build queen
				List<Unit> spawningPools = bot.findUnitsByUnitid(us, bot.getUnit("SpawningPool").getUnitId(),
						Alliance.Self);
				spawningPools.removeIf(u -> u.getBuildProgress() < 1);
				if (spawningPools.size() > 0) {
					// try build queen
					for (Unit base : bases) {
						if (base.getOrdersCount() <= 0) {
							PlayerCommon pc = bot.ob.getObservation().getPlayerCommon();
							if (pc.getMinerals() >= 150 && pc.getFoodUsed() + 2 <= pc.getFoodCap()) {
								bot.command(ActionRawUnitCommand.newBuilder().setAbilityId(1632)
										.addUnitTags(base.getTag()).build());
								break;
							}
						}
					}
				}
			}
			{// remove working on bases
				Set tags = new HashSet<>();
				queens.stream().forEach(u -> bot.addTargets(tags, u.getOrdersList()));
				bases.removeIf(u -> tags.contains(u.getTag()));
				if (bases.isEmpty())
					continue;
			}
			queens.removeIf(u -> (u.getEnergy() < CastEnergy || u.getOrdersCount() > 0));

			if (queens.isEmpty())
				continue;
			for (Unit base : bases) {
				Unit queen = bot.getNearest(base, queens);
				if (queen != null) {
					bot.command(ActionRawUnitCommand.newBuilder().setAbilityId(251).setTargetUnitTag(base.getTag())
							.addUnitTags(queen.getTag()).build());
					queens.remove(queen);
				} else {
					break;
				}
			}
		}

	}

}
