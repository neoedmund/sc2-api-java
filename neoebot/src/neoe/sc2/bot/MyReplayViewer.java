package neoe.sc2.bot;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import SC2APIProtocol.Data.UnitTypeData;
import SC2APIProtocol.Data.Weapon;
import SC2APIProtocol.Data.Weapon.TargetType;
import SC2APIProtocol.Raw.Alliance;
import SC2APIProtocol.Raw.Unit;
import SC2APIProtocol.Sc2Api.Action;
import SC2APIProtocol.Sc2Api.ChatReceived;
import SC2APIProtocol.Sc2Api.Response;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import SC2APIProtocol.Sc2Api.Status;
import neoe.sc2.link.Link;
import neoe.sc2.link.Setting;
import neoe.util.Log;

public class MyReplayViewer extends Bot {

	private boolean inited;
	private Status lastStatus;
	private String name;
	private String lastS1;

	public MyReplayViewer(String name, Setting setting, Link link) {
		this.name = name;
		this.link = link;
	}

	private void doInit() {
		if (inited)
			return;
		inited = true;
	}

	@Override
	public void onResponse(Response resp) throws Exception {
		this.resp = resp;
		Status st = resp.getStatus();
		if (!st.equals(lastStatus)) {
			Log.log(String.format("game status changed from %s to %s", lastStatus, st));
			lastStatus = resp.getStatus();
		}
		boolean ingame = false;
		if (st != null) {
			ingame = st.equals(Status.in_replay);
		}

		switch (resp.getResponseCase()) {
		case GAME_INFO: {
			readGameInfo();
			return;
		}
		case DATA: {
			readDATA();
			return;
		}
		}

		if (ingame) {
			turn();
		}

		// output.add(Request.newBuilder().setStep(RequestStep.newBuilder().setCount(1)).build());

	}

	private void turn() throws Exception {

		if (!inited) {
			doInit();
		}
		switch (resp.getResponseCase()) {
		case OBSERVATION: {
			ResponseObservation ob = resp.getObservation();
			if (ob.getPlayerResultCount() > 0) {
				Log.log("Game over, someone win");
				U.sleep(2000);
				link.gameEnd = true;
				return;
			}

			if (ob.hasObservation()) {
				this.ob = ob;
				debugSelected();
			}

			{
				int cnt = ob.getChatCount();
				if (cnt > 0) {
					for (int i = 0; i < cnt; i++) {
						ChatReceived chat = ob.getChat(i);
						Log.log(String.format("[ob-chat]%s", U.json(chat)));
					}
				}
			}
			{
				int cnt = ob.getActionsCount();
				if (cnt > 0) {
					for (int i = 0; i < cnt; i++) {
						Action act = ob.getActions(i);
						if (act.getActionRaw().hasCameraMove()) {
							// no log
						} else {
							Log.log(String.format("[ob-act]%s", U.json(act)));
						}
					}
				}
			}
			debugDAStat();
			break;
		}

		default: {
			Log.log("[NotHandled]" + U.json(resp));
		}
		}
	}

	/** defence/attack stat, TODO */
	private void debugDAStat() {
		List<Unit> us = ob.getObservation().getRawData().getUnitsList();
		StringBuilder sb = new StringBuilder();
		sb.append(stat("Self",
				us.stream().filter(u -> u.getAlliance().equals(Alliance.Self)).collect(Collectors.toList())));
		sb.append("\n\t");
		sb.append(stat("Enemy",
				us.stream().filter(u -> u.getAlliance().equals(Alliance.Enemy)).collect(Collectors.toList())));
		sb.append("\n\t");
		sb.append(stat("Ally",
				us.stream().filter(u -> u.getAlliance().equals(Alliance.Ally)).collect(Collectors.toList())));
		String s = sb.toString();
		if (!s.equals(lastS1)) {
			Log.log(s);
			lastS1 = s;
		}

	}

	/**
	 * not perfect , consider VikingAssault and VikingFighter(unitAlias)
	 */
	private String stat(String name, List<Unit> us) {
		// A-D, Air-Ground, Attr(1-8)

		Map<Object, List<Unit>> g = us.stream().collect(Collectors.groupingBy(u -> u.getUnitType()));
		float[] da = new float[1], dg = new float[1], aa = new float[1], ag = new float[1];
		for (Object o : g.keySet()) {

			int ut = (int) o;
			int cnt = g.get(o).size();
			Unit unit = g.get(o).get(0);
			UnitTypeData u = idMap.get(ut);
			boolean air = unit.getIsFlying();
			float v = cnt * (unit.getHealthMax() + unit.getShieldMax());
			if (air) {
				da[0] += v;
			} else {
				dg[0] += v;
			}
			u.getWeaponsList().forEach(w -> addWeapon(cnt, w, aa, ag));
		}
		String s = String.format("[AGstat]%s Def:%,d/%,d, ATT:%,d/%,d", name, (int) da[0], (int) dg[0], (int) aa[0],
				(int) ag[0]);
		return s;
	}

	private void addWeapon(int cnt, Weapon w, float[] aa, float[] ag) {
		boolean air = w.getType().equals(TargetType.Air) || w.getType().equals(TargetType.Any);
		boolean ground = w.getType().equals(TargetType.Ground) || w.getType().equals(TargetType.Any);
		float v = cnt * (w.getDamage() * w.getAttacks() / w.getSpeed());
		if (air)
			aa[0] += v;
		if (ground)
			ag[0] += v;
	}
}
