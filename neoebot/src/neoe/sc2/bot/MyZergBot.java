package neoe.sc2.bot;

import java.util.Collection;

import SC2APIProtocol.Sc2Api.Action;
import SC2APIProtocol.Sc2Api.ChatReceived;
import SC2APIProtocol.Sc2Api.Observation;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.Response;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import SC2APIProtocol.Sc2Api.Status;
import neoe.sc2.link.IBot;
import neoe.sc2.link.Setting;
import neoe.util.Log;

public class MyZergBot implements IBot {

	private String name;
	private Setting setting;

	public MyZergBot(String name, Setting setting) {
		this.name = name;
		this.setting = setting;
	}

	int frame;
	private Collection<Request> output;
	private Response resp;
	private Status lastStatus;

	@Override
	public void onObservation(Response rob, Collection<Request> output) throws Exception {
		// DO things

		this.resp = rob;
		this.output = output;
		frame++;

		Status st = resp.getStatus();
		if (!st.equals(lastStatus)) {
			Log.log(String.format("game status changed from %s to %s", lastStatus, st));
			lastStatus = resp.getStatus();
		}
		boolean ingame = false;
		if (st != null) {
			ingame = st.equals(Status.in_game);
		}

		if (ingame) {
			run();
		} else {
			// learn(rob);
		}

		// output.add(Request.newBuilder().setStep(RequestStep.newBuilder().setCount(1)).build());

	}

	private void rebootGame() {

	}

	private void run() {
		ResponseObservation ob = resp.getObservation();
		if (ob.getPlayerResultCount() > 0) {
			Log.log("Game over, someone win");
			rebootGame();
			return;
		}
		{
			int cnt = ob.getChatCount();
			if (cnt > 0) {
				for (int i = 0; i < cnt; i++) {
					ChatReceived chat = ob.getChat(i);
					Log.log(String.format("[chat][%s]:%s", chat.getPlayerId(), chat.getMessage()));
				}
			}
		}
		{
			int cnt = ob.getActionsCount();
			if (cnt > 0) {
				for (int i = 0; i < cnt; i++) {
					Action act = ob.getActions(i);
					Log.log(String.format("[act]%s", act));
				}
			}
		}
		{
			Observation tob = ob.getObservation();
			if (tob != null && tob.getGameLoop() < 20) {
				Log.log(String.format("[obs]gameloop:%s, PlayerCommon:%s, alerts:%s, all:%s, ", tob.getGameLoop(),
						tob.getPlayerCommon(), tob.getAlertsCount(), tob));
			}
		}

	}

}
