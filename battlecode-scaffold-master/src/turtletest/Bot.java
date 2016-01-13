package turtletest;

// inspired by https://github.com/TheDuck314/battlecode2015/blob/master/teams/zephyr26_final/Bot.java

import battlecode.common.*;

public class Bot {

	public static RobotController rc;

	protected static Team myTeam;
	protected static Team enemyTeam;
	protected static MapLocation personalHQ; // my gathering point. may change.
	// NOT set upon init.
	// basically where you should gather if not on a mission,
	// for example, archon that made you.
	protected static int strategy;

	// treat these as final even though they aren't really
	public static RobotType TYPE;
	public static int ID;
	public static int SIGHT_RANGE;
	public static int ATTACK_RANGE;

	protected static MapLocation myLocation; // bot classes are responsible for keeping this up to date

	protected static void init(RobotController _rc) throws GameActionException {
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
