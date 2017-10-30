package neoe.sc2.link;

import java.util.Collection;

import org.java_websocket.client.WebSocketClient;

import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.ResponseObservation;

public interface IBot {

	void onObservation(ResponseObservation rob, Collection<Request> output) throws Exception;

}
