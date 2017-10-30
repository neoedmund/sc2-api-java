package neoe.sc2.bot;

import java.util.Collection;

import SC2APIProtocol.Debug.DebugChat;
import SC2APIProtocol.Debug.DebugCommand;
import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.RequestDebug;
import SC2APIProtocol.Sc2Api.RequestStep;
import SC2APIProtocol.Sc2Api.ResponseObservation;
import neoe.sc2.link.IBot;
import neoe.sc2.link.Sc2Client.Setting;

public class MyZergBot implements IBot {

	public class Commander {
		private Collection<Request> output;
		private ResponseObservation rob;

		public Commander(ResponseObservation rob, Collection<Request> output) {
			this.rob = rob;
			this.output = output;
		}

		public void run() {
			// TODO Auto-generated method stub

		}

	}

	private String name;
	private Setting setting;

	public MyZergBot(String name, Setting setting) {
		this.name = name;
		this.setting = setting;
	}

	int frame;

	@Override
	public void onObservation(ResponseObservation rob, Collection<Request> output) throws Exception {
		// DO things
		frame++;
		output.add(Request.newBuilder()
				.setDebug(RequestDebug.newBuilder().addDebug(
						DebugCommand.newBuilder().setChat(DebugChat.newBuilder().setMessage("frame:" + frame))))
				.build());
		if (!setting.isReplay) {
			new Commander(rob, output).run();
		} else {
			learn(rob);
		}

		output.add(Request.newBuilder().setStep(RequestStep.newBuilder().setCount(1)).build());

	}

	private void learn(ResponseObservation rob) {
		// TODO Auto-generated method stub
	}
}
