package attacktest;

import battlecode.common.*;
import java.util.*;

public class Soldier extends Bot {
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			updateHealth();
			Radio.process();
			myLocation = rc.getLocation();
			action();
			Radio.clear();
			Clock.yield();
		}
	}

	private static void init() throws GameActionException {
		// initializes soldier
		personalHQ = rc.getLocation();
		defendQueue = new MyQueue<Integer>();
		moveQueue = new MyQueue<MapLocation>();
		Radio.broadcastInitialStrategyRequest(10);
/*		if(rc.getRoundNum() > 600) {
			MapLocation[] initialEnemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
			for(int i = 0; i < initialEnemyArchonLocations.length; ++i) {
				 moveQueue.add(initialEnemyArchonLocations[i]);
			}
		}
		*/
	}

	private static SafetyPolicy policy;
	private static MapLocation defendLocation = null;
	private static MapLocation attackLocation = null;
	private static int turnsSinceLastAttack = 100;
	private static void action() throws GameActionException {
		// takes turn in following order:
		//     processes signals
		//     looks for enemies and zombies to attack
		policy = new SPShort(rc.senseHostileRobots(myLocation, 100000));
		processSignals();
		RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, Team.ZOMBIE);
		MapLocation toAttack = attackLocation(zombiesWithinRange, enemiesWithinRange, new MapLocation[0]);
		if(toAttack != null && rc.isWeaponReady()) {
			rc.attackLocation(toAttack);
			turnsSinceLastAttack = 0;
		}
		/*
		if (enemiesWithinRange.length > 0) {
			// Check if weapon is ready
			if (rc.isWeaponReady()) {
				rc.attackLocation(enemiesWithinRange[0].location);
				turnsSinceLastAttack = 0;
			}
		} else if (zombiesWithinRange.length > 0) {
			// Check if weapon is ready
			if (rc.isWeaponReady()) {
				rc.attackLocation(zombiesWithinRange[0].location);
				turnsSinceLastAttack = 0;
			}
		}
		*/

		RobotInfo[] enemiesWithinSightRange = rc.senseNearbyRobots(SIGHT_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinSightRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.ZOMBIE);
		switch(strategy) {
			case -1:
				int channel = Radio.getTuneCommand();
				if(channel == 30) {
					strategy = Radio.getStrategyAssignment();
				}
				break;
			case 0:
				break;
			case 1:
				if(turnsSinceLastAttack >= 4) {
					moveSomewhere();
				}
				break;
			default:
				break;
		}

		double maxHealth = RobotType.SOLDIER.maxHealth;
		if(rc.isCoreReady() && rc.getInfectedTurns() != 0 && rc.getHealth() < 0.5 * maxHealth) {
			RobotInfo[] surroundingHostile = rc.senseNearbyRobots(SIGHT_RANGE, enemyTeam);
			if(surroundingHostile.length != 0) {
				Nav.goTo(surroundingHostile[0].location);
			} else {
				Nav.goTo(rc.getInitialArchonLocations(enemyTeam)[0]);
			}
		}


		// kite
		if(rc.isCoreReady()) {
			RobotInfo[] immediateHostile = rc.senseHostileRobots(myLocation, 4);
			for(int i = immediateHostile.length; --i >= 0; ) {
				if(immediateHostile[i].type == RobotType.STANDARDZOMBIE || immediateHostile[i].type == RobotType.BIGZOMBIE
						|| immediateHostile[i].type == RobotType.FASTZOMBIE || immediateHostile[i].type == RobotType.GUARD) {
					Nav.goTo(myLocation.add(immediateHostile[i].location.directionTo(myLocation)), policy);
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}

		// move closer
		if(rc.isCoreReady()) {
			RobotInfo[] immediateHostile = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
			for(int i = immediateHostile.length; --i >= 0; ) {
				if(immediateHostile[i].type == RobotType.ARCHON || immediateHostile[i].type == RobotType.ZOMBIEDEN
						|| immediateHostile[i].type == RobotType.TTM || immediateHostile[i].type == RobotType.TURRET || immediateHostile[i].type == RobotType.SCOUT
						|| immediateHostile[i].type == RobotType.RANGEDZOMBIE || immediateHostile[i].type == RobotType.SOLDIER || immediateHostile[i].type == RobotType.VIPER) {
					Nav.goTo(immediateHostile[i].location, policy);
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}

		// move closer
		/*
		if (enemiesWithinSightRange.length > 0) {
			if(rc.isCoreReady()) {
				Nav.goTo(enemiesWithinSightRange[0].location);
			}
		} else if (zombiesWithinSightRange.length > 0) {
			if(rc.isCoreReady()) {
				Nav.goTo(zombiesWithinSightRange[0].location);
			}
		}
		*/

/*
		if(rc.getRoundNum() == 600) {
			MapLocation[] initialEnemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
			for(int i = 0; i < initialEnemyArchonLocations.length; ++i) {
				 moveQueue.add(initialEnemyArchonLocations[i]);
			}
		}
*/
		if(turnsSinceLastAttack >= 4) {
			if(rc.getRoundNum() % 5 != 3) {
				tighten(policy);
			}
			if (rc.getRoundNum() % 5 == 3 && rc.isCoreReady()) {
				int rot = (int)(Math.random() * 8);
				Direction dirToMove = Direction.EAST;
				for (int i = 0; i < rot; ++i)
					dirToMove = dirToMove.rotateLeft();

				for (int i = 0; i < 8; ++i) {
					if (rc.isCoreReady()) {
						Nav.goTo(myLocation.add(dirToMove), policy);
						break;
					}

					dirToMove = dirToMove.rotateLeft();
				}
			}
		}
		if(rc.isCoreReady()) {
			// if idle, clear some rubble
			Direction dirToClear = Direction.EAST;
			for(int k = 0; k < 8; ++k) {
				MapLocation target = myLocation.add(dirToClear);
				if(rc.isCoreReady() && rc.senseRubble(target) >= GameConstants.RUBBLE_SLOW_THRESH) {
					rc.clearRubble(dirToClear);
//					System.out.println("looking for rubble to clear");
				}
			}
		}
		turnsSinceLastAttack++;
	}

	private static MyQueue<Integer> defendQueue;
	private static MyQueue<MapLocation> moveQueue;
	private static MapLocation[] teamLocations = new MapLocation[32001];
	private static int[] teamMemberNeedsHelp = new int[32001]; // store what turn request was made

	private static void processSignals() throws GameActionException {
		IdAndMapLocation newDefend = null, newMove = null; int clearDefend = -1; int clearOrders = -1;
		newDefend = Radio.getDefendLocation(); newMove = Radio.getMoveLocation(); clearDefend = Radio.getClearDefend(); clearOrders = Radio.getClear();
		IdAndMapLocation newHQ = Radio.getMoveCampLocation();
		if(newHQ != null) {
			personalHQ = newHQ.location;
		}
		while(newDefend != null) {
			if(teamMemberNeedsHelp[newDefend.id] == 0) {
				defendQueue.add(newDefend.id);
			}
			teamMemberNeedsHelp[newDefend.id] = rc.getRoundNum();
			teamLocations[newDefend.id] = newDefend.location;
			newDefend = Radio.getDefendLocation();
		}
		while(newMove != null) {
			moveQueue.add(newMove.location);
			newMove = Radio.getMoveLocation();
		}
		while(clearDefend != -1) {
			teamMemberNeedsHelp[clearDefend] = 0;
			clearDefend = Radio.getClearDefend();
		}
		while(clearOrders != -1) {
			break; // not implemented yet
		}
	}

	private static void moveSomewhere() throws GameActionException {
		if(strategy == 0) {
			while(!defendQueue.isEmpty()) {
				int next = defendQueue.element();
				if(teamMemberNeedsHelp[next] > 0 /* && rc.getRoundNum() - teamMemberNeedsHelp[next] < 200 */ ) {
					if(rc.isCoreReady()) {
						Nav.goTo(teamLocations[next], policy);
					}
					return;
				}
				defendQueue.remove();
			}
		}
		if(!moveQueue.isEmpty()) {
			MapLocation next = moveQueue.element();
			if(rc.isCoreReady()) {
				Nav.goTo(next, policy);
			}
			if(rc.canSense(next) && rc.senseRobotAtLocation(next) == null) {
				moveQueue.remove();
			}
			return;
		}
		if(rc.isCoreReady()) {
			Nav.goTo(personalHQ, policy);
			return;
		}
	}
}
