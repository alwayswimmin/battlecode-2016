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

	private static MapLocation denLocation = null;
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();

		RobotInfo[] hostileWithinRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);

		double enemycenterx = 0, enemycentery = 0;
		MapLocation enemycenter = null;

		if(hostileWithinRange.length != 0) {
			Radio.broadcastDefendLocation(myLocation, 1000);
			for(int i = hostileWithinRange.length; --i >= 0; ) {
				enemycenterx += hostileWithinRange[i].location.x;
				enemycentery += hostileWithinRange[i].location.y;
			}
			enemycenter = new MapLocation((int) (enemycenterx / hostileWithinRange.length + (enemycenterx > 0 ? 1 : -1)), (int) (enemycentery / hostileWithinRange.length + (enemycentery > 0 ? 1 : -1)));
		} else {

			denLocation = Radio.getDenLocation();

			if(denLocation != null) {
				Radio.broadcastMoveLocation(denLocation, 1000);
			}
		}

				RobotType typeToBuild = scoutsBuilt++ < 1 ? RobotType.SCOUT : (Math.random() > 0.5 ? RobotType.SOLDIER : RobotType.GUARD);
		if (rc.isCoreReady()) {
			if(enemycenter != null && !enemycenter.equals(myLocation)) {
				MapLocation dest = new MapLocation(4 * myLocation.x - 3 * enemycenter.x, 4 * myLocation.y - 3 * enemycenter.y);
				Nav.goTo(dest, new SPAll(hostileWithinRange));
			} else if (rc.hasBuildRequirements(typeToBuild)) {
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
			} else if (denLocation != null) {
				Nav.goTo(denLocation);
			}
		}

		Clock.yield();
	}
}
