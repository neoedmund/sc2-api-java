package neoe.sc2.bot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import SC2APIProtocol.Data.UnitTypeData;
import SC2APIProtocol.Raw.ActionRaw;
import SC2APIProtocol.Raw.ActionRawUnitCommand;
import SC2APIProtocol.Raw.Alliance;
import SC2APIProtocol.Raw.Unit;
import SC2APIProtocol.Raw.UnitOrder;
import SC2APIProtocol.Sc2Api.Action;
import SC2APIProtocol.Sc2Api.ActionChat;
import SC2APIProtocol.Sc2Api.ActionChat.Channel;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestAction;
import SC2APIProtocol.Sc2Api.RequestSaveReplay;
import SC2APIProtocol.Sc2Api.Response;
import SC2APIProtocol.Sc2Api.ResponseData;
import SC2APIProtocol.Sc2Api.ResponseGameInfo;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import neoe.sc2.link.Handle;
import neoe.sc2.link.Link;
import neoe.util.FileUtil;
import neoe.util.Log;

public abstract class Bot {

	public ResponseData data;

	protected Set<Long> debugSeletedHis = new HashSet<>();
	public ResponseGameInfo gameinfo;
	public Map<Integer, UnitTypeData> idMap;
	public Link link;

	public Map<String, UnitTypeData> nameMap;

	public ResponseObservation ob;
	int passFrame;

	public Response resp;

	public Collection th = Collections.synchronizedSet(new HashSet());

	public void addTargets(Set tags, List<UnitOrder> ordersList) {
		ordersList.stream().forEach(o -> tags.add(o.getTargetUnitTag()));
	}

	public void command(ActionRawUnitCommand build) {
		link.sendReq2(Request.newBuilder()
				.setAction(RequestAction.newBuilder()
						.addActions(Action.newBuilder().setActionRaw(ActionRaw.newBuilder().setUnitCommand(build))))
				.build());

	}

	public void debugSelected() {
		for (Unit u : getAllUnits()) {
			long tag = u.getTag();
			if (u.getIsSelected()) {
				if (debugSeletedHis.contains(tag)) {
					// no more
				} else {
					debugSeletedHis.add(tag);
					Log.log("[selected]" + U.json(u));
				}
			} else {
				if (debugSeletedHis.contains(tag)) {
					debugSeletedHis.remove(tag);// for later select
				}
			}
		}
	}

	public List<Unit> findUnitsByUnitid(List<Unit> units, int unitId, Alliance alliance) {
		List<Unit> res = new ArrayList<>();
		for (Unit u : units) {
			if (alliance != null) {
				if (!u.getAlliance().equals(alliance))
					continue;
			}
			if (u.getUnitType() == unitId) {
				res.add(u);
			}
		}
		return res;
	}

	public List<Unit> findUnitsByUnitNames(List<Unit> units, String[] names, Alliance alliance) {
		List<Unit> res = new ArrayList<>();
		List ids = new ArrayList();
		for (String name : names) {
			ids.add(getUnit(name).getUnitId());
		}
		for (Unit u : units) {
			if (alliance != null) {
				if (!u.getAlliance().equals(alliance))
					continue;
			}
			if (ids.contains(u.getUnitType())) {
				res.add(u);
			}
		}
		return res;
	}

	public List<Unit> getAllUnits() {
		return ob.getObservation().getRawData().getUnitsList();
	}

	public ResponseData getData() {
		return data;
	}

	public ResponseGameInfo getGameinfo() {
		return gameinfo;
	}

	public Unit getNearest(Unit a, List<Unit> bs) {
		float min = 100000;
		Unit target = null;
		for (Unit b : bs) {
			float dist = U.distanceSimple(a.getPos(), b.getPos());
			if (dist < min) {
				min = dist;
				target = b;
			}
		}
		return target;
	}

	public UnitTypeData getUnit(String name) {
		UnitTypeData unit = nameMap.get(name);
		if (unit == null) {
			U.err("cannot get unit named " + name);
		}
		return unit;

	}

	public boolean isNewFrame() {
		return ob.getObservation().getGameLoop() > passFrame;
	}

	public void nextFrame() {
		passFrame = ob.getObservation().getGameLoop();
	}

	public abstract void onResponse(Response resp) throws Exception;

	protected void readDATA() {
		this.data = resp.getData();
		List<UnitTypeData> units = data.getUnitsList();
		nameMap = new HashMap<>();
		idMap = new HashMap<>();
		for (UnitTypeData unit : units) {
			nameMap.put(unit.getName(), unit);
			idMap.put(unit.getUnitId(), unit);
		}
		Log.log("got DATA");

	}

	protected void readGameInfo() throws IOException {
		this.gameinfo = resp.getGameInfo();
		Log.log("got gameinfo");
		if (false) {
			U.dumpMap(gameinfo.getMapName(), "PathingGrid", gameinfo.getStartRaw().getPathingGrid());
			U.dumpMap(gameinfo.getMapName(), "PlacementGrid", gameinfo.getStartRaw().getPlacementGrid());
			U.dumpMap(gameinfo.getMapName(), "TerrainHeight", gameinfo.getStartRaw().getTerrainHeight());
		}
	}

	public void saveReplay() {
		saveReplay(null);
	}

	public void saveReplay(String tag) {
		link.sendReq(Request.newBuilder().setSaveReplay(RequestSaveReplay.newBuilder()).build(), new Handle() {

			@Override
			public void run(Response resp) {
				byte[] bs = resp.getSaveReplay().getData().toByteArray();
				try {
					String fn = new SimpleDateFormat("yyyyMMddHHssmm").format(new Date()) + (tag == null ? "" : tag)
							+ ".SC2Replay";
					FileUtil.save(bs, fn);
					say("saved " + fn);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, null);

	}

	public void say(String s) {
		String msg = String.format("[say](%s)%s", ob == null ? "-1" : ob.getObservation().getGameLoop(), s);
		System.out.println(msg);
		Log.getLog("long").log0(msg);
		Log.log(msg);
		link.sendReq(Request.newBuilder()
				.setAction(RequestAction.newBuilder()
						.addActions(Action.newBuilder()
								.setActionChat(ActionChat.newBuilder().setChannel(Channel.Team).setMessage(s))))
				.build(), null, null);
	}

}
