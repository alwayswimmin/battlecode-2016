package team074;

import battlecode.common.*;
import java.util.*;

// Combat contains methods of action in the presense of enemy units

class SPHarass extends Bot implements SafetyPolicy {
	RobotInfo[] enemies;

	public SPHarass(RobotInfo[] enemies) {
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
		RobotInfo loneAttacker = null;
		int numAttackers = 0;
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
				if(numAttackers >= 2) {
					return false;
				}
				loneAttacker = enemy;
			}
		}
		if (numAttackers == 0) {
			return true;
		}
		return canWin1v1AfterMovingTo(loc, loneAttacker);
	}

	public static boolean retreat(RobotInfo[] enemiesWithinSightRange) throws GameActionException {
		// rc.setIndicatorString(0, "trying to retreat");
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

	private static void fight(RobotInfo[] enemiesWithinSightRange) throws GameActionException {
		if (enemiesWithinSightRange.length == 0) {
			return;
		}

		RobotInfo[] allies = rc.senseNearbyRobots(SIGHT_RANGE, myTeam);
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
					return;
				}
				// can't win by self
				Util.copyAlliesInAttackPosition(enemy.location, allies, alliesIAP);
				if(alliesIAP[0] != null) {
					// i have friends, keep fighting
					attackIfReady(enemy.location);
					return;
				}
				// no friends, also can't win by self
				if(rc.getType().cooldownDelay <= 1 && enemy.weaponDelay >= 2
							&& rc.getWeaponDelay() <= enemy.weaponDelay - 1) {
						// we can shoot and leave before taking damage
						attackIfReady(enemy.location);
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
		if(enemiesIAPCount > 1) {
		// more than one enemy
		// prioritize attacking enemy with least health and most allies can also hit
		RobotInfo bestTarget = null;
		double bestRatio = 0;
		int maxAlliedAttackers = 0;
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

		RobotInfo bestTarget = null;
		double bestRatio = 0;
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
			Direction dir = myLocation.directionTo(loc).rotateLeft();
			for(int i = 3; --i >= 0; ) {
				if(!rc.canMove(dir)) continue;
				MapLocation moveLoc = myLocation.add(dir);
				if(ATTACK_RANGE < moveLoc.distanceSquaredTo(loc)) continue; // must engage in one turn

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
			Direction dir = myLocation.directionTo(loc).rotateLeft();
			for(int i = 3; --i >= 0; ) {
				if(!rc.canMove(dir)) continue;
				MapLocation moveLoc = myLocation.add(dir);
				if(ATTACK_RANGE < moveLoc.distanceSquaredTo(loc)) continue; // must engage in one turn

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
		if (rc.isWeaponReady()) {
			rc.attackLocation(loc);
		}
	}

	static boolean firstTurn = true;
	private static int turnsSinceLastAttack = 100;
	static MapLocation target = null;

	public static void action() throws GameActionException {
		if (firstTurn) {
			firstTurn = false;
		}

		RobotInfo[] enemiesWithinSightRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		safetyPolicy = new SPHarass(enemiesWithinSightRange);

		if(enemiesWithinSightRange.length > 0) {
			fight(enemiesWithinSightRange);
			turnsSinceLastAttack = 0;
		}

		// kite short range units
		if(rc.isCoreReady()) {
			for(int i = enemiesWithinSightRange.length; --i >= 0; ) {
				if(enemiesWithinSightRange[i].type == RobotType.STANDARDZOMBIE || enemiesWithinSightRange[i].type == RobotType.BIGZOMBIE
						|| enemiesWithinSightRange[i].type == RobotType.FASTZOMBIE || enemiesWithinSightRange[i].type == RobotType.GUARD) {
					Nav.goTo(myLocation.add(enemiesWithinSightRange[i].location.directionTo(myLocation)), safetyPolicy);
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

		turnsSinceLastAttack++;

		if (rc.isCoreReady()) {
			if(target != null) {
				Nav.goTo(target, safetyPolicy);
			} else {

			}
		}

		if(turnsSinceLastAttack >= 4 && rc.isCoreReady() && (rc.getRoundNum() - ID) % 7 == 0) {
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
