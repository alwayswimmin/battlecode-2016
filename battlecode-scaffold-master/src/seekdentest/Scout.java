package seekdentest;

import battlecode.common.*;

import java.util.Random;
import java.util.LinkedList;

public class Scout extends Bot {
	private static final int SIGHT_RANGE = 53;
	private static RobotType[] robotsEncountered = new RobotType[32001];
	private static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static Random rand;
	private static Direction dirToMove;
	private static LinkedList<MapLocation> locationsPastFewTurns;

	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			Radio.process();
			action();
			Radio.clear();
			Clock.yield();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time
		rand = new Random(rc.getID());
		dirToMove = directions[rand.nextInt(1000) % 8];
		personalHQ = rc.getLocation();
		locationsPastFewTurns = new LinkedList<MapLocation>();
	}
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();

		RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.ZOMBIE);

		boolean denFound = false;

		for(int i = zombiesWithinRange.length; --i >= 0; ) {
			if(robotsEncountered[zombiesWithinRange[i].ID] == null && zombiesWithinRange[i].type == RobotType.ZOMBIEDEN) {
				robotsEncountered[zombiesWithinRange[i].ID] = zombiesWithinRange[i].type;
				Radio.broadcastDenLocation(zombiesWithinRange[i].location, 1000);
				denFound = true;
			}
		}
/*
		if(!denFound && enemiesWithinRange.length != 0) {
			Radio.broadcastMoveLocation(enemiesWithinRange[0].location, 1000);
		}
*/
		for(int i = 8; --i >= 0; ) {
			if(rc.isCoreReady()) {
				Nav.goTo(myLocation.add(dirToMove).add(dirToMove), new SPAll(rc.senseHostileRobots(myLocation, SIGHT_RANGE)));
			}
			if(rc.isCoreReady()) {
				dirToMove = dirToMove.rotateLeft();
			}
		}
		for(int i = 8; --i >= 0; ) {
			if(rc.isCoreReady()) {
				Nav.goTo(myLocation.add(dirToMove).add(dirToMove));
			}
			if(rc.isCoreReady()) {
				dirToMove = dirToMove.rotateLeft();
			}
		}

		locationsPastFewTurns.add(new MapLocation(myLocation.x, myLocation.y));
		if(rc.getRoundNum() >= 20) {
			MapLocation whereIWas = locationsPastFewTurns.remove();
			if(whereIWas.distanceSquaredTo(myLocation) < 5) {
				dirToMove = directions[rand.nextInt(1000) % 8];
			}
		}
	}
}
