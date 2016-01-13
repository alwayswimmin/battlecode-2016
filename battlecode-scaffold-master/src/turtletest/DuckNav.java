package turtletest;

// largely taken from https://github.com/TheDuck314/battlecode2015/blob/master/teams/zephyr26_final/Nav.java
// will likely replace with own bugging and other nav later.

import battlecode.common.*;
/*
interface SafetyPolicy {
    public boolean safe(MapLocation loc);
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
*/
public class DuckNav extends Bot {

    private static MapLocation dest;
    private static SafetyPolicy policy;

    private enum NavState {
        DIRECT, BUG
    }

    public enum Side {
        LEFT, RIGHT
    }

    private static NavState bugState;
    public static Side bugWallSide = Side.LEFT;
    private static int bugInitDist;
    private static Direction bugPrevDir;
    private static Direction bugLookStartDir;
    private static int bugRotationCount;
    private static int bugMovesSinceSeenObstacle = 0;

    public static int minBfsInitRound = 0;

    private static boolean move(Direction dir) throws GameActionException {
        rc.move(dir);
        return true;
    }

    private static boolean canMove(Direction dir) {
        return rc.canMove(dir) && policy.safe(myLocation.add(dir));
    }

	// move directly or 45 degrees off. return true if worked.
    private static boolean tryMoveDirect() throws GameActionException {
        Direction toDest = myLocation.directionTo(dest);

        if (canMove(toDest)) {
            move(toDest);
            return true;
        }

        Direction[] dirs = new Direction[2]; // order is priority
        Direction dirLeft = toDest.rotateLeft();
        Direction dirRight = toDest.rotateRight();
        if (myLocation.add(dirLeft).distanceSquaredTo(dest) < myLocation.add(dirRight).distanceSquaredTo(dest)) {
            dirs[0] = dirLeft;
            dirs[1] = dirRight;
        } else {
            dirs[0] = dirRight;
            dirs[1] = dirLeft;
        }
        for (Direction dir : dirs) {
            if (canMove(dir)) {
                move(dir);
                return true;
            }
        }
        return false;
    }

