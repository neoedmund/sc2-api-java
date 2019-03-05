package neoe.sc2.bot;

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

			break;
		}

		default:

		{
			Log.log("[NotHandled]" + U.json(resp));
		}
		}
	}

}
