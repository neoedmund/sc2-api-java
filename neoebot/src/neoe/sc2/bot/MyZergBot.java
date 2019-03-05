package neoe.sc2.bot;

import SC2APIProtocol.Sc2Api.Action;
import SC2APIProtocol.Sc2Api.ChatReceived;
import SC2APIProtocol.Sc2Api.Observation;
import SC2APIProtocol.Sc2Api.Response;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import SC2APIProtocol.Sc2Api.Status;
import neoe.sc2.bot.task.BalanceDrones;
import neoe.sc2.bot.task.CreepTumorBurrowed;
import neoe.sc2.bot.task.DebugStat;
import neoe.sc2.bot.task.QueenSpawnLarvaTask;
import neoe.sc2.bot.task.SpawningPoolTask;
import neoe.sc2.bot.task.Task;
import neoe.sc2.link.Link;
import neoe.sc2.link.Setting;
import neoe.util.Log;

public class MyZergBot extends Bot {

	private boolean inited;
	private Status lastStatus;

	private String name;

	boolean obPrinted = false;

	private Setting setting;

	public MyZergBot(String name, Setting setting, Link link) {
		this.name = name;
		this.setting = setting;
		this.link = link;
		inited = false;
	}

	private void doInit() throws Exception {
		if (inited)
			return;
		inited = true;

		say(String.format("This is '%s', glhf!", name));

		SpawningPoolTask t1 = new SpawningPoolTask(this);
		t1.start();
		{// make drones
			Task t2 = new BalanceDrones(this, t1);
			t2.start();
		}
		{
			Task t3 = new QueenSpawnLarvaTask(this);
			t3.start();
		}
		{

			new CreepTumorBurrowed(this).start();

		}
		{

			new DebugStat(this).start();

		}

	}

	@Override
	public void onResponse(Response rob) throws Exception {
		// DO things

		this.resp = rob;

		Status st = resp.getStatus();
		if (!st.equals(lastStatus)) {
			Log.log(String.format("game status changed from %s to %s", lastStatus, st));
			lastStatus = resp.getStatus();
		}
		boolean ingame = false;
		if (st != null) {
			ingame = st.equals(Status.in_game);
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

	}

	private void turn() throws Exception {

		if (!inited) {
			doInit();
		}
		switch (resp.getResponseCase()) {
		case OBSERVATION: {
			ResponseObservation ob = resp.getObservation();
			if (ob.getPlayerResultCount() > 0) {
				say("Game over, someone win");
				saveReplay();
				U.sleep(2000);
				link.gameEnd = true;
				return;
			}
			if (ob.hasObservation()) {
				this.ob = ob;
//				Log.log("[new OB]");

				if (!obPrinted) {
					Observation tob = ob.getObservation();
					obPrinted = true;
					Log.log(String.format("[obs]gameloop:%s, all:%s, ", tob.getGameLoop(), U.json(tob)));
					U.dumpMap("ob", "vis", tob.getRawData().getMapState().getVisibility());
					U.dumpMap("ob", "creep", tob.getRawData().getMapState().getCreep());
				}
				debugSelected();
			}

			{
				int cnt = ob.getChatCount();
				if (cnt > 0) {
					for (int i = 0; i < cnt; i++) {
						ChatReceived chat = ob.getChat(i);
						Log.log(String.format("[ob-chat]%s", U.json(chat)));
						if (chat.getMessage().equalsIgnoreCase("gg")) {
							saveReplay();

						}
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

			break;
		}

		default:

		{
			Log.log("[NotHandled]" + U.json(resp));
		}
		}
	}

}
