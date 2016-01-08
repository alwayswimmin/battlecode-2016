package navtest;

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
		personalHQ = new MapLocation(155, 279);
	}
	private static int scoutsBuilt = 0;
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();
		if (rc.isCoreReady()) {
			Nav.goTo(personalHQ); // no avoid yet...
		}
		Clock.yield();
	}
}
