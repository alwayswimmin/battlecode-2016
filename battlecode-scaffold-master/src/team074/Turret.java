package team074;

import battlecode.common.*;

public class Turret extends Bot {
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			action();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time

	}
	private static void action() throws GameActionException {
		// take my turn
		int myAttackRange = rc.getType().attackRadiusSquared;
		if (rc.isWeaponReady()) {
			RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
			RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
			if (enemiesWithinRange.length > 0) {
				for (RobotInfo enemy : enemiesWithinRange) {
					// Check whether the enemy is in a valid attack range (turrets have a minimum range)
					if (rc.canAttackLocation(enemy.location)) {
						rc.attackLocation(enemy.location);
						break;
					}
				}
			} else if (zombiesWithinRange.length > 0) {
				for (RobotInfo zombie : zombiesWithinRange) {
					if (rc.canAttackLocation(zombie.location)) {
						rc.attackLocation(zombie.location);
						break;
					}
				}
			}
		}
		Clock.yield();
	}
}
