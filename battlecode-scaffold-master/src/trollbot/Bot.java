// bot template inherited by all robots
// inspired by https://github.com/TheDuck314/battlecode2015/blob/master/teams/zephyr26_final/Bot.java
package trollbot;

import battlecode.common.*;

public class Bot {

	public static RobotController rc;

	protected static Team myTeam;
	protected static Team enemyTeam;
	protected static MapLocation personalHQ; // where to go when not on mission
	protected static int strategy;

	public static RobotType TYPE;
	public static int ID;
	public static int SIGHT_RANGE;
	public static int ATTACK_RANGE;
	public static Direction[] directions = new Direction[8];

	protected static MapLocation myLocation; // bot classes are responsible for keeping this up to date

	public static double distanceBetween(MapLocation a, MapLocation b) {
		return (a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y);
	}

	public static int distToWall(MapLocation a, Direction dir) throws GameActionException {
		MapLocation b = a;
		for (int i = 0; i < 4; ++i) {
			b = b.add(dir);
			if (!rc.onTheMap(b))
				return i+1;
		}

		return 1000; //represents "very fat", out of SIGHT_RANGE
	}

	public static int rotationsTo(Direction a, Direction b) throws GameActionException {
		Direction c = a;

		for (int i = 0; i < 8; ++i) {
			if (c == b)
				return Math.min(i, 8-i);
			c = c.rotateLeft();
		}

		return 0;
	}

	protected static void init(RobotController _rc) throws GameActionException {
		// initializes bot fields
		rc = _rc;

		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		strategy = -1;

		myLocation = rc.getLocation();

		Radio.init();

		TYPE = rc.getType();
		ID = rc.getID();
		SIGHT_RANGE = TYPE.sensorRadiusSquared;
		ATTACK_RANGE = TYPE.attackRadiusSquared;

		directions[0] = Direction.EAST;
		for (int i = 1; i < 8; ++i) {
			directions[i] = directions[i-1].rotateLeft();
		}
	}
}

