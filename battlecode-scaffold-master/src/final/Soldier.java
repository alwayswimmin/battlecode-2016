package final;

import battlecode.common.*;
import java.util.*;

public class Soldier extends Bot {
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

	private static void action() throws GameActionException {
		updateHealth();
		update();
		Radio.process();
		myLocation = rc.getLocation();
		processSignals();
		
		if(isSurroundedByZombies()) {
			runAtEnemyArchon();
		}

		sanityCheck();
		if(Combat.target != null && myLocation.distanceSquaredTo(Combat.target) < SIGHT_RANGE) {
			RobotInfo robotAtTarget = rc.senseRobotAtLocation(Combat.target);
			if(rc.senseParts(Combat.target) < 1 && (robotAtTarget == null || robotAtTarget.team == Team.NEUTRAL || robotAtTarget.team == myTeam)) {
				setTarget(personalHQ); // nothing to see here, go home
			}
		}

		if(Combat.target != null) {
			rc.setIndicatorString(0, Combat.target.x + ", " + Combat.target.y);
		}
		Combat.action(); // micro attacks

		Radio.clear();
	}

	private static boolean isSurroundedByZombies() throws GameActionException {
		if(rc.getRoundNum() < 1500)
			return false;
		RobotInfo[] zombiesNB = rc.senseNearbyRobots(SIGHT_RANGE, Team.ZOMBIE);
		RobotInfo[] enemiesNB = rc.senseNearbyRobots(6, enemyTeam);
		RobotInfo[] alliesNB = rc.senseNearbyRobots(6, myTeam);
		return (alliesNB.length < 2 && enemiesNB.length < 2 && zombiesNB.length > 5);
	}

	private static void runAtEnemyArchon() throws GameActionException {
		if(rc.isCoreReady()) {
		int closestEnemyArchonIndex = -1;
		int bestDistanceToEnemyArchon = 1000000;
		for(int i = numberOfEnemyArchons, j = 4; --i >= 0 && --j >= 0; ) {
			if(enemyArchonVisited[i]) {
				// this enemy was already visited, don't go here again
				continue;
			}
			int distanceToEnemyArchon = myLocation.distanceSquaredTo(enemyArchons[i]);
			if(closestEnemyArchonIndex == -1 || distanceToEnemyArchon < bestDistanceToEnemyArchon) {
				closestEnemyArchonIndex = i;
				bestDistanceToEnemyArchon = distanceToEnemyArchon;
			}
		}
			if(closestEnemyArchonIndex != -1) {
				Nav.goTo(enemyArchons[closestEnemyArchonIndex]);
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
		// process incoming scout information concerning enemy archons
		IdAndMapLocation newEnemyArchon = Radio.getEnemyArchonLocation();
		while(newEnemyArchon != null) {
			// add to known archon footprint
			enemyArchons[numberOfEnemyArchons] = newEnemyArchon.location;
			enemyArchonVisited[numberOfEnemyArchons] = false;
			numberOfEnemyArchons++;
			// read in another archon
			newEnemyArchon = Radio.getEnemyArchonLocation();
		}

	}
	// can have duplicates, since archons move; this functions more as a cyclic stack
	private static MapLocation[] enemyArchons = new MapLocation[10000];
	private static boolean[] enemyArchonVisited = new boolean[10000];
	private static int numberOfEnemyArchons = 0;
	private static MapLocation target;
	private static void setTarget(MapLocation loc) throws GameActionException {
		Combat.target = loc;
	}

	private static void sanityCheck() throws GameActionException {
		boolean insane = false;
		if(Combat.target != null) {
			int radiusUnsquared = 0;
			while(++radiusUnsquared * radiusUnsquared <= SIGHT_RANGE);
			--radiusUnsquared;
			if(!rc.onTheMap(new MapLocation(myLocation.x - radiusUnsquared, myLocation.y))) {
				// wall on left side
				if(Combat.target.x <= myLocation.x - radiusUnsquared) {
					insane = true;
				}
			}
			if(!rc.onTheMap(new MapLocation(myLocation.x + radiusUnsquared, myLocation.y))) {
				// wall on right side
				if(Combat.target.x >= myLocation.x + radiusUnsquared) {
					insane = true;
				}
			}
			if(!rc.onTheMap(new MapLocation(myLocation.x, myLocation.y + radiusUnsquared))) {
				// wall on top side
				if(Combat.target.y >= myLocation.y + radiusUnsquared) {
					insane = true;
				}
			}
			if(!rc.onTheMap(new MapLocation(myLocation.x, myLocation.y - radiusUnsquared))) {
				// wall on bottom side
				if(Combat.target.y <= myLocation.y - radiusUnsquared) {
					insane = true;
				}
			}
		}
		if(insane) {
			setTarget(personalHQ);
		}
	}
}
