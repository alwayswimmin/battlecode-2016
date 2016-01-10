package seekdentest;

import battlecode.common.*;

public class Archon extends Bot {
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

	private static double distanceBetween(MapLocation a, MapLocation b) {
		return (a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y);
	}

	private static IdAndMapLocation den = null;

	private static int turnsSinceEnemySeen = 100;

	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();

		RobotInfo[] hostileWithinRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		RobotInfo[] neutralWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.NEUTRAL);
		RobotInfo[] friendWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, rc.getTeam());

		for (int i = 0; i < friendWithinRange.length; ++i) {
			if (friendWithinRange[i].health != friendWithinRange[i].maxHealth && 
				(friendWithinRange[i].type.attackRadiusSquared > distanceBetween(rc.getLocation(), friendWithinRange[i].location))) {
				rc.repair(friendWithinRange[i].location); break;
			}
		}

		double enemycenterx = 0, enemycentery = 0;
		MapLocation enemycenter = null;

		if(hostileWithinRange.length != 0) {
			if(turnsSinceEnemySeen > 5) {
			Radio.broadcastDefendLocation(myLocation, 1000);
			}
			turnsSinceEnemySeen = 0;
			for(int i = hostileWithinRange.length; --i >= 0; ) {
				enemycenterx += hostileWithinRange[i].location.x;
				enemycentery += hostileWithinRange[i].location.y;
			}
			enemycenter = new MapLocation((int) (enemycenterx / hostileWithinRange.length + (enemycenterx > 0 ? 1 : -1)), (int) (enemycentery / hostileWithinRange.length + (enemycentery > 0 ? 1 : -1)));
		} else {
			turnsSinceEnemySeen++;
			if(turnsSinceEnemySeen == 15) {
				Radio.broadcastClearDefend(1000);
			}

			den = Radio.getDenLocation();

			if(den != null) {
				Radio.broadcastMoveLocation(den.location, 1000);
			}
		}

		RobotType typeToBuild = scoutsBuilt++ < 1 ? RobotType.SCOUT : (Math.random() > 0.5 ? RobotType.SOLDIER : RobotType.GUARD);

		if (rc.isCoreReady()) {
			if (neutralWithinRange.length > 0) {
				rc.activate(neutralWithinRange[0].location);
			} else if(enemycenter != null && !enemycenter.equals(myLocation)) {
				MapLocation dest = new MapLocation(4 * myLocation.x - 3 * enemycenter.x, 4 * myLocation.y - 3 * enemycenter.y);
				Nav.goTo(dest, new SPAll(hostileWithinRange));
			} else if (rc.hasBuildRequirements(typeToBuild)) {
					Direction dirToBuild = Direction.EAST;
					for (int i = 0; i < 8; i++) {
						if (rc.canBuild(dirToBuild, typeToBuild)) {
							rc.build(dirToBuild, typeToBuild);
							break;
						} else {
							dirToBuild = dirToBuild.rotateLeft();
						}
					}
			} else if (den != null) {
				Nav.goTo(den.location);
			}
		}

		Clock.yield();
	}
}
