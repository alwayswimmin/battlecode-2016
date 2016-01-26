package freshstart;

import battlecode.common.*;
import java.util.*;

// Combat contains methods of action in the presense of enemy units

class SPCombat extends Bot implements SafetyPolicy {
	RobotInfo[] enemies;

	public SPCombat(RobotInfo[] enemies) {
		this.enemies = enemies;
	}

	public boolean safe(MapLocation loc) {
		return Combat.safe(loc, enemies);
	}
}

public class Combat extends Bot {

	private static RobotInfo[] enemiesIAP = new RobotInfo[100];
	private static RobotInfo[] alliesIAP = new RobotInfo[100];
	private static SafetyPolicy safetyPolicy;

	public static int turnsToKill(RobotInfo attacker, RobotInfo victim) {
		int numAttacksAfterFirstToKill = (int) ((victim.health - 0.001) / Util.attackPower(attacker.type));
		int turnsTillFirstAttack = (int) (attacker.weaponDelay);
		double subsequentAttackDelay = attacker.type.attackDelay;
		return (int) (turnsTillFirstAttack + subsequentAttackDelay * numAttacksAfterFirstToKill);
	}

	public static boolean canWin1v1(RobotInfo enemy) {
		return turnsToKill(INFO, enemy) < turnsToKill(enemy, INFO);
	}

	public static boolean canWin1v1AfterMovingTo(MapLocation loc, RobotInfo enemy) {
		int actualTurnsTillFirstAttack = 1 + (int) (Math.max(TYPE.cooldownDelay, rc.getWeaponDelay()) - 1.0);
		int differenceDueToMovement = actualTurnsTillFirstAttack - ( (int) rc.getWeaponDelay());
		return turnsToKill(INFO, enemy) + differenceDueToMovement < turnsToKill(enemy, INFO);
	}

