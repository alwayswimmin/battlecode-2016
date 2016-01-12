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
		RobotInfo[] neutralsWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.NEUTRAL);
		MapLocation[] partsWithinRange = rc.sensePartLocations(SIGHT_RANGE);

		int broadcastCount = 0;
		int cooldownPartsBroadcast = 100;

		for(int i = zombiesWithinRange.length; --i >= 0; ) {
			if(broadcastCount < 20 && robotsEncountered[zombiesWithinRange[i].ID] == null && zombiesWithinRange[i].type == RobotType.ZOMBIEDEN) {
				robotsEncountered[zombiesWithinRange[i].ID] = zombiesWithinRange[i].type;
				Radio.broadcastDenLocation(zombiesWithinRange[i].location, 1000);
				++broadcastCount;
			}
		}
/*
		if(!denFound && enemiesWithinRange.length != 0) {
			Radio.broadcastMoveLocation(enemiesWithinRange[0].location, 1000);
		}
*/
		for(int i = neutralsWithinRange.length; --i >= 0; ) {
			if(broadcastCount < 20 && robotsEncountered[neutralsWithinRange[i].ID] == null) {
				robotsEncountered[neutralsWithinRange[i].ID] = neutralsWithinRange[i].type;
				Radio.broadcastNeutralLocation(neutralsWithinRange[i].location, 1000);
				++broadcastCount;
			}
		}

		cooldownPartsBroadcast++;
		if(broadcastCount < 20 && cooldownPartsBroadcast >= 200 && partsWithinRange.length != 0) {
			double total = 0.0;
			for(int i = partsWithinRange.length; --i >= 0; ) {
				total += rc.senseParts(partsWithinRange[i]);
			}
			if(total > 200.0) {
				Radio.broadcastPartsLocation(partsWithinRange[0], 1000);
				cooldownPartsBroadcast = 0;
			}
			++broadcastCount;
		}

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
