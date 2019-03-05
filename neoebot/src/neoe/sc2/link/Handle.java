package neoe.sc2.link;

import SC2APIProtocol.Sc2Api.Response;

public interface Handle {
	void run(Response resp);
}
