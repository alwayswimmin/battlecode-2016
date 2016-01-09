package seekdentest;

import battlecode.common.*;

public class Archon extends Bot {
	private static final int SIGHT_RANGE = 35;
	private static int scoutsBuilt = 0;

	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			Radio.process();
			action();
		}
	}

	private static void init() throws GameActionException {
		// things that run for the first time
		personalHQ = rc.getLocation();
	}

	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();

		RobotInfo[] hostileWithinRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);

		if(hostileWithinRange.length != 0) {
			Radio.broadcastDefendLocation(myLocation, 1000);
		} else {

			MapLocation denLocation = Radio.getDenLocation();

			if(denLocation != null) {
				Radio.broadcastMoveLocation(denLocation, 1000);
			}
		}

		if (rc.isCoreReady()) {
			// Nav.goTo(personalHQ); // no avoid yet...
			RobotType typeToBuild = scoutsBuilt++ < 2 ? RobotType.SCOUT : RobotType.SOLDIER;
			if (rc.hasBuildRequirements(typeToBuild)) {
				Direction dirToBuild = Direction.EAST;
				for (int i = 0; i < 8; i++) {
					// If possible, build in this direction
					if (rc.canBuild(dirToBuild, typeToBuild)) {
						rc.build(dirToBuild, typeToBuild);
						break;
					} else {
						// Rotate the direction to try
						dirToBuild = dirToBuild.rotateLeft();
					}
				}
			}
		}

		Clock.yield();
	}
}
