package chaintest;

import battlecode.common.*;

public class Scout extends Bot {
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		while(true) {
			action();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time

	}
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();
		if (rc.isCoreReady()) {
				Direction dirToMove = Direction.EAST;
				if (rc.canMove(dirToMove)) {
					// Move
					rc.move(dirToMove);
				}
		}
		Clock.yield();
	}
}
