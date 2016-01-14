package team074;

import battlecode.common.*;

import java.util.*;

public class Turret extends Bot {
	private static Random rnd;
	private static MapLocation defendLocation, attackLocation;
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
		personalHQ = rc.getLocation();
		defendQueue = new LinkedList<Integer>();
		moveQueue = new LinkedList<MapLocation>();
		rnd = new Random(rc.getID());
		Radio.broadcastInitialStrategyRequest(10);
	}
	private static void action() throws GameActionException {
		processSignals();
		switch(strategy) {                                                       
                        case -1:                                                         
                                int channel = Radio.getTuneCommand();                    
                                if(channel == 30) {                                      
                                        strategy = Radio.getStrategyAssignment();        
                                }      
                                break;
                        default:                                                         
                                break;
                }
		if(rc.getType() == RobotType.TURRET) {
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
				//could not find any enemies adjacent to attack
				//try to move toward them
				if(rc.isCoreReady()){
					MapLocation goal = enemyArray[0];
					Direction toEnemy = rc.getLocation().directionTo(goal);
				if(strategy == 1) {
					rc.pack();
				}
				}
			} else {//there are no enemies nearby
				//check to see if we are in the way of friends
				//we are obstructing them
				if(rc.isCoreReady()){
					RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
					if(nearbyFriends.length>3){
						Direction away = randomDirection();
						if(strategy == 1) {
							rc.pack();
						}
					}
				}
			}
		}
		else {
			RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
			ArrayList<MapLocation> enemyArrayList = new ArrayList<MapLocation>();
			while(!Radio.enemySignal.isEmpty()) {
				enemyArrayList.add(Radio.enemySignal.remove().location);
			}
			for(RobotInfo enemyRI : visibleEnemyArray) {
				enemyArrayList.add(enemyRI.location);
			}
			MapLocation[] enemyArray = new MapLocation[enemyArrayList.size()];

			for(int i=0;i<enemyArrayList.size();i++){
				enemyArray[i]=enemyArrayList.get(i);
			}

			if(enemyArray.length>0){
				rc.unpack();
				//could not find any enemies adjacent to attack
				//try to move toward them
				if(rc.isCoreReady()){
					MapLocation goal = enemyArray[0];
					Nav.goTo(goal);
				}
			}else{
				if(strategy == 1) {
					moveSomewhere();
				}
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
	}
}
