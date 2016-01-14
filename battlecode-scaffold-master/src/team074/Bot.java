// bot template inherited by all robots
// inspired by https://github.com/TheDuck314/battlecode2015/blob/master/teams/zephyr26_final/Bot.java
package team074;

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

	protected static MapLocation myLocation; // bot classes are responsible for keeping this up to date

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
	}
}
