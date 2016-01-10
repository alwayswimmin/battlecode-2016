package team074;

import battlecode.common.*;

public class Soldier extends Bot {
	private static final int ATTACK_RANGE = 13;
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			Radio.process();
			action();
			Radio.clear();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time

	}
	private static MapLocation defendLocation = null;
	private static MapLocation attackLocation = null;
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();
		boolean shouldAttack = false;	
		if (ATTACK_RANGE > 0) {
			RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, enemyTeam);
			RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, Team.ZOMBIE);
			if (enemiesWithinRange.length > 0) {
				shouldAttack = true;
				// Check if weapon is ready
				if (rc.isWeaponReady()) {
					rc.attackLocation(enemiesWithinRange[0].location);
				}
			} else if (zombiesWithinRange.length > 0) {
				shouldAttack = true;
				// Check if weapon is ready
				if (rc.isWeaponReady()) {
					rc.attackLocation(zombiesWithinRange[0].location);
				}
			}
		}
		if(!shouldAttack) {
			MapLocation newDefend = null, newAttack = null;
			newDefend = Radio.getDefendLocation();
			if(newDefend != null) {
				if(defendLocation == null && newDefend != null) {
					defendLocation = newDefend;
				}
			} else {
				defendLocation = null;
			}
			newAttack = Radio.getMoveLocation();
			if(attackLocation == null && newAttack != null) {
				attackLocation = newAttack;
			}
			if(defendLocation != null) {
				if(rc.isCoreReady()) {
					Nav.goTo(defendLocation);
				}
			} else if(attackLocation != null) {
				if(rc.isCoreReady()) {
					Nav.goTo(attackLocation);
				}
				if(rc.canSenseLocation(attackLocation) && rc.senseRobotAtLocation(attackLocation) == null) {
					attackLocation = null;
				}
			}
		}
		Clock.yield();
	}
}
