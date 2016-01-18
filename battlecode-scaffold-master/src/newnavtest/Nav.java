package newnavtest;

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
	// used to move directly or 45 degrees off. return true if worked.
	// now moves in best local direction.
	private static boolean tryMoveDirect() throws GameActionException {
		PairDirectionDouble pdd = dfsRubble(myLocation, 0, 0.0, 2, 2000.5);

		if(pdd != null) {
			Direction mydir = pdd.direction;
			if (canMove(mydir)) {
				move(mydir);
				dir = mydir;
			} else {
				rc.clearRubble(mydir);
			}
			System.out.println("trying to go in direction " + mydir);
			return true;
		}
//		System.out.println("couldn't move direct");

		return false;
	}

	private static class PairDirectionDouble {
		Direction direction;
		double d;
		PairDirectionDouble(Direction _direction, double _d) {
			direction = _direction;
			d = _d;
		}
	}

	private static PairDirectionDouble dfsRubble(MapLocation curr, int steps, double accumulatedRubble, int stepThreshold, double rubbleThreshold) throws GameActionException {
		// returns best direction to move in, as determined by least amount of rubble
		// returns null if no good direction exists
		// returns Direction.NONE if curr is better than myLocation or if curr is dest
		if(!rc.onTheMap(curr)) {
			return null; // can't move here
		}
		if(accumulatedRubble > rubbleThreshold) {
			return null; // too much rubble to go on this path
		}
		if(curr.equals(dest)) {
			return new PairDirectionDouble(Direction.NONE, accumulatedRubble); // found dest
		}
		if(rc.isLocationOccupied(curr) && !curr.equals(myLocation)) {
			return null; // square is blocked, don't count on robot here moving...
		}

		if(rc.senseRubble(curr) < GameConstants.RUBBLE_OBSTRUCTION_THRESH && curr.distanceSquaredTo(dest) < myLocation.distanceSquaredTo(dest)) {
			return new PairDirectionDouble(Direction.NONE, accumulatedRubble); // found a path to a square not too costly that is closer
		}

		if(steps >= stepThreshold) {
			return null; // couldn't get closer, so end dfs, too costly
		}
//		System.out.println("testing square " + curr);
		Direction mydir = toDest;
		PairDirectionDouble best = null;
		boolean turnRight = Math.random() < 0.5;
		for(int i = 8; --i >= 0;) {
			MapLocation next = curr.add(mydir);
			PairDirectionDouble pdd = dfsRubble(next, steps + 1, accumulatedRubble + rc.senseRubble(next), stepThreshold, rubbleThreshold);
			if(pdd != null && (best == null || pdd.d < best.d - GameConstants.RUBBLE_SLOW_THRESH)) {
			// dont change from toDest unless you find something significantly better
				best = new PairDirectionDouble(mydir, pdd.d);
			}
			mydir = turnRight ? mydir.rotateRight() : mydir.rotateLeft();
		}
		return best;
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
