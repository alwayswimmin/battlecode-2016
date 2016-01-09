package seekdentest;

import battlecode.common.*;

import java.util.Random;
import java.util.ArrayList;

public class Turret extends Bot {
	private static Random rnd;
	private static MapLocation defendLocation, attackLocation;
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			Radio.process();
			action();
			Clock.yield();
			Radio.clear();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time
		rnd = new Random(rc.getID());
	}
	private static void action() throws GameActionException {
		if(rc.getType() == RobotType.TURRET) {
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
				rc.pack();
			}
		} else {//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(nearbyFriends.length>3){
					Direction away = randomDirection();
					rc.pack();
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
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			MapLocation newDefend = null, newAttack = null;
			newDefend = Radio.getDefendLocation();
			if(newDefend != null) {
				if(defendLocation == null && newDefend != null) {
					defendLocation = newDefend;
				}
			} else {
				defendLocation = null;
			}
			newAttack = Radio.getMoveLocation();
			if(attackLocation == null && newAttack != null) {
				attackLocation = newAttack;
			}
			if(defendLocation != null) {
				if(rc.isCoreReady()) {
					Nav.goTo(defendLocation);
				}
			} else if(attackLocation != null) {
				if(rc.isCoreReady()) {
					Nav.goTo(attackLocation);
				}
				if(rc.canSenseLocation(attackLocation) && rc.senseRobotAtLocation(attackLocation) == null) {
					attackLocation = null;
				}
			}
		}
			
		}
	}
	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
}
