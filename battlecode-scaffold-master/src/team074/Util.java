package team074;

import battlecode.common.*;

// Util implements basic tasks for calculation

public class Util extends Bot {
	public static RobotInfo closest(RobotInfo[] robots, MapLocation toHere) {
		RobotInfo ret = null;
		int bestDistSq = 999999;
		for (int i = robots.length; i-- > 0;) {
			int distSq = toHere.distanceSquaredTo(robots[i].location);
			if (distSq < bestDistSq) {
				bestDistSq = distSq;
				ret = robots[i];
			}
		}
		return ret;
	}

	public static RobotInfo leastHealth(RobotInfo[] robots) {
		RobotInfo ret = null;
		double minHealth = 1e99;
		for(int i = robots.length; i --> 0; ) {
			if(robots[i].health < minHealth) {
				minHealth = robots[i].health;
				ret = robots[i];
			}
		}
		return ret;
	}

	public static void copyEnemiesInAttackPosition(MapLocation loc, RobotInfo[] enemies, RobotInfo[] ret) {
		int cnt = 0;
		for (int i = enemies.length; i-- > 0; ) {
			if (enemies[i].type.attackRadiusSquared >= loc.distanceSquaredTo(enemies[i].location)) {
				ret[cnt++] = enemies[i];
			}
		}
		ret[cnt] = null;
	}

	public static void copyAlliesInAttackPosition(MapLocation loc, RobotInfo[] allies, RobotInfo[] ret) {
		int cnt = 0;
		for (int i = allies.length; i-- > 0; ) {
			if (allies[i].type.attackRadiusSquared >= loc.distanceSquaredTo(allies[i].location)) {
				ret[cnt++] = allies[i];
			}
		}
		ret[cnt] = null;
	}

	public static boolean isAttacker(RobotType robot) {
		switch (robot) {
			case SOLDIER:
			case VIPER:
			case GUARD:
			case TURRET:
				return true;

			default:
				return false;
		}
	}

	public static boolean isNotAttacker(RobotType robot) {
		switch (robot) {
			case ARCHON:
			case SCOUT:
			case ZOMBIEDEN:
			case TTM:
				return true;

			default:
				return false;
		}
	}

	public static double attackPower(RobotType robot) {
		switch (robot) {
			case VIPER:
				return 8; // account for viper infection
			default:
				return robot.attackPower;
		}
	}
}
