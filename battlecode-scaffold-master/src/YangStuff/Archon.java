package seekdentest;

import battlecode.common.*;
import java.util.Random;

public class Archon extends Bot {
	private static final int SIGHT_RANGE = 35;
	private static int scoutsBuilt = 0;
	private static int ct = 0;
	private static long turn = 1;
	private static long x = 12345678;
	private static long mod = 1000000007;

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

	private static int dist(MapLocation a, MapLocation b) throws GameActionException {
		return (a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y);
	}

	private static void action() throws GameActionException {
		// take my turn
		turn = turn*x%mod;
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
			RobotType typeToBuild = (scoutsBuilt < 2 ? RobotType.SCOUT : RobotType.SOLDIER);

			if (ct > 1 && (ct%5 == 0)) {
				typeToBuild = RobotType.TURRET;
			}

			boolean y = false;

			RobotType typeToBuild = scoutsBuilt++ < 1 ? RobotType.SCOUT : RobotType.SOLDIER;
			if (rc.hasBuildRequirements(typeToBuild)) {

				Direction dirToBuild = Direction.EAST;
				for (int i = 0; i < 8; i++) {
					// If possible, build in this direction
					MapLocation toBuild = rc.getLocation().add(dirToBuild);
					// System.out.println(toBuild);
					
					if ((toBuild.x+toBuild.y)%2 == 0 && (ct%5 == 0)) {
						dirToBuild = dirToBuild.rotateLeft(); continue;
					}

					if ((toBuild.x+toBuild.y)%2 == 1 && (ct%5 != 0)) {
						dirToBuild = dirToBuild.rotateLeft(); continue;
					}

					if (rc.canBuild(dirToBuild, typeToBuild)) {
						rc.build(dirToBuild, typeToBuild); ct++; scoutsBuilt++; y = true;
						break;
					} else {
						// Rotate the direction to try
						dirToBuild = dirToBuild.rotateLeft();
					}
				}
			}

			if (y == false) {
				Direction dirToMove = Direction.EAST;

				int k = (int)(turn%8);

				for (int i = 0; i < k; ++i)
					dirToMove = dirToMove.rotateLeft();

				for (int i = 0; i < 8; ++i) {
					if (rc.canMove(dirToMove) && dist(rc.getLocation().add(dirToMove), personalHQ) < 40) {
						rc.move(dirToMove); break;
					} else {
						dirToMove = dirToMove.rotateRight();
					}
				}
			}
		}

		Clock.yield();
	}
}
