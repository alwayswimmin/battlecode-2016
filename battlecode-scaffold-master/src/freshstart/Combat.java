package freshstart;

import battlecode.common.*;
import java.util.*;


class SPHarass extends Bot implements SafetyPolicy {
	RobotInfo[] nearbyEnemies;

	public SPHarass(RobotInfo[] nearbyEnemies) {
		this.nearbyEnemies = nearbyEnemies;
	}

	public boolean safe(MapLocation loc) {
		return Combat.isSafeToMoveTo(loc, nearbyEnemies);
	}
}

public class Combat extends Bot {
	private static boolean canWin1v1(RobotInfo enemy) {
		int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
		int turnsTillWeCanAttack;
		double effectiveAttackDelay;

		turnsTillWeCanAttack = (int) rc.getWeaponDelay();
		effectiveAttackDelay = rc.getType().attackDelay;

		int turnsToKillEnemy = (int) (turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy);

		int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
		int turnsTillEnemyCanAttack;
		double effectiveEnemyAttackDelay;

		turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
		effectiveEnemyAttackDelay = enemy.type.attackDelay;

		int turnsForEnemyToKillUs = (int) (turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs);

		return turnsToKillEnemy <= turnsForEnemyToKillUs;
	}

	public static boolean canWin1v1AfterMovingTo(MapLocation loc, RobotInfo enemy) {
		double cooldownDelay = rc.getType().cooldownDelay;

		int numAttacksAfterFirstToKillEnemy = (int) ((enemy.health - 0.001) / rc.getType().attackPower);
		int turnsTillWeCanAttack;
		double effectiveAttackDelay;

		double weaponDelayAfterMoving = Math.max(cooldownDelay, rc.getWeaponDelay()) - 1.0;
		turnsTillWeCanAttack = 1 + (int) weaponDelayAfterMoving;
		effectiveAttackDelay = rc.getType().attackDelay;

		int turnsToKillEnemy = (int) (turnsTillWeCanAttack + effectiveAttackDelay * numAttacksAfterFirstToKillEnemy);

		int numAttacksAfterFirstForEnemyToKillUs = (int) ((rc.getHealth() - 0.001) / enemy.type.attackPower);
		int turnsTillEnemyCanAttack;
		double effectiveEnemyAttackDelay;
		turnsTillEnemyCanAttack = (int) Math.max(0, enemy.weaponDelay - 1);
		effectiveEnemyAttackDelay = enemy.type.attackDelay;
		int turnsForEnemyToKillUs = (int) (turnsTillEnemyCanAttack + effectiveEnemyAttackDelay * numAttacksAfterFirstForEnemyToKillUs);

		return turnsToKillEnemy <= turnsForEnemyToKillUs;
	}	


	public static boolean isSafeToMoveTo(MapLocation loc, RobotInfo[] nearbyEnemies) {
		RobotInfo loneAttacker = null;
		int numAttackers = 0;
		for (RobotInfo enemy : nearbyEnemies) {
			if(enemy.type == RobotType.ZOMBIEDEN && enemy.location.isAdjacentTo(loc)) {
				return false;
			}
			if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
				numAttackers++;
				if (numAttackers >= 2) {
					return false;
				}
				loneAttacker = enemy;
			}
		}

		if (numAttackers == 0) return true;

