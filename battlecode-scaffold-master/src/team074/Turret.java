package team074;

import battlecode.common.*;

import java.util.*;

public class Turret extends Bot {
	private static Random rnd;
	private static MapLocation defendLocation, attackLocation;
	private static int radiusLimit = 4;
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			myLocation = rc.getLocation();
			Radio.process();
			action();
			Radio.clear();
			Clock.yield();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time
		personalHQ = rc.getInitialArchonLocations(myTeam)[0];
		defendQueue = new LinkedList<Integer>();
		moveQueue = new LinkedList<MapLocation>();
		rnd = new Random(rc.getID());
		Radio.broadcastInitialStrategyRequest(10);
	}
	private static int counterSinceExpandSignal = 0;
	private static void action() throws GameActionException {
		processSignals();
		switch(strategy) {                                                       
                        case -1:                                                         
                                int channel = Radio.getTuneCommand();                    
                                if(channel == 30) {                                      
                                        strategy = Radio.getStrategyAssignment();        
                                }      
                                break;
						case 0:
                        default:                                                         
                                break;
                }
			int newRadius = Radio.getTurtleExpand();
			if(newRadius != -1) {
				radiusLimit = newRadius;
				counterSinceExpandSignal = 0;
			}
			counterSinceExpandSignal++;
			RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
			ArrayList<MapLocation> enemyArrayList = new ArrayList<MapLocation>();
			while(!Radio.enemySignal.isEmpty()) {
				enemyArrayList.add(Radio.enemySignal.remove().location);
			}
			for(RobotInfo enemyRI : visibleEnemyArray) {
				enemyArrayList.add(enemyRI.location);
			}

			IdAndMapLocation scoutInstruction = Radio.getTurretAttack();
			while(scoutInstruction != null) {
				enemyArrayList.add(scoutInstruction.location);
				scoutInstruction = Radio.getTurretAttack();
			}
			MapLocation[] enemyArray = new MapLocation[enemyArrayList.size()];
			for(int i=0;i<enemyArrayList.size();i++){
				enemyArray[i]=enemyArrayList.get(i);
			}
		if(rc.getType() == RobotType.TURRET) {
			if(enemyArray.length>0){
				if(rc.isWeaponReady()){
					//look for adjacent enemies to attack
					for(MapLocation oneEnemy:enemyArray){
						if(rc.canAttackLocation(oneEnemy)){
							rc.setIndicatorString(0,"trying to attack");
							rc.attackLocation(oneEnemy);
							break;
						}
					}
				}
			} else if(counterSinceExpandSignal < 20) {
				expand(radiusLimit);
			} else if(myLocation.distanceSquaredTo(personalHQ) > radiusLimit) {
				rc.pack();
			}
		}
		else {
			if(myLocation.distanceSquaredTo(personalHQ) > radiusLimit && rc.isCoreReady()) {
				Nav.goTo(personalHQ);
			}
			if(enemyArray.length>0){
				rc.unpack();
			}else if(counterSinceExpandSignal < 20) {
				expand(radiusLimit);
			} else if(counterSinceExpandSignal < 30) {
				if(myLocation.distanceSquaredTo(personalHQ) > radiusLimit / 3 && rc.isCoreReady()) {
					Nav.goTo(personalHQ);
				}
			} else {
				rc.unpack();
			}
		}
	}
	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	private static LinkedList<Integer> defendQueue;
	private static LinkedList<MapLocation> moveQueue;
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

	private static void expand(int radius) throws GameActionException {
				if(rc.getType() == RobotType.TURRET) {
					rc.pack();
				}
				Direction dirToMove = Direction.EAST;
					for (int i = 0; i < 8; ++i) {
						MapLocation target = myLocation.add(dirToMove);
						if (rc.onTheMap(target) && !rc.isLocationOccupied(target) && rc.senseRubble(target) < GameConstants.RUBBLE_OBSTRUCTION_THRESH && target.distanceSquaredTo(personalHQ) > myLocation.distanceSquaredTo(personalHQ) && target.distanceSquaredTo(personalHQ) <= radius) {
							if(rc.getType() == RobotType.TTM) {
								if(rc.isCoreReady()) {
									Nav.goTo(target);
								}
							} else {
								rc.pack();
							}
							return;
						}
						dirToMove = dirToMove.rotateLeft();
					}
				/*
				if(rc.getType() == RobotType.TTM) {
					rc.unpack();
				}
				*/
	}
	private static void contract(int radius) throws GameActionException {
				if(rc.getType() == RobotType.TURRET) {
					rc.pack();
				}
				Direction dirToMove = Direction.EAST;
					for (int i = 0; i < 8; ++i) {
						MapLocation target = myLocation.add(dirToMove);
						if (rc.onTheMap(target) && !rc.isLocationOccupied(target) && rc.senseRubble(target) < GameConstants.RUBBLE_OBSTRUCTION_THRESH && target.distanceSquaredTo(personalHQ) < myLocation.distanceSquaredTo(personalHQ) && target.distanceSquaredTo(personalHQ) <= radius) {
							if(rc.getType() == RobotType.TTM) {
								if(rc.isCoreReady()) {
									Nav.goTo(target);
								}
							} else {
								rc.pack();
							}
							return;
						}
						dirToMove = dirToMove.rotateLeft();
					}
	}
}
