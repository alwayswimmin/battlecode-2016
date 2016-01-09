package seekdentest;

import battlecode.common.*;

public class Scout extends Bot {
	private static final int SIGHT_RANGE = 53;
	private static RobotType[] robotsEncountered = new RobotType[32001];

	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			Radio.process();
			action();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time

	}
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();

		RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, enemyTeam);
		RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.ZOMBIE);

		for(int i = zombiesWithinRange.length; --i >= 0; ) {
			if(robotsEncountered[zombiesWithinRange[i].ID] == null && zombiesWithinRange[i].type == RobotType.ZOMBIEDEN) {
				robotsEncountered[zombiesWithinRange[i].ID] = zombiesWithinRange[i].type;
				Radio.broadcastDenLocation(zombiesWithinRange[i].location, 1000);
				System.out.println("DEN ENCOUNTERED!!!");
			}
		}

		if(rc.isCoreReady()) {
			Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
			Direction dirToMove = directions[(int) (Math.random() * 8)];
			// Check the rubble in that direction
			if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				// Too much rubble, so I should clear it
				rc.clearRubble(dirToMove);
				// Check if I can move in this direction
			} else if (rc.canMove(dirToMove)) {
				// Move
				rc.move(dirToMove);
			}
		}
		Clock.yield();
	}
}
