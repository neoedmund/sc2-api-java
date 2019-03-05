package neoe.sc2.bot.task;

import java.util.List;

import SC2APIProtocol.Data.UnitTypeData;
import SC2APIProtocol.Raw.ActionRawUnitCommand;
import SC2APIProtocol.Raw.Alliance;
import SC2APIProtocol.Raw.Unit;
import SC2APIProtocol.Raw.UnitOrder;
import SC2APIProtocol.Sc2Api.PlayerCommon;
import neoe.sc2.bot.Bot;
import neoe.sc2.bot.U;

public class BalanceDrones extends Task {

	private static final int GOAL_DRONE_1 = 44;
	PlayerCommon pc;

	private Task prevTask;

	List<Unit> us;

	public BalanceDrones(Bot bot, Task t1) {
		super(bot);
		this.prevTask = t1;
	}

	private void buildADrone() {
		List<Unit> larvas = bot.findUnitsByUnitid(us, bot.getUnit("Larva").getUnitId(), Alliance.Self);
		if (!larvas.isEmpty()) {
			bot.command(
					ActionRawUnitCommand.newBuilder().setAbilityId(1342).addUnitTags(larvas.get(0).getTag()).build());
		}

	}

	private void buildAFood() {
		List<Unit> larvas = bot.findUnitsByUnitid(us, bot.getUnit("Larva").getUnitId(), Alliance.Self);
		if (!larvas.isEmpty()) {
			bot.command(
					ActionRawUnitCommand.newBuilder().setAbilityId(1344).addUnitTags(larvas.get(0).getTag()).build());
		}
	}

	private int foodInBuilding() {
		int s = 0;
		List<Unit> eggs = bot.findUnitsByUnitid(us, bot.getUnit("Egg").getUnitId(), Alliance.Self);
		for (Unit u : eggs) {
			for (UnitOrder ord : u.getOrdersList()) {
				if (ord.getAbilityId() == 1344) {
					s += 8;
				}
			}
		}
		return s;
	}

	@Override
	public void logic() throws Exception {
		if (prevTask != null)
			prevTask.waitDone();
		while (true) {
			U.nap();
			if (bot.link.gameEnd) {
				return;
			}
			if (!bot.isNewFrame()) {
				continue;
			}
			us = bot.getAllUnits();
			pc = bot.ob.getObservation().getPlayerCommon();
			{
				if (pc.getFoodCap() >= 200)
					continue;
				if (pc.getFoodUsed() + 2 >= pc.getFoodCap() + foodInBuilding()) {
					if (pc.getMinerals() >= 100) {
						buildAFood();
						bot.nextFrame();
						continue;
					}
				}
			}
			{
				UnitTypeData unittype = bot.getUnit("Drone");
				List<Unit> unitsTarget = bot.findUnitsByUnitid(us, unittype.getUnitId(), Alliance.Self);
				if (unitsTarget.size() < GOAL_DRONE_1) {
					if (pc.getMinerals() >= 50 && (pc.getFoodCap() > pc.getFoodUsed())) {
						buildADrone();
						bot.nextFrame();
						continue;
					}
				} else {
					bot.say("built enough drones");
					bot.saveReplay("mid");
					break;
				}
			}
		}

	}

}
