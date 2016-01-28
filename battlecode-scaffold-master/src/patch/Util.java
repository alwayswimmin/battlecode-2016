package patch;

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
	
	private static RobotInfo[] temp = new RobotInfo[100];
	public static boolean likelyToBecomeZombie(RobotInfo info, RobotInfo[] enemies) {
		copyEnemiesInAttackPosition(info.location, enemies, temp);
		double overallTurns = 0.0;
		int vipersIAP = 0;
		int zombiesIAP = 0;
		for(int i = -1; temp[++i] != null; ) {
			if(temp[i].type != RobotType.VIPER || vipersIAP++ == 0) {
				overallTurns += 1.0 / (Combat.turnsToKill(temp[i], info));
			}
			if(temp[i].team == Team.ZOMBIE) {
				zombiesIAP++;
			}
		}
		overallTurns = 1.0 / (0.001 + overallTurns); // now actually the number of turns
		return vipersIAP > 0 && overallTurns < 20 || zombiesIAP > 0 && overallTurns < 10 || info.viperInfectedTurns > overallTurns || info.zombieInfectedTurns > overallTurns;
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

	public static double[] zombieAttackMultiplier = {1.00, 1.10, 1.20, 1.30, 1.50, 1.70, 2.00, 2.30, 2.60, 3.00};
	public static double attackPower(RobotType robot) {
		double multiplier = 1.0;
		if(robot.isZombie) {
			int zombieRound = rc.getRoundNum() / 300;
			if(zombieRound > 9) {
				multiplier = (zombieRound - 9) * 1.0 + zombieAttackMultiplier[9];
			} else {
				multiplier = zombieAttackMultiplier[zombieRound];
			}
		}
		switch (robot) {
			case VIPER:
				return 8; // account for viper infection
			default:
				return robot.attackPower * multiplier;
		}
	}
	public static boolean isCorner(MapLocation loc) throws GameActionException {
		int counter = 0;
		if(rc.canSense(loc.add(Direction.NORTH))) {
			if(!rc.onTheMap(loc.add(Direction.NORTH))) {
				counter++;
			}
		}
		if(rc.canSense(loc.add(Direction.EAST))) {
			if(!rc.onTheMap(loc.add(Direction.EAST))) {
				counter++;
			}
		}
		if(rc.canSense(loc.add(Direction.SOUTH))) {
			if(!rc.onTheMap(loc.add(Direction.SOUTH))) {
				counter++;
			}
		}
		if(rc.canSense(loc.add(Direction.WEST))) {
			if(!rc.onTheMap(loc.add(Direction.WEST))) {
				counter++;
			}
		}
		return counter >= 2;
		
	}
	public static boolean isEdge(MapLocation loc) throws GameActionException {
		int counter = 0;
		if(rc.canSense(loc.add(Direction.NORTH))) {
			if(!rc.onTheMap(loc.add(Direction.NORTH))) {
				counter++;
			}
		}
		if(rc.canSense(loc.add(Direction.EAST))) {
			if(!rc.onTheMap(loc.add(Direction.EAST))) {
				counter++;
			}
		}
		if(rc.canSense(loc.add(Direction.SOUTH))) {
			if(!rc.onTheMap(loc.add(Direction.SOUTH))) {
				counter++;
			}
		}
		if(rc.canSense(loc.add(Direction.WEST))) {
			if(!rc.onTheMap(loc.add(Direction.WEST))) {
				counter++;
			}
		}
		return counter >= 1;
		
	}
}