	public static boolean safe(MapLocation loc, RobotInfo[] enemiesWithinSightRange) {
		int numAttackers = 0;
		RobotInfo singleEnemy = null;
		for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
			RobotInfo enemy = enemiesWithinSightRange[i];
			if(enemy.type == RobotType.ZOMBIEDEN && enemy.location.isAdjacentTo(loc)) {
				// getting close to zombie dens is dangerous
				return false;
			}
			if(enemy.type == RobotType.ARCHON) {
				// for whwatever reason, archons have nonnegative attack radius
				continue;
			}
			if(enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
				numAttackers++;
				singleEnemy = enemy;
			}
		}
		if (numAttackers == 0) {
			return true;
		}
		if(numAttackers > 1) {
			return false;
		}
		return canWin1v1AfterMovingTo(loc, singleEnemy);
	}

	public static RobotInfo bestTarget(RobotInfo[] enemiesWithinSightRange) throws GameActionException {
		Util.copyEnemiesInAttackPosition(myLocation, enemiesWithinSightRange, enemiesIAP);
		int enemiesIAPCount = -1;
		while(enemiesIAP[++enemiesIAPCount] != null);
		// enemy prioritization:
		// 1) enemies that can hit us
		// 2) if viper, if enemy is not already infected by viper, or if enemy is not a zombie
		// 3) attacking enemy with least health and most allies can also hit
		RobotInfo bestTarget = null;
		if(TYPE == RobotType.VIPER) {
			boolean isTargetSuboptimal = true;
			double bestRatio = 0;
			for(int i = enemiesIAPCount; --i >= 0; ) {
				RobotInfo enemy = enemiesIAP[i];
				Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
				int attackers = -1;
				while(alliesIAP[++attackers] != null);
				if(ATTACK_RANGE >= myLocation.distanceSquaredTo(enemy.location)) {
					attackers++; // increment since we can attack too
					double ratio = attackers / enemy.health;
					if(!isTargetSuboptimal && (enemy.team == Team.ZOMBIE || enemy.viperInfectedTurns > 3)) {
						// currently already have a target that is better than this one
						continue;
					}
					if(isTargetSuboptimal || ratio > bestRatio) {
						bestRatio = ratio;
						bestTarget = enemy;
						isTargetSuboptimal = enemy.viperInfectedTurns > 3;
					}
				}
			}
			if(bestTarget != null && isTargetSuboptimal) {
				// found an enemy that can hit us but all possible enemies are already infected, and attacking won't do much more damage
				// see if any enemies that can't hit us are not infected
				for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
					RobotInfo enemy = enemiesWithinSightRange[i];
					Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
					int attackers = -1;
					while(alliesIAP[++attackers] != null);
					if(ATTACK_RANGE >= myLocation.distanceSquaredTo(enemy.location)) {
						attackers++; // increment since we can attack too
						double ratio = attackers / enemy.health;
						if(!isTargetSuboptimal && (enemy.team == Team.ZOMBIE || enemy.viperInfectedTurns > 3)) {
							continue;
						}
						if(enemy.team == Team.ZOMBIE || enemy.viperInfectedTurns > 3) {
							continue; // this target is no better than the one we already have
						}
						if(isTargetSuboptimal || ratio > bestRatio) {
							bestRatio = ratio;
							bestTarget = enemy;
							isTargetSuboptimal = enemy.viperInfectedTurns > 3;
						}
					}

				}
			}
			if(bestTarget == null) {
				// all possible targets can't hit us, and all others are suboptimal, so just fire somewhere
				for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
					RobotInfo enemy = enemiesWithinSightRange[i];
					Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
					int attackers = -1;
					while(alliesIAP[++attackers] != null);
					if(ATTACK_RANGE >= myLocation.distanceSquaredTo(enemy.location)) {
						attackers++; // increment since we can attack too
						double ratio = attackers / enemy.health;
						if(ratio > bestRatio) {
							bestRatio = ratio;
							bestTarget = enemy;
						}
					}
				}
			}
		} else {
			double bestRatio = 0;
			for(int i = enemiesIAPCount; --i >= 0; ) {
				RobotInfo enemy = enemiesIAP[i];
				Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
				int attackers = -1;
				while(alliesIAP[++attackers] != null);
				if(ATTACK_RANGE >= myLocation.distanceSquaredTo(enemy.location)) {
					attackers++; // increment since we can attack too
					double ratio = attackers / enemy.health;
					if(ratio > bestRatio) {
						bestRatio = ratio;
						bestTarget = enemy;
					}
				}
			}
			if(bestTarget == null) {
				// all possible targets can't hit us
				for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
					RobotInfo enemy = enemiesWithinSightRange[i];
					Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
					int attackers = -1;
					while(alliesIAP[++attackers] != null);
					if(ATTACK_RANGE >= myLocation.distanceSquaredTo(enemy.location)) {
						attackers++; // increment since we can attack too
						double ratio = attackers / enemy.health;
						if(ratio > bestRatio) {
							bestRatio = ratio;
							bestTarget = enemy;
						}
					}
				}
			}
		}
		return bestTarget;
	}

	public static boolean retreat(RobotInfo[] enemiesWithinSightRange) throws GameActionException {
		rc.setIndicatorString(0, "trying to retreat");
		Direction bestRetreatDir = null;
		RobotInfo currentClosestEnemy = Util.closest(enemiesWithinSightRange, myLocation);

		int bestDist = myLocation.distanceSquaredTo(currentClosestEnemy.location);
		Direction dir = currentClosestEnemy.location.directionTo(myLocation).rotateRight();
		for(int i = 8; --i >= 0; ) {
			if(!rc.canMove(dir)) {
				continue;
			}
			MapLocation retreatLocation = myLocation.add(dir);
			RobotInfo closestEnemy = Util.closest(enemiesWithinSightRange, retreatLocation);
			int dist = retreatLocation.distanceSquaredTo(closestEnemy.location);
			if (dist > bestDist) {
				bestDist = dist;
				bestRetreatDir = dir;
			}
			dir = dir.rotateLeft();
		}
		if (bestRetreatDir != null && rc.isCoreReady()) {
			rc.move(bestRetreatDir);
			return true;
		}
		return false;
	}

	private static RobotInfo[] allies;

	private static void fight(RobotInfo[] enemiesWithinSightRange) throws GameActionException {
		if (enemiesWithinSightRange.length == 0) {
			return;
		}
		allies = rc.senseNearbyRobots(SIGHT_RANGE, myTeam);

		Util.copyEnemiesInAttackPosition(myLocation, enemiesWithinSightRange, enemiesIAP);
		int enemiesIAPCount = -1;
		while(enemiesIAP[++enemiesIAPCount] != null);

		if(enemiesIAPCount == 1) {
			// only one enemy who can attack us
			RobotInfo enemy = enemiesIAP[0];
			if(TYPE.attackRadiusSquared >= myLocation.distanceSquaredTo(enemy.location)) {
				// enemy in range
				if(canWin1v1(enemy)) {
					// can win by self
					attackIfReady(enemy.location);
					rc.setIndicatorString(0, "can win the 1v1 on turn " + rc.getRoundNum());
					return;
				}
				// can't win by self
				Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
				if(alliesIAP[0] != null) {
					// i have friends, keep fighting
					attackIfReady(enemy.location);
					rc.setIndicatorString(0, "has enough friends on turn " + rc.getRoundNum());
					return;
				}
				// no friends, also can't win by self
				if(rc.getType().cooldownDelay <= 1 && enemy.weaponDelay >= 2
						&& rc.getWeaponDelay() <= enemy.weaponDelay - 1) {
					// we can shoot and leave before taking damage
					attackIfReady(enemy.location);
					rc.setIndicatorString(0, "can shoot and bounce " + rc.getRoundNum());
					return;
				}
				// we can't shoot without taking damage
				if(rc.isCoreReady()) {
					// see if we can run away
					if(retreat(enemiesWithinSightRange)) {
						// success
						return;
					}
					// couldn't run anywhere, just fight
					attackIfReady(enemy.location);
					return;
				}
				// can't move, so attack if it won't slow us down
				// don't bother attacking if it would slow us down because we came from somewhere, so we aren't stuck
				if (rc.getType().cooldownDelay <= 1) {
					attackIfReady(enemy.location);
				}
				return;
			}
			// we are outranged
			retreat(enemiesWithinSightRange);
			return;
		}
		RobotInfo bestTarget = bestTarget(enemiesWithinSightRange);
		if(enemiesIAPCount > 1) {
			// more than one enemy
			// prioritize attacking enemy with least health and most allies can also hit
			int maxAlliedAttackers = 0;
			for(int i = enemiesIAPCount; --i >= 0; ) {
				RobotInfo enemy = enemiesIAP[i];
				Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
				int attackers = -1;
				while(alliesIAP[++attackers] != null);
				if(attackers > maxAlliedAttackers) { 
					maxAlliedAttackers = attackers;
				}
			}
			// multiple enemies; fight if we have enough allies
			if (maxAlliedAttackers >= enemiesIAPCount && bestTarget != null) {
				// enough allies are in the fight.
				attackIfReady(bestTarget.location);
				return;
			}
			// not enough allies or outranged
			if (rc.isCoreReady()) {
				if (retreat(enemiesWithinSightRange)) {
					return;
				}
				// couldn't retreat
				if(bestTarget != null) {
					// there is a target
					attackIfReady(bestTarget.location);
					return;
				}
				// move into range
				Nav.goTo(enemiesIAP[0].location);
				return;
			}
			// can't move, so attack if it won't slow us down
			if(bestTarget != null) {
				attackIfReady(bestTarget.location);
				return;
			}
			// outranged, but can't move in range, so don't do anything
			return;
		}
		// enemiesIAPCount == 0
		// shoot if possible
		if(bestTarget != null) {
			// shoot someone if there is someone to shoot
			attackIfReady(bestTarget.location);
			return;
		}

		// not in range; move somwwhere
		if(rc.isCoreReady()) {
			// help a friend
			RobotInfo closestEnemy = Util.closest(enemiesWithinSightRange, myLocation);
			if(ATTACK_RANGE >= closestEnemy.type.attackRadiusSquared) {
				// enemy does not outrange us
				Util.copyAlliesInAttackPosition(closestEnemy.location, allies, alliesIAP);
				int alliesIAPCount = -1;
				while(alliesIAP[++alliesIAPCount] != null);
				if(alliesIAPCount > 0) {
					// initiate team fight; only engage if you have at least as many friends as allies
					if(tryToInitiateTeamFight(closestEnemy.location, alliesIAPCount, enemiesWithinSightRange)) {
						return;
					}
				} else {
					// no one is fighting this enemy, but we can initiate 1v1 if it's safe
					if(canWin1v1AfterMovingTo(myLocation.add(myLocation.directionTo(closestEnemy.location)), closestEnemy) && safe(myLocation.add(myLocation.directionTo(closestEnemy.location)), enemiesWithinSightRange)) {
						if (tryToInitiate1v1(closestEnemy.location, enemiesWithinSightRange)) {
							// found an enemy to 1v1
							return;
						}
					}
				}
			}

			// no armed enemies to fight, try to kill nonattacking units
			if (tryToFightNonattackingRobot(enemiesWithinSightRange)) {
				return;
			}

			return;
		}

		// core is not ready
		return;
	}
	private static boolean tryToInitiate1v1(MapLocation loc, RobotInfo[] enemiesWithinSightRange)
		throws GameActionException {
			rc.setIndicatorString(1, "initiate 1v1");
			Direction dir = myLocation.directionTo(loc).rotateLeft();
			for(int i = 3; --i >= 0; ) {
				if(!rc.canMove(dir)) continue;
				MapLocation moveLoc = myLocation.add(dir);
				if(ATTACK_RANGE < moveLoc.distanceSquaredTo(loc)) {
					continue;
				}
				Util.copyAlliesInAttackPosition(moveLoc, enemiesWithinSightRange, enemiesIAP);
				int enemiesIAPCount = -1;
				while(enemiesIAP[++enemiesIAPCount] != null);
				if(enemiesIAPCount <= 1) {
					rc.move(dir);
					return true;
				}
				dir = dir.rotateRight();
			}
			return false;
		}

	private static boolean tryToInitiateTeamFight(MapLocation loc, int numberOfAllies, RobotInfo[] enemiesWithinSightRange)
		throws GameActionException {
			rc.setIndicatorString(1, "initiate fight");
			Direction dir = myLocation.directionTo(loc).rotateLeft();
			for(int i = 3; --i >= 0; ) {
				if(!rc.canMove(dir)) continue;
				MapLocation moveLoc = myLocation.add(dir);
				if(ATTACK_RANGE < moveLoc.distanceSquaredTo(loc)) {
					continue;
				}
				Util.copyAlliesInAttackPosition(moveLoc, enemiesWithinSightRange, enemiesIAP);
				int enemiesIAPCount = -1;
				while(enemiesIAP[++enemiesIAPCount] != null);
				if(enemiesIAPCount <= numberOfAllies) {
					rc.move(dir);
					return true;
				}
				dir = dir.rotateRight();
			}
			return false;
		}
	private static boolean tryToFightNonattackingRobot(RobotInfo[] enemiesWithinSightRange) throws GameActionException {
		rc.setIndicatorString(1, "CHASE");
		for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
			RobotInfo enemy = enemiesWithinSightRange[i];
			if(Util.isNotAttacker(enemy.type)) {
				if(safe(myLocation.add(myLocation.directionTo(enemy.location)), enemiesWithinSightRange) && rc.isCoreReady()) {
					Nav.goTo(enemy.location, safetyPolicy);
					return true;
				}
			}
		}
		return false;
	}

	private static void attackIfReady(MapLocation loc) throws GameActionException {
		rc.setIndicatorString(1, "ATTACK");
		// if ready, attack
		if (rc.isWeaponReady()) {
			rc.attackLocation(loc);
		}
	}

	private static RobotInfo[] enemiesWithinSightRange;
	private static int turnsSinceLastAttack = 100;
	static MapLocation target = null;

	public static void action() throws GameActionException {
		enemiesWithinSightRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		RobotInfo[] alliesWithinSightRange = rc.senseNearbyRobots(SIGHT_RANGE, myTeam);
		safetyPolicy = new SPCombat(enemiesWithinSightRange);

		if(enemiesWithinSightRange.length >= 2 && alliesWithinSightRange.length >= 1) { 
			// only suicide if onstensibly in combat
			if(Util.likelyToBecomeZombie(INFO, enemiesWithinSightRange)) {
				rc.setIndicatorString(1, "likelyToBecomeZombie");
				if(rc.isCoreReady()) {
					for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
						if(TYPE.cooldownDelay == 1 && enemiesWithinSightRange[i].team == enemyTeam) {
							Nav.goTo(enemiesWithinSightRange[i].location);
							if(!rc.isCoreReady()) {
								break;
							}
						}
					}
				}
			}
		}

		if(enemiesWithinSightRange.length > 0) {
			fight(enemiesWithinSightRange);
			turnsSinceLastAttack = 0;
		}

		turnsSinceLastAttack++;

		if (rc.isCoreReady()) {
			if(target != null) {
				Nav.goTo(target, safetyPolicy);
			} else {

			}
		}

		// not ready; if possible, kite shorter range units
		if(rc.isCoreReady()) {
			for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
				if(TYPE.cooldownDelay == 1 && enemiesWithinSightRange[i].attackPower > 0 && enemiesWithinSightRange[i].type.attackRadiusSquared <= ATTACK_RANGE) {
					Nav.goTo(myLocation.add(enemiesWithinSightRange[i].location.directionTo(myLocation)));
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}

		// move away from friends who might become zombies
		if(rc.isCoreReady()) {
			for(int i = alliesWithinSightRange.length; --i >= 0; ) {
				if(Util.likelyToBecomeZombie(alliesWithinSightRange[i], enemiesWithinSightRange)) {
					Nav.goTo(myLocation.add(alliesWithinSightRange[i].location.directionTo(myLocation)));
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}


		// move closer to zombie dens, archons for maximum damage output, turret for minimum damage received
		if(rc.isCoreReady()) {
			for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
				if(enemiesWithinSightRange[i].type == RobotType.ZOMBIEDEN || enemiesWithinSightRange[i].type == RobotType.ARCHON
						|| enemiesWithinSightRange[i].type == RobotType.TTM || enemiesWithinSightRange[i].type == RobotType.TURRET) {
					Nav.goTo(myLocation.add(enemiesWithinSightRange[i].location.directionTo(myLocation)), safetyPolicy);
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}


		if(turnsSinceLastAttack >= 4 && rc.isCoreReady() /*&& (rc.getRoundNum() - ID) % 7 == 0*/) {
			// System.out.println("trying to move randomly");
			int rot = (int)(Math.random() * 8);
			Direction dirToMove = Direction.EAST;
			for (int i = 0; i < rot; ++i)
				dirToMove = dirToMove.rotateLeft();

			for (int i = 0; i < 8; ++i) {
				if (rc.isCoreReady()) {
					Nav.goTo(myLocation.add(dirToMove), safetyPolicy);
					break;
				}

				dirToMove = dirToMove.rotateLeft();
			}
		}

		if(turnsSinceLastAttack >= 4 && rc.isCoreReady()) {
			// if idle, clear some rubble
			Direction dirToClear = Direction.EAST;
			for(int k = 0; k < 8; ++k) {
				MapLocation target = myLocation.add(dirToClear);
				if(rc.isCoreReady() && rc.senseRubble(target) >= GameConstants.RUBBLE_SLOW_THRESH) {
					rc.clearRubble(dirToClear);
					// System.out.println("looking for rubble to clear");
				}
			}
		}
	}
}
