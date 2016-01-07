package team074;

import battlecode.common.*;

public class Archon extends Bot {
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			action();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time
	}
	private static void action() throws GameActionException {
		// take my turn
		Clock.yield();
	}
}