		return canWin1v1AfterMovingTo(loc, loneAttacker);
	}

	private static boolean tryToRetreat(RobotInfo[] nearbyEnemies) throws GameActionException {
		rc.setIndicatorString(0, "trying to retreat");
		Direction bestRetreatDir = null;
		RobotInfo currentClosestEnemy = Util.closest(nearbyEnemies, myLocation);

		int bestDistSq = myLocation.distanceSquaredTo(currentClosestEnemy.location);
		for (Direction dir : Direction.values()) {
			if (!rc.canMove(dir)) continue;

			MapLocation retreatLoc = myLocation.add(dir);

			RobotInfo closestEnemy = Util.closest(nearbyEnemies, retreatLoc);
			int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
			if (distSq > bestDistSq) {
				bestDistSq = distSq;
				bestRetreatDir = dir;
			}
		}

		if (bestRetreatDir != null && rc.isCoreReady()) {
			rc.move(bestRetreatDir);
			return true;
		}
		return false;
	}

	private static boolean doMicro(RobotInfo[] nearbyEnemies, boolean shadowEnemyHarassers) throws GameActionException {
		if (nearbyEnemies.length == 0) {
			RobotInfo[] moreEnemies = rc.senseNearbyRobots(63, enemyTeam);
			if (moreEnemies.length == 0) {
				// Debug.indicate("micro", 0, "no enemies, no micro");
				return false;
			} else {
				RobotInfo closestEnemy = Util.closest(moreEnemies, myLocation);
				if (closestEnemy != null && isHarasser(closestEnemy.type) && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
					// Debug.indicate("micro", 0, "no nearby enemies, shadowing an enemy at long range");
					if (rc.isCoreReady()) {
						shadowHarasser(closestEnemy, nearbyEnemies);
					}
					return true;
				}
			}
		}

		int numEnemiesAttackingUs = 0;
		RobotInfo[] enemiesAttackingUs = new RobotInfo[99];
		for (RobotInfo enemy : nearbyEnemies) {
			if (enemy.type.attackRadiusSquared >= myLocation.distanceSquaredTo(enemy.location)) {
				enemiesAttackingUs[numEnemiesAttackingUs++] = enemy;
			}
		}

		// TODO: below cases don't handle missiles very well
		// TODO: possible also don't handle launchers well

		if (numEnemiesAttackingUs > 0) {
			// we are in combat
			if (numEnemiesAttackingUs == 1) {
				// we are in a 1v1
				RobotInfo loneAttacker = enemiesAttackingUs[0];
				if (rc.getType().attackRadiusSquared >= myLocation.distanceSquaredTo(loneAttacker.location)) {
					// we can actually shoot at the enemy we are 1v1ing
					if (canWin1v1(loneAttacker)) {
						rc.setIndicatorString(0, "Can win 1v1 on turn " + rc.getRoundNum() + " at position " + loneAttacker.location.x + ", " + loneAttacker.location.y);
						// we can beat the other guy 1v1. fire away!
						// Debug.indicate("micro", 0, "winning 1v1");
						attackIfReady(loneAttacker.location);
						return true;
					} else {
						// check if we actually have some allied support. if so we can keep fighting
						boolean haveSupport = false;
						for (int i = 0; i < numEnemiesAttackingUs; i++) {
							if (numOtherAlliesInAttackRange(enemiesAttackingUs[i].location) > 0) {
								haveSupport = true;
								break;
							}
						}
						if (haveSupport) {
							rc.setIndicatorString(0, "I have allied support on turn " + rc.getRoundNum());
							// an ally is helping us, so keep fighting the lone enemy
							// Debug.indicate("micro", 0, "losing 1v1 but we have support");
							attackIfReady(loneAttacker.location);
							return true;
						} else {
							// we can't win the 1v1.
							if (rc.getType().cooldownDelay <= 1 && loneAttacker.weaponDelay >= 2
									&& rc.getWeaponDelay() <= loneAttacker.weaponDelay - 1) {
								// we can get a shot off and retreat before the enemy can fire at us again, so do that
								// Debug.indicate("micro", 0, "firing one last shot before leaving losing 1v1");
								attackIfReady(loneAttacker.location);
								return true;
							} else {
								// we can't get another shot off. run away!
								if (rc.isCoreReady()) {
									// we can move this turn
									if (tryToRetreat(nearbyEnemies)) {
										// we moved away
										// Debug.indicate("micro", 0, "retreated");
										return true;
									} else {
										// we couldn't find anywhere to retreat to. fire a desperate shot if possible
										// Debug.indicate("micro", 0, "couldn't find anywhere to retreat! trying to shoot");
										attackIfReady(loneAttacker.location);
										return true;
									}
								} else {
									// we can't move this turn. if it won't delay retreating, shoot instead
									// Debug.indicate("micro", 0, "want to retreat but core isn't ready; trying to shoot if cooldown <= 1");
									if (rc.getType().cooldownDelay <= 1) {
										attackIfReady(loneAttacker.location);
									}
									return true;
								}
							}
						}
					}
				} else {
					// we are getting shot by someone who outranges us. run away!
					// Debug.indicate("micro", 0, "trying to retreat from a 1v1 where we are outranged");
					tryToRetreat(nearbyEnemies);
					return true;
				}
			} else {
				RobotInfo bestTarget = null;
				double bestTargetingMetric = 0;
				int maxAlliesAttackingAnEnemy = 0;
				for (int i = 0; i < numEnemiesAttackingUs; i++) {
					RobotInfo enemy = enemiesAttackingUs[i];
					int numAlliesAttackingEnemy = 1 + numOtherAlliesInAttackRange(enemy.location);
					if (numAlliesAttackingEnemy > maxAlliesAttackingAnEnemy) maxAlliesAttackingAnEnemy = numAlliesAttackingEnemy;
					if (rc.getType().attackRadiusSquared >= myLocation.distanceSquaredTo(enemy.location)) {
						double targetingMetric = numAlliesAttackingEnemy / enemy.health;
						if (targetingMetric > bestTargetingMetric) {
							bestTargetingMetric = targetingMetric;
							bestTarget = enemy;
						}
					}
				}

				// multiple enemies are attacking us. stay in the fight iff enough allies are also engaged
				if (maxAlliesAttackingAnEnemy >= numEnemiesAttackingUs && bestTarget != null) {
					// enough allies are in the fight.
					// Debug.indicate("micro", 0, "attacking because numEnemiesAttackingUs = " + numEnemiesAttackingUs + ", maxAlliesAttackingEnemy = "
					// + maxAlliesAttackingAnEnemy);
					attackIfReady(bestTarget.location);
					return true;
				} else {
					// not enough allies are in the fight. we need to retreat
					// or too far to fire at anywhere
					if (rc.isCoreReady()) {
						// we can move this turn
						if (tryToRetreat(nearbyEnemies)) {
							// we moved away
							// Debug.indicate("micro", 0, "retreated because numEnemiesAttackingUs = " + numEnemiesAttackingUs + ", maxAlliesAttackingEnemy = "
							// + maxAlliesAttackingAnEnemy);
							return true;
						} else {
							// we couldn't find anywhere to retreat to. fire a desperate shot if possible
							// Debug.indicate("micro", 0, "no retreat square :( numEnemiesAttackingUs = " + numEnemiesAttackingUs +
							// ", maxAlliesAttackingEnemy = "
							// + maxAlliesAttackingAnEnemy);
							if(bestTarget != null) {
								attackIfReady(bestTarget.location);
								return true;
							} else {
								// this part added 
								// can't move away, try to move closer
								if(rc.isCoreReady()) {
									Nav.goTo(enemiesAttackingUs[0].location);
								}
								return !rc.isCoreReady();
							}
						}
					} else {
						// we can't move this turn. if it won't delay retreating, shoot instead
						// Debug.indicate("micro", 0, "want to retreat but core on cooldown :( numEnemiesAttackingUs = " + numEnemiesAttackingUs
						// + ", maxAlliesAttackingEnemy = " + maxAlliesAttackingAnEnemy);
						if(bestTarget != null) {
							attackIfReady(bestTarget.location);
							return true;
						}
					}
				}
			}
		} else {
			// no one is shooting at us. if we can shoot at someone, do so
			RobotInfo bestTarget = null;
			double minHealth = 1e99;
			for (RobotInfo enemy : nearbyEnemies) {
				if (rc.getType().attackRadiusSquared >= myLocation.distanceSquaredTo(enemy.location)) {
					if (enemy.health < minHealth) {
						minHealth = enemy.health;
						bestTarget = enemy;
					}
				}
			}

			// shoot someone if there is someone to shoot
			if (bestTarget != null) {
				// Debug.indicate("micro", 0, "shooting an enemy while no one can shoot us");
				attackIfReady(bestTarget.location);
				return true;
			}

			// we can't shoot anyone

			if (rc.isCoreReady()) { // all remaining possible actions are movements
				// check if we can move to help an ally who has already engaged a nearby enemy
				RobotInfo closestEnemy = Util.closest(nearbyEnemies, myLocation);
				// we can only think about engage enemies with equal or shorter range, and we shouldn't try to engage missiles
				if (closestEnemy != null && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
					int numAlliesFightingEnemy = numOtherAlliesInAttackRange(closestEnemy.location);

					if (numAlliesFightingEnemy > 0) {
						// see if we can assist our ally(s)
						int maxEnemyExposure = numAlliesFightingEnemy;
						if (tryMoveTowardLocationWithMaxEnemyExposure(closestEnemy.location, maxEnemyExposure, nearbyEnemies)) {
							// Debug.indicate("micro", 0, "moved to assist allies against " + closestEnemy.location.toString() + "; maxEnemyExposure = "
							// + maxEnemyExposure);
							return true;
						}
					} else {
						// no one is fighting this enemy, but we can try to engage them if we can win the 1v1
						if (canWin1v1AfterMovingTo(myLocation.add(myLocation.directionTo(closestEnemy.location)), closestEnemy)) {
							int maxEnemyExposure = 1;
							if (tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(closestEnemy.location, maxEnemyExposure, nearbyEnemies)) {
								// Debug.indicate("micro", 0, "moved to engage enemy we can 1v1");
								return true;
							}
						}
					}
				}

				// try to move toward and kill an enemy worker
				if (tryMoveToEngageAnyUndefendedWorkerOrBuilding(nearbyEnemies)) {
					// Debug.indicate("micro", 0, "moved to engage an undefended worker or building");
					return true;
				}

				if (shadowEnemyHarassers) {
					if (closestEnemy != null && isHarasser(closestEnemy.type) && rc.getType().attackRadiusSquared >= closestEnemy.type.attackRadiusSquared) {
						// Debug.indicate("micro", 0, "shadowing " + closestEnemy.location.toString());
						shadowHarasser(closestEnemy, nearbyEnemies);
						return true;
					}
				}

				// no required actions
				// Debug.indicate("micro", 0, "no micro action though core is ready and there are nearby enemies");
				return false;
			}

			// return true here because core is not ready, so it's as if we took a required action
			// in the sense that we can't do anything else
			// Debug.indicate("micro", 0, "no micro action; core isn't ready");
			return true;
		}
		return false;
	}
	private static boolean tryMoveToEngageEnemyAtLocationInOneTurnWithMaxEnemyExposure(MapLocation loc, int maxEnemyExposure, RobotInfo[] nearbyEnemies)
		throws GameActionException {
			Direction toLoc = myLocation.directionTo(loc);
			Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
			for (Direction dir : tryDirs) {
				if (!rc.canMove(dir)) continue;
				MapLocation moveLoc = myLocation.add(dir);
				if (rc.getType().attackRadiusSquared < moveLoc.distanceSquaredTo(loc)) continue; // must engage in one turn

				int enemyExposure = numEnemiesAttackingLocation(moveLoc, nearbyEnemies);
				if (enemyExposure <= maxEnemyExposure) {
					rc.move(dir);
					return true;
				}
			}

			return false;
		}

	private static boolean tryMoveTowardLocationWithMaxEnemyExposure(MapLocation loc, int maxEnemyExposure, RobotInfo[] nearbyEnemies)
		throws GameActionException {
			Direction toLoc = myLocation.directionTo(loc);
			Direction[] tryDirs = new Direction[] { toLoc, toLoc.rotateLeft(), toLoc.rotateRight() };
			for (Direction dir : tryDirs) {
				if (!rc.canMove(dir)) continue;
				MapLocation moveLoc = myLocation.add(dir);

				int enemyExposure = numEnemiesAttackingLocation(moveLoc, nearbyEnemies);
				if (enemyExposure <= maxEnemyExposure) {
					rc.move(dir);
					return true;
				}
			}

			return false;
		}
	private static boolean tryMoveToEngageAnyUndefendedWorkerOrBuilding(RobotInfo[] nearbyEnemies) throws GameActionException {
		for (RobotInfo enemy : nearbyEnemies) {
			if (isWorkerOrBuilding(enemy.type)) {
				if (canWin1v1(enemy)) {
					boolean canReach = true;
					MapLocation loc = myLocation;
					while (loc.distanceSquaredTo(enemy.location) > rc.getType().attackRadiusSquared) {
						Direction dir = loc.directionTo(enemy.location);
						MapLocation newLoc = loc.add(dir);

						//                        if (!rc.isPathable(rc.getType(), newLoc)) {
						//                            canReach = false;
						//                            break;
						//                        }

						boolean noOtherEnemiesAttackNewLoc = true;
						for (RobotInfo otherEnemy : nearbyEnemies) {
							if (otherEnemy != enemy
									&& otherEnemy.type.attackRadiusSquared >= newLoc.distanceSquaredTo(otherEnemy.location)) {
								noOtherEnemiesAttackNewLoc = false;
								break;
							}
						}

						if (noOtherEnemiesAttackNewLoc) {
							loc = newLoc;
						} else {
							canReach = false;
							break;
						}
					}
					if (canReach) {
						if(rc.isCoreReady()) {
							Nav.goTo(enemy.location);
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	private static void shadowHarasser(RobotInfo enemyToShadow, RobotInfo[] nearbyEnemies) throws GameActionException {
		Direction toEnemy = myLocation.directionTo(enemyToShadow.location);
		Direction[] dirs = new Direction[] { toEnemy, toEnemy.rotateRight(), toEnemy.rotateLeft(), toEnemy.rotateRight().rotateRight(),
			toEnemy.rotateLeft().rotateLeft() };
		for (Direction dir : dirs) {
			if (!rc.canMove(dir)) continue;

			MapLocation loc = myLocation.add(dir);

			boolean locIsSafe = true;

			for (RobotInfo enemy : nearbyEnemies) {
				if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
					locIsSafe = false;
					break;
				}
			}

			if (locIsSafe) {
				rc.move(dir);
				break;
			}
		}
	}

	private static int numEnemiesAttackingLocation(MapLocation loc, RobotInfo[] nearbyEnemies) {
		int ret = 0;
		for (int i = nearbyEnemies.length; i-- > 0;) {
			if (nearbyEnemies[i].type.attackRadiusSquared >= loc.distanceSquaredTo(nearbyEnemies[i].location)) ret++;
		}
		return ret;
	}

	private static int numOtherAlliesInAttackRange(MapLocation loc) {
		int ret = 0;
		RobotInfo[] allies = rc.senseNearbyRobots(loc, 15, myTeam);
		for (RobotInfo ally : allies) {
			if (ally.type.attackRadiusSquared >= loc.distanceSquaredTo(ally.location)) ret++;
		}
		return ret;
	}

	private static boolean isHarasser(RobotType rt) {
		switch (rt) {
			case SOLDIER:
			case VIPER:
			case GUARD:
			case TURRET:
				return true;

			default:
				return false;
		}
	}

	private static boolean isWorkerOrBuilding(RobotType rt) {
		switch (rt) {
			case ARCHON:
			case SCOUT:
			case ZOMBIEDEN:
			case TTM:
				return true;

			default:
				return false;
		}
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

		/*
		   RobotInfo[] allNearbyEnemies = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		   ArrayList<RobotInfo> removeDens = new ArrayList<RobotInfo>();
		   for(int i = allNearbyEnemies.length; --i >= 0; ) {
		   if(allNearbyEnemies[i].type != RobotType.ZOMBIEDEN) {
		   removeDens.add(allNearbyEnemies[i]);
		   }
		   }
		   RobotInfo[] nearbyEnemies= new RobotInfo[removeDens.size()];
		   for(int i = removeDens.size(); --i >= 0; ) {
		   nearbyEnemies[i] = removeDens.get(i);
		   }
		   SafetyPolicy safetyPolicy = new SPHarass(allNearbyEnemies);
		   */
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		SafetyPolicy safetyPolicy = new SPHarass(nearbyEnemies);

		boolean shadowEnemyHarassers = true; // myLocation.distanceSquaredTo(ourHQ) < myLocation.distanceSquaredTo(theirHQ);
		if (doMicro(nearbyEnemies, shadowEnemyHarassers)) {
			turnsSinceLastAttack = 0;
			//			return;
		}

		// kite short range units
		if(rc.isCoreReady()) {
			RobotInfo[] immediateHostile = rc.senseHostileRobots(myLocation, ATTACK_RANGE);
			for(int i = immediateHostile.length; --i >= 0; ) {
				if(immediateHostile[i].type == RobotType.STANDARDZOMBIE || immediateHostile[i].type == RobotType.BIGZOMBIE
						|| immediateHostile[i].type == RobotType.FASTZOMBIE || immediateHostile[i].type == RobotType.GUARD) {
					Nav.goTo(myLocation.add(immediateHostile[i].location.directionTo(myLocation)), safetyPolicy);
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}

		// move closer to zombie dens, archons for maximum damage output, turret for minimum damage received
		if(rc.isCoreReady()) {
			RobotInfo[] immediateHostile = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
			for(int i = immediateHostile.length; --i >= 0; ) {
				if(immediateHostile[i].type == RobotType.ZOMBIEDEN || immediateHostile[i].type == RobotType.ARCHON
						|| immediateHostile[i].type == RobotType.TTM || immediateHostile[i].type == RobotType.TURRET) {
					Nav.goTo(myLocation.add(immediateHostile[i].location.directionTo(myLocation)), safetyPolicy);
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}

		turnsSinceLastAttack++;

		rc.setIndicatorString(1, "turns since last attack: " + turnsSinceLastAttack);

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
