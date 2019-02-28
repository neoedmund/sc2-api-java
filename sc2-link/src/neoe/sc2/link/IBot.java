package neoe.sc2.link;

import java.util.Collection;

import SC2APIProtocol.Sc2Api.Request;
import SC2APIProtocol.Sc2Api.Response;

public interface IBot {

	void pullRequsts(Collection<Request> to) throws Exception;

	void onResponse(Response resp) throws Exception;

}
