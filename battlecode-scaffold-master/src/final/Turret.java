package final;

import battlecode.common.*;

import java.util.*;

public class Turret extends Bot {
	private static Random rnd;
	private static MapLocation defendLocation, attackLocation;
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			try {
				action();
			} catch(Exception e) {
				
			}
			Clock.yield();
		}
	}

	private static int archonCommanderID = -1;
	private static int lastTurnIHeardFromCommander = -1;

	private static void init() throws GameActionException {
		// initializes soldier
		personalHQ = myLocation;
		RobotInfo[] initialFriends = rc.senseNearbyRobots(SIGHT_RANGE, myTeam);
		RobotInfo closestArchon = null;
		for(int i = initialFriends.length; --i >= 0; ) {
			if(initialFriends[i].type == RobotType.ARCHON) {
				if(closestArchon == null || initialFriends[i].location.distanceSquaredTo(myLocation) < closestArchon.location.distanceSquaredTo(myLocation)) {
					closestArchon = initialFriends[i];
				}
			}
		}
		if(closestArchon != null) {
			archonCommanderID = closestArchon.ID;
			personalHQ = closestArchon.location;
		}
		lastTurnIHeardFromCommander = rc.getRoundNum();
	}

	private static int turnsSinceEnemySeen = 0;

	private static void action() throws GameActionException {
		updateHealth();
		update();
		Radio.process();
		myLocation = rc.getLocation();
		processSignals();
		
		sanityCheck();
		if(rc.getType() == RobotType.TURRET) {
			//RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
			
			RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, enemyTeam);
			RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, Team.ZOMBIE);
			ArrayList<MapLocation> enemyArrayList = new ArrayList<MapLocation>();
			while(!Radio.enemySignal.isEmpty()) {
				enemyArrayList.add(Radio.enemySignal.remove().location);
			}
			/*
			for(RobotInfo enemyRI : visibleEnemyArray) {
				enemyArrayList.add(enemyRI.location);
			}*/

			IdAndMapLocation scoutInstruction = Radio.getTurretAttack();
			while(scoutInstruction != null) {
				enemyArrayList.add(scoutInstruction.location);
				scoutInstruction = Radio.getTurretAttack();
			}
			MapLocation[] enemyArray = new MapLocation[enemyArrayList.size()];


			for(int i=0;i<enemyArrayList.size();i++){
				enemyArray[i]=enemyArrayList.get(i);
			}

			MapLocation toAttack = attackLocation(zombiesWithinRange, enemiesWithinRange, enemyArray);

			if(toAttack != null) {
			// if(enemyArray.length>0){
				if(rc.isWeaponReady()){
					//look for adjacent enemies to attack
//					for(MapLocation oneEnemy:enemyArray){
//						if(rc.canAttackLocation(oneEnemy)){
//							rc.setIndicatorString(0,"trying to attack");
//							rc.attackLocation(oneEnemy);
//							break;
//						}
//					}
					rc.attackLocation(toAttack);
					turnsSinceEnemySeen = 0;
				}
				//could not find any enemies adjacent to attack
				//try to move toward them
				if(rc.isCoreReady()){
					// MapLocation goal = toAttack; // enemyArray[0];
					// Direction toEnemy = rc.getLocation().directionTo(goal);
					if(turnsSinceEnemySeen >= 10) {
						if(strategy == 1) {
							rc.pack();
						}
					}
				}
			} else {//there are no enemies nearby
				//check to see if we are in the way of friends
				//we are obstructing them
//				if(rc.isCoreReady()){
				if(turnsSinceEnemySeen >= 10) {
//					RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
//					if(nearbyFriends.length>3){
//						Direction away = randomDirection();
						rc.pack();
//					}
				}
				turnsSinceEnemySeen++;
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
				turnsSinceEnemySeen = 0;
				//could not find any enemies adjacent to attack
				//try to move toward them
				//if(rc.isCoreReady()){
				//	MapLocation goal = enemyArray[0];
				//	Nav.goTo(goal);
				//}
			}else{
		if(target != null && myLocation.distanceSquaredTo(Combat.target) < SIGHT_RANGE) {
			RobotInfo robotAtTarget = rc.senseRobotAtLocation(target);
			if(rc.senseParts(target) < 1 && (robotAtTarget == null || robotAtTarget.team == Team.NEUTRAL || robotAtTarget.team == myTeam)) {
				setTarget(personalHQ); // nothing to see here, go home
			}
		}
				if(target != null) {
					Nav.goTo(target);
				}
			}
		}
	}
	private static void processSignals() throws GameActionException {
		IdAndMapLocation newDefend = null, newMove = null; int clearDefend = -1; int clearOrders = -1;
		newDefend = Radio.getDefendLocation(); newMove = Radio.getMoveLocation(); clearDefend = Radio.getClearDefend(); clearOrders = Radio.getClear();
		IdAndMapLocation newHQ = Radio.getMoveCampLocation();
		if(rc.getRoundNum() - lastTurnIHeardFromCommander > 500) {
			archonCommanderID = -1;
		}
		while(newHQ != null) {
			if(archonCommanderID == -1 || newHQ.id == archonCommanderID) {
				personalHQ = newHQ.location;
				lastTurnIHeardFromCommander = rc.getRoundNum();
				archonCommanderID = newHQ.id;
			}
			newHQ = Radio.getMoveCampLocation();
		}
		while(newMove != null) {
			if(archonCommanderID == -1 || newMove.id == archonCommanderID) {
				setTarget(newMove.location);
				lastTurnIHeardFromCommander = rc.getRoundNum();
				archonCommanderID = newMove.id;
			}
			newMove = Radio.getMoveLocation();
		}
	}
	private static MapLocation target = null;
	private static void setTarget(MapLocation loc) throws GameActionException {
		target = loc;
	}

	private static void sanityCheck() throws GameActionException {
		boolean insane = false;
		if(target != null) {
			int radiusUnsquared = 0;
			while(++radiusUnsquared * radiusUnsquared <= SIGHT_RANGE);
			--radiusUnsquared;
			if(!rc.onTheMap(new MapLocation(myLocation.x - radiusUnsquared, myLocation.y))) {
				// wall on left side
				if(target.x <= myLocation.x - radiusUnsquared) {
					insane = true;
				}
			}
			if(!rc.onTheMap(new MapLocation(myLocation.x + radiusUnsquared, myLocation.y))) {
				// wall on right side
				if(target.x >= myLocation.x + radiusUnsquared) {
					insane = true;
				}
			}
			if(!rc.onTheMap(new MapLocation(myLocation.x, myLocation.y + radiusUnsquared))) {
				// wall on top side
				if(target.y >= myLocation.y + radiusUnsquared) {
					insane = true;
				}
			}
			if(!rc.onTheMap(new MapLocation(myLocation.x, myLocation.y - radiusUnsquared))) {
				// wall on bottom side
				if(target.y <= myLocation.y - radiusUnsquared) {
					insane = true;
				}
			}
		}
		if(insane) {
			setTarget(personalHQ);
		}
	}
}
