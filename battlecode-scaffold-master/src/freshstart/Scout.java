package freshstart;

import battlecode.common.*;

import java.util.Random;

public class Scout extends Bot {
	private static RobotType[] robotsEncountered = new RobotType[32001];
	private static int[] turnBroadcasted = new int[32001];
	private static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static Random rand;
	private static Direction dirToMove;
	private static MyQueue<MapLocation> locationsPastFewTurns;
	private static int myFirstTurn = -1;

	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			updateHealth();
			Radio.process();
			action();
			Radio.clear();
			Clock.yield();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time
		myFirstTurn = rc.getRoundNum();
		Radio.broadcastInitialStrategyRequest(10);
		rand = new Random(rc.getID());
		dirToMove = directions[rand.nextInt(1000) % 8];
		personalHQ = rc.getLocation();
		locationsPastFewTurns = new MyQueue<MapLocation>();
	}

	private static RobotInfo[]   friendsWithinRange;
	private static RobotInfo[]   enemiesWithinRange;
	private static RobotInfo[]   zombiesWithinRange;
	private static RobotInfo[]   neutralsWithinRange;
	private static MapLocation[] partsWithinRange;
	private static int radiusLimit = 4;
	private static int cooldownPartsBroadcast = 180;

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
			Nav.goTo(rc.getInitialArchonLocations(enemyTeam)[0]);
		}
	}
	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();
		if(isSurroundedByZombies())
			runAtEnemyArchon();

		int enemyCentroidX = 0, enemyCentroidY = 0;
		MapLocation enemyCentroid = null;
		RobotInfo[] hostileWithinRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		if(hostileWithinRange.length != 0) {
			int enemyCount = 0;
			// finds centroid of visible enemies
			for(int i = hostileWithinRange.length; --i >= 0; ) {
				if(hostileWithinRange[i].attackPower > 0) {
					enemyCentroidX += hostileWithinRange[i].location.x;
					enemyCentroidY += hostileWithinRange[i].location.y;
					enemyCount++;
				}
			}
			if(enemyCount != 0)
			enemyCentroid = new MapLocation(enemyCentroidX / enemyCount, enemyCentroidY / enemyCount);
		}
		if(enemyCentroid != null) {

					int dx = myLocation.x - enemyCentroid.x;
					int dy = myLocation.y - enemyCentroid.y;
					MapLocation dest = new MapLocation(myLocation.x + 3 * dx, myLocation.y + 3 * dy);
					Combat.retreat(hostileWithinRange);
					if(rc.isCoreReady()) {
						Nav.goTo(dest);
					}
		}

		friendsWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, myTeam);
		enemiesWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, enemyTeam);
		zombiesWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.ZOMBIE);
		neutralsWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.NEUTRAL);
		partsWithinRange = rc.sensePartLocations(SIGHT_RANGE);

		int broadcastCount = 0;

		for(int i = zombiesWithinRange.length; --i >= 0; ) {
			if(broadcastCount < 20 && (robotsEncountered[zombiesWithinRange[i].ID] == null || rc.getRoundNum() - turnBroadcasted[zombiesWithinRange[i].ID] > 100) && zombiesWithinRange[i].type == RobotType.ZOMBIEDEN) {
				robotsEncountered[zombiesWithinRange[i].ID] = zombiesWithinRange[i].type;
				turnBroadcasted[zombiesWithinRange[i].ID] = rc.getRoundNum();
				Radio.broadcastDenLocation(zombiesWithinRange[i].location, 1000);
				++broadcastCount;
			}
		}

		/*
		   if(!denFound && enemiesWithinRange.length != 0) {
		   Radio.broadcastMoveLocation(enemiesWithinRange[0].location, 1000);
		   }
		*/

		for(int i = enemiesWithinRange.length; --i >= 0; ) {
			if(broadcastCount < 20 && (robotsEncountered[enemiesWithinRange[i].ID] == null || rc.getRoundNum() - turnBroadcasted[enemiesWithinRange[i].ID] > 100) && enemiesWithinRange[i].type == RobotType.ARCHON) {
				robotsEncountered[enemiesWithinRange[i].ID] = enemiesWithinRange[i].type;
				turnBroadcasted[enemiesWithinRange[i].ID] = rc.getRoundNum();
				Radio.broadcastEnemyArchonLocation(enemiesWithinRange[i].location, 1000);
				++broadcastCount;
			}
		}

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

		instructTurret();

		switch(strategy) {
			case -1:
				int channel = Radio.getTuneCommand();
				if(channel == 30) {
					strategy = Radio.getStrategyAssignment();
				}
				break;
			case 0:
				// turret defense
				for (int i = 0; i < 8; ++i) {
					if (rc.isCoreReady() && rc.canMove(dirToMove) && myLocation.add(dirToMove).distanceSquaredTo(personalHQ) < radiusLimit) {
						Nav.goTo(myLocation.add(dirToMove)); break;
					}                                                                            
					dirToMove = dirToMove.rotateLeft();
				}    
				if(rc.getRoundNum() % 50 == 0) {                                                 
					radiusLimit++;
				} 
				break;
			case 1:
				// move randomly
				
				/* for(int i = 8; --i >= 0; ) {
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
				} */
				
				double centerScoutsX = 0, centerScoutsY = 0;
				int scoutCount = 0;

				for (int j = 0; j < friendsWithinRange.length; ++j) {
					if (friendsWithinRange[j].type != RobotType.SCOUT)
						continue;

					centerScoutsX += friendsWithinRange[j].location.x;
					centerScoutsY += friendsWithinRange[j].location.y;
					scoutCount++;
				}

				if (scoutCount != 0.0) {
					centerScoutsX /= scoutCount;
					centerScoutsY /= scoutCount;

					double maxDist = 0;

					for (int i = 0; i < 8; ++i) {
						if (rc.canMove(directions[i])) {
							double goToX = (double)(rc.getLocation().add(directions[i]).x);
							double goToY = (double)(rc.getLocation().add(directions[i]).y);

							double diffX = goToX - centerScoutsX;
							double diffY = goToY - centerScoutsY;

							if (diffX*diffX + diffY*diffY > maxDist) {
								maxDist = diffX*diffX + diffY*diffY;
								dirToMove = directions[i];
							}
						}
					}
				}

				if(rc.isCoreReady()) {
					Nav.goTo(myLocation.add(dirToMove).add(dirToMove), new SPAll(rc.senseHostileRobots(myLocation, SIGHT_RANGE)));
				}

				if(rc.isCoreReady()) {
					Nav.goTo(myLocation.add(dirToMove).add(dirToMove));
				}

				locationsPastFewTurns.add(new MapLocation(myLocation.x, myLocation.y));
				if(rc.getRoundNum() - myFirstTurn >= 20) {
					MapLocation whereIWas = locationsPastFewTurns.remove();
					if(whereIWas.distanceSquaredTo(myLocation) < 5) {
						dirToMove = directions[rand.nextInt(1000) % 8];
					}
				}

				break;

			case 2:
				// follow offensive turret
				boolean shouldMoveAway = false;
				for(int j = friendsWithinRange.length; --j >= 0; ) {
					if(friendsWithinRange[j].type == RobotType.SCOUT) {
						shouldMoveAway = true;
					}
				}
				if(!shouldMoveAway) {
					for(int j = friendsWithinRange.length; --j >= 0; ) {
						if(friendsWithinRange[j].type == RobotType.TURRET || friendsWithinRange[j].type == RobotType.TTM) {
							if(rc.isCoreReady()) {
								Nav.goTo(friendsWithinRange[j].location);
							}
						}
					}
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
				if(rc.getRoundNum() - myFirstTurn >= 20) {
					MapLocation whereIWas = locationsPastFewTurns.remove();
					if(whereIWas.distanceSquaredTo(myLocation) < 5) {
						dirToMove = directions[rand.nextInt(1000) % 8];
					}
				}
		}
	}

	private static void instructTurret() throws GameActionException {
		// maximum broadcast 3 times
		int instructionCount = 0;
		for(int i = enemiesWithinRange.length; --i >= 0; ) {
			for(int j = friendsWithinRange.length; --j >= 0; ) {
				if(friendsWithinRange[j].type == RobotType.TURRET || friendsWithinRange[j].type == RobotType.TTM) {
					int dist = enemiesWithinRange[i].location.distanceSquaredTo(friendsWithinRange[j].location);
					if(dist > friendsWithinRange[j].type.sensorRadiusSquared && dist <= friendsWithinRange[j].type.attackRadiusSquared) {
						Radio.broadcastTurretAttack(enemiesWithinRange[i].location, 2 * friendsWithinRange[j].location.distanceSquaredTo(myLocation));
						++instructionCount;
						break;
					}
				}
			}
			if(instructionCount == 3) {
				return;
			}
		}
		for(int i = zombiesWithinRange.length; --i >= 0; ) {
			for(int j = friendsWithinRange.length; --j >= 0; ) {
				if(friendsWithinRange[j].type == RobotType.TURRET || friendsWithinRange[j].type == RobotType.TTM) {
					int dist = zombiesWithinRange[i].location.distanceSquaredTo(friendsWithinRange[j].location);
					if(dist > friendsWithinRange[j].type.sensorRadiusSquared && dist <= friendsWithinRange[j].type.attackRadiusSquared) {
						Radio.broadcastTurretAttack(zombiesWithinRange[i].location, 2 * friendsWithinRange[j].location.distanceSquaredTo(myLocation));
						++instructionCount;
						break;
					}
				}
			}
			if(instructionCount == 3) {
				return;
			}
		}
	}
}
