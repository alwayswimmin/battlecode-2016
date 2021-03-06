package finalbot;

import battlecode.common.*;

interface SafetyPolicy {
	public boolean safe(MapLocation loc) throws GameActionException ;
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

// avoid short range units
class SPShort extends Bot implements SafetyPolicy {
	RobotInfo[] enemies;
	public SPShort(RobotInfo[] _enemies) {
		enemies = _enemies;
	}

	public boolean safe(MapLocation loc) {
		for (int i = enemies.length; --i >= 0; ) {	
			if(enemies[i].type == RobotType.ZOMBIEDEN) {
				int dx = enemies[i].location.x - loc.x;
				int dy = enemies[i].location.y - loc.y;
				if(dx < 0) {
					dx = -dx;
				}
				if(dy < 0) {
					dy = -dy;
				}
				if(dx + dy <= 2 && dx != 2 && dy != 2) {
					return false;
				}
			}
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
		if(rc.getType() != RobotType.TTM && rc.getType() != RobotType.SCOUT && rc.getType() != RobotType.ARCHON) {
			if(rc.senseRubble(myLocation.add(dir)) >= GameConstants.RUBBLE_SLOW_THRESH) {
				rc.clearRubble(dir);
				return true;
			}
		}
		rc.move(dir);
		return true;
	}

	private static boolean canMove(Direction dir) throws GameActionException {
		return rc.canMove(dir) && policy.safe(myLocation.add(dir));
	}
	// used to move directly or 45 degrees off. return true if worked.
	// now moves in best local direction.
	private static boolean tryMoveDirect() throws GameActionException {
//		PairDirectionDouble pdd = dfsRubble(myLocation, 0, 0.0, 2, 2000.5);
//		Direction mydir = dijkstraRubble(2, 2000.5);
//		if(pdd != null) {
//			Direction mydir = pdd.direction;
//		if(mydir != null) {
//			if (canMove(mydir)) {
//				move(mydir);
//				dir = mydir;
//			} else {
//				rc.clearRubble(mydir);
//			}
//			System.out.println("trying to go in direction " + mydir);
//			return true;
//		}
//		System.out.println("couldn't move direct");


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
		if(TYPE == RobotType.SCOUT) {
			return false;
		}
		double rubbleForward = rc.senseRubble(myLocation.add(toDest));
		double rubbleLeft = rc.senseRubble(myLocation.add(dirLeft));
		double rubbleRight = rc.senseRubble(myLocation.add(dirRight));
		double threshold = 500.5;

		if(rc.getType() != RobotType.TTM) {

		if(rubbleLeft < rubbleForward) {
			if(rubbleLeft < rubbleRight) {
				if(rubbleRight < rubbleForward) {
					// left right forward
					if(rubbleLeft < threshold && !rc.isLocationOccupied(myLocation.add(dirLeft)) && rc.onTheMap(myLocation.add(dirLeft))) {
						rc.clearRubble(dirLeft);
						return true;
					}
					if(rubbleRight < threshold && !rc.isLocationOccupied(myLocation.add(dirRight)) && rc.onTheMap(myLocation.add(dirRight))) {
						rc.clearRubble(dirRight);
						return true;
					}
					if(rubbleForward < threshold && !rc.isLocationOccupied(myLocation.add(toDest)) && rc.onTheMap(myLocation.add(toDest))) {
						rc.clearRubble(toDest);
						return true;
					}
				} else {
					// left forward right
					if(rubbleLeft < threshold && !rc.isLocationOccupied(myLocation.add(dirLeft)) && rc.onTheMap(myLocation.add(dirLeft))) {
						rc.clearRubble(dirLeft);
						return true;
					}
					if(rubbleForward < threshold && !rc.isLocationOccupied(myLocation.add(toDest)) && rc.onTheMap(myLocation.add(toDest))) {
						rc.clearRubble(toDest);
						return true;
					}
					if(rubbleRight < threshold && !rc.isLocationOccupied(myLocation.add(dirRight)) && rc.onTheMap(myLocation.add(dirRight))) {
						rc.clearRubble(dirRight);
						return true;
					}
				}
			} else {
				// right left forward
					if(rubbleRight < threshold && !rc.isLocationOccupied(myLocation.add(dirRight)) && rc.onTheMap(myLocation.add(dirRight))) {
						rc.clearRubble(dirRight);
						return true;
					}
					if(rubbleLeft < threshold && !rc.isLocationOccupied(myLocation.add(dirLeft)) && rc.onTheMap(myLocation.add(dirLeft))) {
						rc.clearRubble(dirLeft);
						return true;
					}
					if(rubbleForward < threshold && !rc.isLocationOccupied(myLocation.add(toDest)) && rc.onTheMap(myLocation.add(toDest))) {
						rc.clearRubble(toDest);
						return true;
					}
			}
		} else {
			if(rubbleForward < rubbleRight) {
				if(rubbleLeft < rubbleRight) {
					// forward left right
					if(rubbleForward < threshold && !rc.isLocationOccupied(myLocation.add(toDest)) && rc.onTheMap(myLocation.add(toDest))) {
						rc.clearRubble(toDest);
						return true;
					}
					if(rubbleLeft < threshold && !rc.isLocationOccupied(myLocation.add(dirLeft)) && rc.onTheMap(myLocation.add(dirLeft))) {
						rc.clearRubble(dirLeft);
						return true;
					}
					if(rubbleRight < threshold && !rc.isLocationOccupied(myLocation.add(dirRight)) && rc.onTheMap(myLocation.add(dirRight))) {
						rc.clearRubble(dirRight);
						return true;
					}
				} else {
					// forward right left
					if(rubbleForward < threshold && !rc.isLocationOccupied(myLocation.add(toDest)) && rc.onTheMap(myLocation.add(toDest))) {
						rc.clearRubble(toDest);
						return true;
					}
					if(rubbleRight < threshold && !rc.isLocationOccupied(myLocation.add(dirRight)) && rc.onTheMap(myLocation.add(dirRight))) {
						rc.clearRubble(dirRight);
						return true;
					}
					if(rubbleLeft < threshold && !rc.isLocationOccupied(myLocation.add(dirLeft)) && rc.onTheMap(myLocation.add(dirLeft))) {
						rc.clearRubble(dirLeft);
						return true;
					}
				}
			} else {
				// right forward left
					if(rubbleRight < threshold && !rc.isLocationOccupied(myLocation.add(dirRight)) && rc.onTheMap(myLocation.add(dirRight))) {
						rc.clearRubble(dirRight);
						return true;
					}
					if(rubbleForward < threshold && !rc.isLocationOccupied(myLocation.add(toDest)) && rc.onTheMap(myLocation.add(toDest))) {
						rc.clearRubble(toDest);
						return true;
					}
					if(rubbleLeft < threshold && !rc.isLocationOccupied(myLocation.add(dirLeft)) && rc.onTheMap(myLocation.add(dirLeft))) {
						rc.clearRubble(dirLeft);
						return true;
					}
			}
		}
			
			boolean goodSpotFound = false;
			MapLocation[] neighborhood = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, SIGHT_RANGE);
			for(int k = neighborhood.length; --k >= 0; ) {
				if(rc.onTheMap(neighborhood[k]) && rc.senseRubble(neighborhood[k]) < threshold && neighborhood[k].distanceSquaredTo(dest) < myLocation.distanceSquaredTo(dest)) {
					goodSpotFound = true;
					break;
				}
			}

			if((goodSpotFound || rc.canSense(dest)) && rc.onTheMap(myLocation.add(toDest)) && rubbleForward < 2000.5) {
				rc.clearRubble(toDest);
				return true;
			}

		}
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
/*
	private static double[][] dist = new double[11][11];
	private static boolean[][] visited = new boolean[11][11];
	private static Direction[][] dirTo = new Direction[11][11];
	private static MapLocation[][] locations = new MapLocation[11][11];
	private static double[][] cost = new double[11][11];

	private static Direction dijkstraRubble(int stepThreshold, double rubbleThreshold) throws GameActionException {
		for(int i = 5 + stepThreshold + 1; --i >= 5 - stepThreshold; ) {
			for(int j = 5 + stepThreshold + 1; --j >= 5 - stepThreshold; ) {
				dist[i][j] = rubbleThreshold;
				visited[i][j] = false;
				dirTo[i][j] = null;
				locations[i][j] = new MapLocation(myLocation.x + i - 5, myLocation.y + j - 5);
				if(!rc.onTheMap(locations[i][j])) {
					locations[i][j] = null;
				} else if(!locations[i][j].equals(myLocation) && rc.isLocationOccupied(locations[i][j])) {
					locations[i][j] = null;
				} else {
					cost[i][j] = rc.senseRubble(locations[i][j]);
				}
			}
		}
		dist[5][5] = 0.0;
		while(true) {
			int imin = -1, jmin = -1;
			for(int i = 5 + stepThreshold + 1; --i >= 5 - stepThreshold; ) {
				for(int j = 5 + stepThreshold + 1; --j >= 5 - stepThreshold; ) {
					if(!visited[i][j] && (imin == -1 || dist[i][j] < dist[imin][jmin])) {
						imin = i;
						jmin = j;
					}
				}
			}
			if(imin == -1) {
				break;
			}
			if(dist[imin][jmin] >= rubbleThreshold) {
				break;
			}
			visited[imin][jmin] = true;
			for(int i = -2; ++i <= 1; ) {
				for(int j = -2; ++j <= 1; ) {
					if(visited[imin + i][jmin + j]) {
						continue;
					}
					if(locations[imin + i][jmin + j] == null) {
						continue;
					}
					if(dist[imin + i][jmin + j] > dist[imin][jmin] + cost[imin + i][jmin + j] + 1.0) {
						dist[imin + i][jmin + j] = dist[imin][jmin] + cost[imin + i][jmin + j] + 1.0;
						dirTo[imin + i][jmin + j] = dirTo[imin][jmin] == null ? myLocation.directionTo(locations[imin + i][jmin + j]) : dirTo[imin][jmin];
					}
				}
			}
		}
		int imin = -1, jmin = -1;
		for(int i = 5 + stepThreshold + 1; --i >= 5 - stepThreshold; ) {
			for(int j = 5 + stepThreshold + 1; --j >= 5 - stepThreshold; ) {
				if((imin == -1 || dist[i][j] < dist[imin][jmin]) && locations[i][j] != null && myLocation.distanceSquaredTo(dest) > locations[i][j].distanceSquaredTo(dest)) {
					imin = i;
					jmin = j;
				}
			}
		}
		return imin == -1 ? null : dirTo[imin][jmin];
	}

	private static PairDirectionDouble dfsRubble(MapLocation curr, int steps, double accumulatedRubble, int stepThreshold, double rubbleThreshold) throws GameActionException {
		// returns best direction to move in, as determined by least amount of rubble
		// returns null if no good direction exists
		// returns Direction.NONE if curr is better than myLocation or if curr is dest
		if(!rc.onTheMap(curr)) {
			return null; // can't move here
		}
		if(accumulatedRubble >= rubbleThreshold) {
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
*/

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
		if(Util.isAttacker(TYPE)) {
			wallOnRight = ID % 2 == 0;
		} else {
			wallOnRight = dest.distanceSquaredTo(myLocation.add(leftDir)) < dest.distanceSquaredTo(myLocation.add(rightDir));
		}
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

		policy = _policy;

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
