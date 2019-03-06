package neoe.sc2.bot;

import SC2APIProtocol.Sc2Api.Difficulty;

public interface C {
	String[] _MAPS = { "CyberForestLE", "AutomatonLE", "KairosJunctionLE", "KingsCoveLE", "NewRepugnancyLE",
			"PortAleksanderLE", "YearZeroLE" };
	boolean debugOut = false;
	Difficulty DIFFICULTY = Difficulty.VeryHard;
	String urlPath = "/sc2api";
}
