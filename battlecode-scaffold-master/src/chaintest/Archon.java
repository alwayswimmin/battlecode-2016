package chaintest;

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
		personalHQ = new MapLocation(149, 296);
	}
	private static int scoutsBuilt = 0;
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();
		if (rc.isCoreReady()) {
			if (myLocation.distanceSquaredTo(personalHQ) > 30) {
				Direction dirToMove = myLocation.directionTo(personalHQ);
				if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					// Too much rubble, so I should clear it
					rc.clearRubble(dirToMove);
					// Check if I can move in this direction
				} else if (rc.canMove(dirToMove)) {
					// Move
					rc.move(dirToMove);
				}
			} else {
				RobotType typeToBuild = scoutsBuilt++ < 5 ? RobotType.SCOUT : RobotType.TURRET;
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
		}
		Clock.yield();
	}
}
