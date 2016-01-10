package seekdentest;

import battlecode.common.*;
import java.util.LinkedList;

public class Guard extends Bot {
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
	}
	private static void action() throws GameActionException {
		// take my turn
		processSignals();
		RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(ATTACK_RANGE, Team.ZOMBIE);
		if (zombiesWithinRange.length > 0) {
			if (rc.isWeaponReady()) {
				rc.attackLocation(zombiesWithinRange[0].location);
			}
		} else if (enemiesWithinRange.length > 0) {
			if (rc.isWeaponReady()) {
				rc.attackLocation(enemiesWithinRange[0].location);
			}
		}
		RobotInfo[] enemiesWithinSightRange = rc.senseNearbyRobots(SIGHT_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinSightRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.ZOMBIE);
		if (zombiesWithinRange.length > 0) {
			if(rc.isCoreReady()) {
				Nav.goTo(zombiesWithinSightRange[0].location);
			}
		} else if (enemiesWithinRange.length > 0) {
			if(rc.isCoreReady()) {
				Nav.goTo(enemiesWithinSightRange[0].location);
			}
		}
		moveSomewhere();
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
