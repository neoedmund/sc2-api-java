package neoe.sc2.bot;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import SC2APIProtocol.Sc2Api.Action;
import SC2APIProtocol.Sc2Api.ActionChat;
import SC2APIProtocol.Sc2Api.ActionChat.Channel;
import SC2APIProtocol.Sc2Api.ChatReceived;
import SC2APIProtocol.Sc2Api.Observation;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestAction;
import SC2APIProtocol.Sc2Api.RequestData;
import SC2APIProtocol.Sc2Api.RequestGameInfo;
import SC2APIProtocol.Sc2Api.Response;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import SC2APIProtocol.Sc2Api.Status;
import neoe.sc2.link.IBot;
import neoe.sc2.link.Setting;
import neoe.sc2.link.U;
import neoe.util.Log;

public class MyZergBot implements IBot {

	private String name;
	private Setting setting;
	private boolean inited;

	public MyZergBot(String name, Setting setting) {
		this.name = name;
		this.setting = setting;
		inited = false;
	}

	private Response resp;
	private Status lastStatus;
	private LinkedBlockingQueue<Request> output = new LinkedBlockingQueue<Request>();

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

		if (ingame) {
			run();
		} else {
			// learn(rob);
		}

		// output.add(Request.newBuilder().setStep(RequestStep.newBuilder().setCount(1)).build());

	}

	private void rebootGame() {

	}

	private void run() throws Exception {

		if (!inited) {
			doInit();
		}

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
						U.toJson(tob.getPlayerCommon()), tob.getAlertsCount(), U.toJson(tob)));
			}
		}

	}

	private void doInit() {
		if (inited)
			return;
		inited = true;
		output.add(Request.newBuilder().setGameInfo(RequestGameInfo.newBuilder()).build());
		output.add(Request.newBuilder().setData(RequestData.newBuilder()).build());
		output.add(Request.newBuilder()
				.setAction(RequestAction.newBuilder()
						.addActions(Action.newBuilder().setActionChat(ActionChat.newBuilder()
								.setChannel(Channel.Broadcast).setMessage(String.format("I'm '%s', glhf!", name)))))
				.build());
	}

	@Override
	public void pullRequsts(Collection<Request> to) throws Exception {
		output.drainTo(to);
	}

}
