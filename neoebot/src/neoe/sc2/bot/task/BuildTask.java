package neoe.sc2.bot.task;

import java.util.List;

import SC2APIProtocol.Common.ImageData;
import SC2APIProtocol.Common.Point;
import SC2APIProtocol.Common.Point2D;
import SC2APIProtocol.Data.UnitTypeData;
import SC2APIProtocol.Debug.Color;
import SC2APIProtocol.Debug.DebugCommand;
import SC2APIProtocol.Debug.DebugDraw;
import SC2APIProtocol.Debug.DebugLine;
import SC2APIProtocol.Debug.Line;
import SC2APIProtocol.Raw.ActionRaw;
import SC2APIProtocol.Raw.ActionRawUnitCommand;
import SC2APIProtocol.Raw.Alliance;
import SC2APIProtocol.Raw.Unit;
import SC2APIProtocol.Sc2Api.Action;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestAction;
import SC2APIProtocol.Sc2Api.RequestDebug;
import SC2APIProtocol.Sc2Api.Response;
import neoe.sc2.bot.Bot;
import neoe.sc2.bot.U;
import neoe.sc2.link.Handle;
import neoe.util.Log;

public class BuildTask extends Task {
	private String buildingName;
	private int targetCnt;
	private UnitTypeData unit;
	private int workingCnt;

	public BuildTask(Bot bot, String buildingName, int cnt) {
		super(bot);
		this.buildingName = buildingName;
		this.targetCnt = cnt;

	}

	private Point2D findBuildPos() {
		ImageData creep = bot.ob.getObservation().getRawData().getMapState().getCreep();
		return findInMap(creep, 1);

	}

	private Point2D findInMap(ImageData data, int v) {
		int w = data.getSize().getX();
		int h = data.getSize().getY();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (data.getData().byteAt(x + y * w) == v) {
					return Point2D.newBuilder().setX(x + 3).setY(h - y - 3).build();
				}
			}
		}
		return null;

	}

	@Override
	public void logic() throws Exception {
		Log.log("Logic run");
		while (true) {
			nap();
			if (bot.nameMap == null)
				continue;
			if (bot.idMap == null)
				continue;
			break;
		}
		unit = bot.getUnit(buildingName);
		while (true) {
			nap();
			if (bot.link.gameEnd)
				break;
			if (bot.ob == null) {
				continue;
			}
			List<Unit> units = bot.getAllUnits();
			List<Unit> unitsTarget = bot.findUnitsByUnitid(units, unit.getUnitId(), Alliance.Self);
			int existsCnt = unitsTarget.size();
			if (existsCnt >= targetCnt) {
				bot.say(String.format("Task complete, build %s x %s", unit.getName(), targetCnt));
				return;
			}
			if (existsCnt + workingCnt < targetCnt) {
				tryBuild();
			}

		}

	}

	private void nap() {
		U.sleep(10);

	}

	private boolean timeToGo() {
		if (bot.ob.getObservation().getPlayerCommon().getMinerals() >= 200) {
			return true;
		}
		return false;
	}

	private Point toPoint(Point2D p2) {
		return Point.newBuilder().setX(p2.getX()).setY(p2.getY()).build();
	}

	private void tryBuild() {
		if (unit.getName().equals("SpawningPool")) {
			tryBuildTrans(bot.getUnit("Drone"), unit.getAbilityId());
		}
	}

	private void tryBuildTrans(UnitTypeData src, int abilityId) {
		List<Unit> units = bot.getAllUnits();
		List<Unit> sus = bot.findUnitsByUnitid(units, src.getUnitId(), Alliance.Self);
		if (sus.size() <= 0) {
			// no source found
			Log.log(String.format("%s for building %s not found yet", src.getName(), unit.getName()));
			U.sleep(1000);
			return;
		}
		Unit u1 = sus.get(0);
		Point p1 = u1.getPos();
		Point2D p2 = // Point2D.newBuilder().setX(124.5f).setY(29.5f).build(); //
				findBuildPos();
		if (p2 != null) {
			if (timeToGo()) {

				Request debug;
				bot.link.sendReq2(
						debug = Request.newBuilder()
								.setDebug(RequestDebug.newBuilder()
										.addDebug(DebugCommand.newBuilder().setDraw(DebugDraw.newBuilder()
												.addLines(DebugLine.newBuilder().setColor(Color.newBuilder().setR(255))
														.setLine(Line.newBuilder().setP0(p1)
																.setP1(Point.newBuilder().setX(p2.getX())
																		.setY(p2.getY()).setZ(p1.getZ()).build()))))))
								.build());
				Log.log("build at " + U.json(p2) + " by " + U.json(u1) + " abli:" + abilityId + ", debug="
						+ U.json(debug));// 1155

				bot.link.sendReq2(
						Request.newBuilder()
								.setAction(RequestAction.newBuilder()
										.addActions(Action.newBuilder().setActionRaw(ActionRaw.newBuilder()
												.setUnitCommand(ActionRawUnitCommand.newBuilder().setAbilityId(16)
														.setTargetWorldSpacePos(p2).addUnitTags(u1.getTag())))))
								.build());
				bot.link.sendReq(Request.newBuilder().setAction(RequestAction.newBuilder()
						.addActions(Action.newBuilder().setActionRaw(ActionRaw.newBuilder()
								.setUnitCommand(ActionRawUnitCommand.newBuilder().setAbilityId(abilityId)
										.setTargetWorldSpacePos(p2).addUnitTags(u1.getTag()).setQueueCommand(true)))))
						.build(), null, new Handle() {

							@Override
							public void run(Response resp) {
								Point2D p3 = Point2D.newBuilder().setX(p2.getX()).setY(p2.getY() - 2).build();
								bot.link.sendReq2(Request.newBuilder()
										.setAction(RequestAction.newBuilder()
												.addActions(Action.newBuilder().setActionRaw(ActionRaw.newBuilder()
														.setUnitCommand(ActionRawUnitCommand.newBuilder()
																.setAbilityId(abilityId).setTargetWorldSpacePos(p3)
																.addUnitTags(u1.getTag()).setQueueCommand(true)))))
										.build());

							}
						});
				workingCnt++;
			}
		}
	}
}
