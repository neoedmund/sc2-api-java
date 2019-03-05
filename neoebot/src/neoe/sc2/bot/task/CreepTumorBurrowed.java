package neoe.sc2.bot.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import SC2APIProtocol.Common.ImageData;
import SC2APIProtocol.Common.Point;
import SC2APIProtocol.Common.Point2D;
import SC2APIProtocol.Common.Point2D.Builder;
import SC2APIProtocol.Raw.ActionRawUnitCommand;
import SC2APIProtocol.Raw.Alliance;
import SC2APIProtocol.Raw.Unit;
import neoe.sc2.bot.Bot;
import neoe.sc2.bot.U;
import neoe.util.Log;

/** not work yet */
public class CreepTumorBurrowed extends Task {

	public static Set<Long> done_CreepTumorBurrowed = new HashSet<>();

	float R = 9;

	public CreepTumorBurrowed(Bot bot) {
		super(bot);
	}

	private Point2D findGoodPos(Unit src) {
		int maxv = 0;
		Builder p = null;
		Point p0 = src.getPos();
		for (float ax = -R; ax <= R; ax++) {
			float ay = (float) Math.sqrt(100 - ax * ax);
			{
				float x, y;
				int v = testValue(x = p0.getX() + ax, y = p0.getY() + ay);
				if (v > maxv) {
					maxv = v;
					p = Point2D.newBuilder().setX(x).setY(y);
				}
			}
		}
		return p == null ? null : p.build();
	}

	private int getCell(ImageData creep, int w, int h, int x, int y) {
		if (x < 0 || x >= w || y < 0 || y >= h)
			return -1;
		return creep.getData().byteAt(w * y + x);
	}

	@Override
	public void logic() throws Exception {
		while (true) {
			U.nap200();
			if (bot.link.gameEnd)
				return;

			List<Unit> us = bot.getAllUnits();
			// "unitId": 137, "name": "CreepTumorBurrowed", "abilityId": 1662,
			List<Unit> srcs = bot.findUnitsByUnitid(us, 137, Alliance.Self);

			srcs.removeIf(u -> done_CreepTumorBurrowed.contains(u.getTag()));
			if (srcs.isEmpty())
				continue;

			for (Unit src : srcs) {
				if (src.getOrdersCount() > 0) {
					done_CreepTumorBurrowed.add(src.getTag());
					continue;
				}
				Point2D p2 = findGoodPos(src);
				if (p2 != null) {
					bot.command(ActionRawUnitCommand.newBuilder().setAbilityId(1662).setTargetWorldSpacePos(p2)
							.addUnitTags(src.getTag()).build());
				} else {
					Log.log("CreepTumorBurrowed cannot find pos.");
				}
			}

		}

	}

	private int testValue(float x0, float y0) {
		ImageData creep = bot.ob.getObservation().getRawData().getMapState().getCreep();
		int w = creep.getSize().getX();
		int h = creep.getSize().getY();
		int x = (int) x0;
		int y = (int) (h - 1 - y0);
		if (getCell(creep, w, h, x, y) == 0) {
			return 0;
		}
		int v = 0;
		for (int ax = (int) -R; ax <= R; ax++) {
			int ay0 = (int) Math.sqrt(100 - ax * ax);
			for (int ay = -ay0; ay <= ay0; ay++) {
				if (getCell(creep, w, h, x, y) == 0) {
					v++;
				}
			}
		}
		return v;
	}

}