    private static void initBug() throws GameActionException {
        bugInitDist = myLocation.distanceSquaredTo(dest);
        bugPrevDir = myLocation.directionTo(dest);
        bugLookStartDir = myLocation.directionTo(dest);
        bugRotationCount = 0;
        bugMovesSinceSeenObstacle = 0;

        // try to intelligently choose on which side we will keep the wall
        Direction leftTryDir = bugPrevDir.rotateLeft();
        for (int i = 0; i < 3; i++) {
            if (!canMove(leftTryDir)) leftTryDir = leftTryDir.rotateLeft();
            else break;
        }
        Direction rightTryDir = bugPrevDir.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (!canMove(rightTryDir)) rightTryDir = rightTryDir.rotateRight();
            else break;
        }
        if (dest.distanceSquaredTo(myLocation.add(leftTryDir)) < dest.distanceSquaredTo(myLocation.add(rightTryDir))) {
            bugWallSide = Side.RIGHT;
        } else {
            bugWallSide = Side.LEFT;
        }
    }

    private static Direction findBugMoveDir() throws GameActionException {
        bugMovesSinceSeenObstacle++;
        Direction dir = bugLookStartDir;
        for (int i = 8; i-- > 0;) {
            if (canMove(dir)) return dir;
            dir = (bugWallSide == Side.LEFT ? dir.rotateRight() : dir.rotateLeft());
            bugMovesSinceSeenObstacle = 0;
        }
        return null;
    }

    private static int numRightRotations(Direction start, Direction end) {
        return (end.ordinal() - start.ordinal() + 8) % 8;
    }

    private static int numLeftRotations(Direction start, Direction end) {
        return (-end.ordinal() + start.ordinal() + 8) % 8;
    }

    private static int calculateBugRotation(Direction moveDir) {
        if (bugWallSide == Side.LEFT) {
            return numRightRotations(bugLookStartDir, moveDir) - numRightRotations(bugLookStartDir, bugPrevDir);
        } else {
            return numLeftRotations(bugLookStartDir, moveDir) - numLeftRotations(bugLookStartDir, bugPrevDir);
        }
    }

    private static void bugMove(Direction dir) throws GameActionException {
        if (move(dir)) {
            bugRotationCount += calculateBugRotation(dir);
            bugPrevDir = dir;
            if (bugWallSide == Side.LEFT) bugLookStartDir = dir.rotateLeft().rotateLeft();
            else bugLookStartDir = dir.rotateRight().rotateRight();
        }
    }

    private static boolean detectBugIntoEdge() throws GameActionException {
        if (bugWallSide == Side.LEFT) {
            return !rc.onTheMap(myLocation.add(bugPrevDir.rotateLeft()));
        } else {
            return !rc.onTheMap(myLocation.add(bugPrevDir.rotateRight()));
        }
    }

    private static void reverseBugWallFollowDir() throws GameActionException {
        bugWallSide = (bugWallSide == Side.LEFT ? Side.RIGHT : Side.LEFT);
        initBug();
    }

    private static void bugTurn() throws GameActionException {
        if (detectBugIntoEdge()) {
            reverseBugWallFollowDir();
        }
        Direction dir = findBugMoveDir();
        if (dir != null) {
            bugMove(dir);
        }
    }

    private static boolean canEndBug() {
        if (bugMovesSinceSeenObstacle >= 4) return true;
        return (bugRotationCount <= 0 || bugRotationCount >= 8) && myLocation.distanceSquaredTo(dest) <= bugInitDist;
    }

    private static void bugMove() throws GameActionException {
        // Debug.clear("nav");
        // Debug.indicate("nav", 0, "bugMovesSinceSeenObstacle = " + bugMovesSinceSeenObstacle + "; bugRotatoinCount = " + bugRotationCount);

        // Check if we can stop bugging at the *beginning* of the turn
        if (bugState == NavState.BUG) {
            if (canEndBug()) {
                // Debug.indicateAppend("nav", 1, "ending bug; ");
                bugState = NavState.DIRECT;
            }
        }

        // If DIRECT mode, try to go directly to target
        if (bugState == NavState.DIRECT) {
            if (!tryMoveDirect()) {
                // Debug.indicateAppend("nav", 1, "starting to bug; ");
                bugState = NavState.BUG;
                initBug();
            } else {
                // Debug.indicateAppend("nav", 1, "successful direct move; ");
            }
        }

        // If that failed, or if bugging, bug
        if (bugState == NavState.BUG) {
            // Debug.indicateAppend("nav", 1, "bugging; ");
            bugTurn();
        }
    }

/*
    private static boolean tryMoveBfs() throws GameActionException {
        if (!BfsDistributed.isSearchDest(dest)) {
            // BFS is not searching for our destination
            return false;
        }

        Direction bfsDir = BfsDistributed.readResult(myLocation, dest, minBfsInitRound);

        if (bfsDir == null) {
            return false;
        }

        if (canMove(bfsDir)) {
            move(bfsDir);
            return true;
        }

        Direction[] dirs = new Direction[] { bfsDir.rotateLeft(), bfsDir.rotateRight() };
        for (Direction dir : dirs) {
            if (canMove(dir)) {
                move(dir);
                return true;
            }
        }

        // recompute path if we run into a previously unknown obstacle
		if (Util.isImpassable(rc.senseTerrainTile(myLocation.add(bfsDir)))) {
            BfsDistributed.reinitQueue(dest);
            minBfsInitRound = Clock.getRoundNum();
        }

        return false;
    }
*/

    public static void goTo(MapLocation _dest, SafetyPolicy _policy) throws GameActionException {
        if (!_dest.equals(dest)) {
            dest = _dest;
            bugState = NavState.DIRECT;
        }

        if (myLocation.equals(dest)) return;

        policy = _policy;

        bugMove();
    }

	public static void goTo(MapLocation _dest) throws GameActionException {
        if (!_dest.equals(dest)) {
            dest = _dest;
            bugState = NavState.DIRECT;
        }

        if (myLocation.equals(dest)) return;

        policy = new SPAll();

        bugMove();
	}
}
