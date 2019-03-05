package neoe.sc2.bot;

import SC2APIProtocol.Sc2Api.Difficulty;

public interface C {
	final String[] _MAPS = { "CyberForestLE", "AutomatonLE", "KairosJunctionLE", "KingsCoveLE", "NewRepugnancyLE",
			"PortAleksanderLE", "YearZeroLE" };
	final boolean debugOut = false;
	final Difficulty DIFFICULTY = Difficulty.VeryHard;
	final String MAP = "CyberForestLE";
	final String urlPath = "/sc2api";
}
