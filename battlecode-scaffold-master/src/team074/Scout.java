package team074;

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
				if(Math.random() > 0.5) {
					if(Math.random() > 0.3) {
						dirToMove = Direction.NORTH_EAST;
					} else {
						dirToMove = Direction.SOUTH_EAST;
					}
				}
				if (rc.canMove(dirToMove)) {
					// Move
					rc.move(dirToMove);
				}
		}
		if(rc.getInfectedTurns() > 0) {
			rc.disintegrate();
		}
		Clock.yield();
	}
}
