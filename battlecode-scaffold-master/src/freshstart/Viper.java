package freshstart;

import battlecode.common.*;
import java.util.*;

public class Viper extends Bot {
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
		// things that run for the first time
		personalHQ = rc.getLocation();
		defendQueue = new MyQueue<Integer>();
		moveQueue = new MyQueue<MapLocation>();
		Radio.broadcastInitialStrategyRequest(10);
		// MapLocation[] initialEnemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
		if(rc.getRoundNum() > 600) {
			MapLocation[] initialEnemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
			for(int i = 0; i < initialEnemyArchonLocations.length; ++i) {
//				 moveQueue.add(initialEnemyArchonLocations[i]);
			}
		}
	}

	private static MapLocation defendLocation = null;
	private static MapLocation attackLocation = null;
	private static int turnsSinceLastAttack = 100;
	private static void action() throws GameActionException {
		// take my turn
		processSignals();
		RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, Team.ZOMBIE);
		if (enemiesWithinRange.length > 1) {
			// Check if weapon is ready
			for(int i = enemiesWithinRange.length; --i >= 0; ) {
				if(enemiesWithinRange[i].viperInfectedTurns < 5) {
					if (rc.isWeaponReady()) {
						rc.attackLocation(enemiesWithinRange[i].location);
						turnsSinceLastAttack = 0;
					}
				}
			}
			if (rc.isWeaponReady()) {
				rc.attackLocation(enemiesWithinRange[enemiesWithinRange.length - 1].location);
				turnsSinceLastAttack = 0;
			}
		} else if (enemiesWithinRange.length == 1 && enemiesWithinRange[0].type == RobotType.ARCHON) {
			if (rc.isWeaponReady()) {
				rc.attackLocation(enemiesWithinRange[enemiesWithinRange.length - 1].location);
				turnsSinceLastAttack = 0;
			}
		} else if (zombiesWithinRange.length > 0) {
			// Check if weapon is ready
			for(int i = zombiesWithinRange.length; --i >= 0; ) {
				// if(zombiesWithinRange[i].viperInfectedTurns < 5) {
					if (rc.isWeaponReady()) {
						rc.attackLocation(zombiesWithinRange[i].location);
						turnsSinceLastAttack = 0;
					}
				// }
			}
			if (rc.isWeaponReady()) {
				rc.attackLocation(zombiesWithinRange[0].location);
				turnsSinceLastAttack = 0;
			}
		}
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
				if(turnsSinceLastAttack >= 2) {
					moveSomewhere();
				}
				break;
			default:
				break;
		}

		if(rc.isCoreReady()) {
			RobotInfo[] immediateHostile = rc.senseHostileRobots(myLocation, 4);
			for(int i = immediateHostile.length; --i >= 0; ) {
				if(immediateHostile[i].type == RobotType.STANDARDZOMBIE || immediateHostile[i].type == RobotType.BIGZOMBIE
						|| immediateHostile[i].type == RobotType.FASTZOMBIE || immediateHostile[i].type == RobotType.GUARD) {
					Nav.goTo(myLocation.add(immediateHostile[i].location.directionTo(myLocation)));
					if(!rc.isCoreReady()) {
						break;
					}
				}
			}
		}

		RobotInfo[] enemiesWithinSightRange = rc.senseNearbyRobots(SIGHT_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinSightRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.ZOMBIE);
		// move closer
		if (zombiesWithinSightRange.length > 0) {
			if(rc.isCoreReady()) {
				Nav.goTo(zombiesWithinSightRange[0].location);
			}
		} else if (enemiesWithinSightRange.length > 0) {
			if(rc.isCoreReady()) {
				Nav.goTo(enemiesWithinSightRange[0].location);
			}
		}

		if(rc.getRoundNum() == 600) {
			MapLocation[] initialEnemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
			for(int i = 0; i < initialEnemyArchonLocations.length; ++i) {
				 moveQueue.add(initialEnemyArchonLocations[i]);
			}
		}
				if(turnsSinceLastAttack >= 2) {
		if (rc.isCoreReady()) {
			int rot = (int)(Math.random() * 8);
			Direction dirToMove = Direction.EAST;
			for (int i = 0; i < rot; ++i)
				dirToMove = dirToMove.rotateLeft();

			for (int i = 0; i < 8; ++i) {
				if (rc.canMove(dirToMove)) {
					rc.move(dirToMove); break;
				}

				dirToMove = dirToMove.rotateLeft();
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
		IdAndMapLocation newDefend = null, newMove = null; int clearDefend = -1;
		newDefend = Radio.getDefendLocation(); newMove = Radio.getMoveLocation(); clearDefend = Radio.getClearDefend();
        IdAndMapLocation newHQ = Radio.getMoveCampLocation();                            
        if(newHQ != null) {
            personalHQ = newHQ.location;
        }
		while(newDefend != null) {
			if(teamMemberNeedsHelp[newDefend.id] == 0) {
				defendQueue.add(newDefend.id);
				teamMemberNeedsHelp[newDefend.id] = rc.getRoundNum();
			}
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
	}

	private static void moveSomewhere() throws GameActionException {
		while(!defendQueue.isEmpty()) {
			int next = defendQueue.element();
			if(teamMemberNeedsHelp[next] > 0 && rc.getRoundNum() - teamMemberNeedsHelp[next] < 200) {
				if(rc.isCoreReady()) {
					Nav.goTo(teamLocations[next]);
				}
				return;
			}
			defendQueue.remove();
		}
		if(!moveQueue.isEmpty()) {
			MapLocation next = moveQueue.element();
			if(rc.isCoreReady()) {
				Nav.goTo(next);
			}
			if(rc.canSense(next) && rc.senseRobotAtLocation(next) == null) {
				moveQueue.remove();
			}
			return;
		}
        if(rc.isCoreReady()) {
            Nav.goTo(personalHQ);
            return;
        }
	}
}
