package turtletest;

import battlecode.common.*;

interface SafetyPolicy {
	public boolean safe(MapLocation loc);
}

// avoid none
class SPNone extends Bot implements SafetyPolicy {
	public boolean safe(MapLocation loc) {
		return true;
	}
}

// avoid all
class SPAll extends Bot implements SafetyPolicy {
	RobotInfo[] enemies;

	public SPAll(RobotInfo[] _enemies) {
		enemies = _enemies;
	}

	public SPAll() {
		enemies = new RobotInfo[0];
	}

	public boolean safe(MapLocation loc) {
		for (int i = enemies.length; --i >= 0; ) {
			if (enemies[i].type.attackRadiusSquared >= loc.distanceSquaredTo(enemies[i].location)) return false;
		}
		return true;
	}
}

public class Nav extends Bot {

	private static MapLocation dest;
	private static MapLocation start;
	private static SafetyPolicy policy;
	private static boolean obsFlag; // true means obstacle detected, false otherwise
	private static boolean wallOnRight; // true means wall on right hand side in bug

	private static Direction toDest;
	private static Direction dir;

	private static void init(MapLocation _dest, SafetyPolicy _policy) throws GameActionException {
		dest = _dest;
		start = myLocation;
		policy = _policy;
		obsFlag = false;
		wallOnRight = true;
		toDest = myLocation.directionTo(dest);
		dir = toDest;
	}

	private static boolean move(Direction dir) throws GameActionException {
		rc.move(dir);
		return true;
	}

	private static boolean canMove(Direction dir) {
		return rc.canMove(dir) && policy.safe(myLocation.add(dir));
	}
	// move directly or 45 degrees off. return true if worked.
	private static boolean tryMoveDirect() throws GameActionException {
		if (canMove(toDest)) {
			move(toDest);
			dir = toDest;
			return true;
		}

		Direction dirLeft = toDest.rotateLeft();
		Direction dirRight = toDest.rotateRight();
		if (myLocation.add(dirLeft).distanceSquaredTo(dest) < myLocation.add(dirRight).distanceSquaredTo(dest)) {
			if (canMove(dirLeft)) {
				move(dirLeft);
				dir = dirLeft;
				return true;
			}
			if (canMove(dirRight)) {
				move(dirRight);
				dir = dirRight;
				return true;
			}
		} else {
			if (canMove(dirRight)) {
				move(dirRight);
				dir = dirRight;
				return true;
			}
			if (canMove(dirLeft)) {
				move(dirLeft);
				dir = dirLeft;
				return true;
			}
		}
		if (rc.senseRubble(rc.getLocation().add(toDest)) >= 50) {
			MapLocation locAfterMove = rc.getLocation().add(toDest);
			Direction[] nextMoves = {toDest.rotateLeft(), toDest, toDest.rotateRight()};

			if(rc.getType() != RobotType.TTM) {
				boolean y = false;

				for (int i = 0; i < 3; ++i)
					for (int j = 0; j < 3; ++j) {
						if ((dest.equals(locAfterMove.add(nextMoves[i])) || rc.senseRubble(locAfterMove.add(nextMoves[i])) < 50) && rc.onTheMap(locAfterMove.add(nextMoves[i])))
							y = true;
						if ((dest.equals(locAfterMove.add(nextMoves[i]).add(nextMoves[j])) || rc.senseRubble(locAfterMove.add(nextMoves[i]).add(nextMoves[j])) < 50) && rc.onTheMap(locAfterMove.add(nextMoves[i]).add(nextMoves[j])))
							y = true;
					}

				if (y == true && rc.onTheMap(rc.getLocation().add(toDest))) {
					rc.clearRubble(toDest);
					return true;
				}
			}
		}
		return false;
	}

	private static void chooseWallDirection() throws GameActionException {
		Direction leftDir = dir.rotateLeft();
		for(int i = 3; --i >= 0;) {
			if(!canMove(leftDir)) {
				leftDir = leftDir.rotateLeft();
			}
			else {
				break;
			}
		}
		Direction rightDir = dir.rotateRight();
		for(int i = 3; --i >= 0;) {
			if(!canMove(rightDir)) {
				rightDir = rightDir.rotateRight();
			}
			else {
				break;
			}
		}
		wallOnRight = dest.distanceSquaredTo(myLocation.add(leftDir)) < dest.distanceSquaredTo(myLocation.add(rightDir));
		dir = wallOnRight ? leftDir : rightDir;
	}

	private static void followBoundary() throws GameActionException {
		dir = wallOnRight ? dir.rotateRight() : dir.rotateLeft();
		for(int i = 8; --i >= 0;) {
			if(canMove(dir)) {
				move(dir);
				return;
			}
			dir = wallOnRight ? dir.rotateLeft() : dir.rotateRight();
		}
	}

	public static void goTo(MapLocation _dest, SafetyPolicy _policy) throws GameActionException {
		if(!_dest.equals(dest)) {
			init(_dest, _policy);
		}

		if(myLocation.equals(dest)) {
			return;
		}

		toDest = myLocation.directionTo(dest);

		if(!tryMoveDirect()) {
			if(!obsFlag) {
				chooseWallDirection();
			}
			obsFlag = true;
			followBoundary();
		} else {
			obsFlag = false;
		}
	}

	public static void goTo(MapLocation _dest) throws GameActionException {
		goTo(_dest, new SPNone());
	}

	public static MapLocation closestWall() throws GameActionException {
		MapLocation[] visibleLocations = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, SIGHT_RANGE);
		MapLocation toReturn = null;
		for(int i = visibleLocations.length; --i >= 0; ) {
			if(rc.onTheMap(visibleLocations[i]) && (toReturn == null || myLocation.distanceSquaredTo(visibleLocations[i]) < myLocation.distanceSquaredTo(toReturn))) {
				toReturn = visibleLocations[i];
			}
		}
		return toReturn;
	}
}
