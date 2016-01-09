package seekdentest;

import battlecode.common.*;

public class Archon extends Bot {
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
	private static int scoutsBuilt = 0;
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();
		
		MapLocation denLocation = Radio.getDenLocation();

		if(denLocation != null) {
			System.out.println("Den at " + denLocation.x + " " + denLocation.y);
		}

		if (rc.isCoreReady()) {
			// Nav.goTo(personalHQ); // no avoid yet...
			RobotType typeToBuild = scoutsBuilt++ < 2 ? RobotType.SCOUT : RobotType.GUARD;
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
