package turtletest;

import battlecode.common.*;
import java.util.*;

public class Archon extends Bot {
	private static int scoutsBuilt = 0;
	private static int forcedMoveCounter = 0; // for when Archon is overidden to move in a specific direction
	private static Direction forcedMoveDir;

	private static MapLocation neutralLocation = null;
	private static MapLocation partsLocation = null;
	private static LinkedList<MapLocation> neutralQueue;
	private static LinkedList<MapLocation> partsQueue;

	private static int radiusLimit = 4;
	private static IdAndMapLocation den = null;
	private static int turnsSinceEnemySeen = 100;

	// 0: ARCHON
	// 1: GUARD
	// 2: SCOUT
	// 3: SOLDIER
	// 4: TURRET/TTM
	// 5: VIPER
	private static RobotType[] robotTypes = {
		RobotType.ARCHON,
		RobotType.GUARD,
		RobotType.SCOUT,
		RobotType.SOLDIER,
		RobotType.TURRET,
		RobotType.VIPER
	};

	private static int[] unitsOfTypeBuilt = {1, 0, 0, 0, 0, 0};
	// includes activation.

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
		// initializes Archon
		personalHQ = rc.getLocation();
		neutralQueue = new LinkedList<MapLocation>();
		partsQueue = new LinkedList<MapLocation>();
	}

	private static void action() throws GameActionException {
		// takes turn in following order:
		//     makes forced moves if necessary
		//     heals friends
		//     processes enemies (and possibly calls to defend)

		myLocation = rc.getLocation();
		// make forced moves if forced move counter is non-zero
		if (forcedMoveCounter > 0 && rc.isCoreReady()) {
			forcedMoveCounter--;
			if (rc.canMove(forcedMoveDir)) {
				rc.move(forcedMoveDir);
				return;
			} else if (rc.canMove(forcedMoveDir.rotateLeft())) {
				rc.move(forcedMoveDir.rotateLeft());
				return;
			} else if (rc.canMove(forcedMoveDir.rotateRight())) {
				rc.move(forcedMoveDir.rotateRight());
				return;
			} else {
				forcedMoveCounter = 0;
			}
		}

		RobotInfo[] hostileWithinRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		RobotInfo[] neutralWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.NEUTRAL);
		RobotInfo[] friendWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, rc.getTeam());

		// processes friends and tries to heal them
		for (int i = 0; i < friendWithinRange.length; ++i) {
			if (friendWithinRange[i].health != friendWithinRange[i].maxHealth && 
					(ATTACK_RANGE > distanceBetween(rc.getLocation(), friendWithinRange[i].location)) 
					&& (friendWithinRange[i].type != RobotType.ARCHON)) {
				rc.repair(friendWithinRange[i].location);
				break;
			}
		}

		int enemyCentroidX = 0, enemyCentroidY = 0;
		MapLocation enemyCentroid = null;

		if(hostileWithinRange.length != 0) {
			// if enemies, call friends to defend
			int enemyCount = hostileWithinRange.length;
			if(turnsSinceEnemySeen > 5) { // waits at least 5 turns between broadcasts
				Radio.broadcastDefendLocation(myLocation, 1000);
			}
			turnsSinceEnemySeen = 0;
			// finds centroid of visible enemies
			for(int i = enemyCount; --i >= 0; ) {
				enemyCentroidX += hostileWithinRange[i].location.x;
				enemyCentroidY += hostileWithinRange[i].location.y;
			}
			enemyCentroid = new MapLocation(enemyCentroidX / enemyCount, enemyCentroidY / enemyCount);
		} else {
			turnsSinceEnemySeen++;
			if(turnsSinceEnemySeen == 15) {
				Radio.broadcastClearDefend(1000);
			}
			// if no enemies, send friends to den
			den = Radio.getDenLocation();
			if(den != null) {
				Radio.broadcastMoveLocation(den.location, 1000);
			}
		}

		// finds neutrals to activate
		int canActivate = -1;
		for (int i = 0; i < neutralWithinRange.length; ++i) {
			if (distanceBetween(rc.getLocation(), neutralWithinRange[i].location) < 3) {
				canActivate = i;
			}
		}

		// builds friends
		int typeToBuild;
		boolean addedRobot;
		switch(strategy) {
			case -1:
				// assign a strategy to Archon
				if(rc.getTeamParts() >= 300) {
					strategy = 0;
				} else {
					strategy = 1;
				}
				action();
				break;
			case 0:
				processSignals();
				// 0: ARCHON
				// 1: GUARD
				// 2: SCOUT
				// 3: SOLDIER
				// 4: TURRET/TTM
				// 5: VIPER
				if(scoutsBuilt++ < 2){
					typeToBuild = 4;
				} else if(Math.random() > 0.8){
					typeToBuild = 2;
				} else {
					typeToBuild = 4;
				}
				addedRobot = false;
				if (rc.isCoreReady()) {
					// activates neutrals if possible
					if (canActivate > -1) {
						rc.activate(neutralWithinRange[canActivate].location);
						switch(neutralWithinRange[canActivate].type) {
							case ARCHON:
								typeToBuild = 0;
								break;
							case GUARD:
								typeToBuild = 1;
								break;
							case SCOUT:
								typeToBuild = 2;
								break;
							case SOLDIER:
								typeToBuild = 3;
								break;
							case TURRET:
							case TTM:
								typeToBuild = 4;
								break;
							case VIPER:
								typeToBuild = 5;
								break;
							default:
								break;
						}
						unitsOfTypeBuilt[typeToBuild]++;
						addedRobot = true;
					} else if(enemyCentroid != null && !enemyCentroid.equals(myLocation)) {
						MapLocation dest = new MapLocation(4 * myLocation.x - 3 * enemyCentroid.x, 4 * myLocation.y - 3 * enemyCentroid.y);
						boolean[] closeTo = {false, false, false, false};

						Direction goTo = directions[1];

						for (int i = 0; i < 8; i += 2) {
							closeTo[i/2] = (distToWall(rc.getLocation(), directions[i]) <= 2);
						}

						for (int i = 0; i < 4; ++i) {
							if (closeTo[i] && closeTo[(i+1)%4])
								goTo = directions[(2*i + 4)%8];
						}

						MapLocation opt1 = new MapLocation(0, 0).add(goTo);
						MapLocation opt2 = new MapLocation(0, 0).add(goTo.rotateLeft().rotateLeft());


						if (goTo != directions[1] && rc.canMove((distanceBetween(opt1, dest) <= distanceBetween(opt2, dest)) ? goTo : goTo.rotateLeft().rotateLeft())) {
							rc.move((distanceBetween(opt1, dest) <= distanceBetween(opt2, dest)) ? goTo : goTo.rotateLeft().rotateLeft());
							forcedMoveDir = (distanceBetween(opt1, dest) <= distanceBetween(opt2, dest)) ? goTo : goTo.rotateLeft().rotateLeft();
							forcedMoveCounter = 8;
						} else {
							Nav.goTo(dest, new SPAll(hostileWithinRange));
						}
					} else if (rc.hasBuildRequirements(robotTypes[typeToBuild])) {
						Direction dirToBuild = Direction.EAST;
						for (int i = 0; i < 8; i++) {
							if ((rc.getRoundNum() <= 20 || rc.getRoundNum() > 600) && rc.canBuild(dirToBuild, robotTypes[typeToBuild]) && (myLocation.add(dirToBuild).x + myLocation.add(dirToBuild).y) % 2 == 1 && rc.getTeamParts() > 131) {
								rc.build(dirToBuild, robotTypes[typeToBuild]);
								unitsOfTypeBuilt[typeToBuild]++;
								addedRobot = true;
								break;
							} else {
								dirToBuild = dirToBuild.rotateLeft();
							}
						}
					}

					if(rc.isCoreReady()) {
						if (rc.isCoreReady()) {
							int rot = (int)(Math.random() * 8);
							Direction dirToMove = Direction.EAST;
							for (int i = 0; i < rot; ++i)
								dirToMove = dirToMove.rotateLeft();

							for (int i = 0; i < 8; ++i) {
								if (rc.canMove(dirToMove) && myLocation.add(dirToMove).distanceSquaredTo(personalHQ) < radiusLimit) {
									Nav.goTo(myLocation.add(dirToMove)); break;
								}
								dirToMove = dirToMove.rotateLeft();
							}
						}
					}
				}
				if(rc.getRoundNum() % 50 == 0) {
					radiusLimit++;
				}
				break;
			case 1:
				processSignals();
				// 0: ARCHON
				// 1: GUARD
				// 2: SCOUT
				// 3: SOLDIER
				// 4: TURRET/TTM
				// 5: VIPER
				if(unitsOfTypeBuilt[2] < 1 || Math.random() > 0.9){
					typeToBuild = 2;
				} else if(Math.random() > 0.8){
					typeToBuild = 1;
				} else {
					typeToBuild = 3;
				}
				addedRobot = false;
				if (rc.isCoreReady()) {
					if (canActivate > -1) {
						rc.activate(neutralWithinRange[canActivate].location);
						switch(neutralWithinRange[canActivate].type) {
							case ARCHON:
								typeToBuild = 0;
								break;
							case GUARD:
								typeToBuild = 1;
								break;
							case SCOUT:
								typeToBuild = 2;
								break;
							case SOLDIER:
								typeToBuild = 3;
								break;
							case TURRET:
							case TTM:
								typeToBuild = 4;
								break;
							case VIPER:
								typeToBuild = 5;
								break;
							default:
								break;
						}
						unitsOfTypeBuilt[typeToBuild]++;
						addedRobot = true;
					} else if(enemyCentroid != null && !enemyCentroid.equals(myLocation)) {
						MapLocation dest = new MapLocation(4 * myLocation.x - 3 * enemyCentroid.x, 4 * myLocation.y - 3 * enemyCentroid.y);
						Nav.goTo(dest, new SPAll(hostileWithinRange));
					} else if (typeToBuild != -1 && rc.hasBuildRequirements(robotTypes[typeToBuild]) && (rc.getRoundNum() < 550 || rc.getTeamParts() > 130)) {
						Direction dirToBuild = Direction.EAST;
						for (int i = 0; i < 8; i++) {
							if (rc.canBuild(dirToBuild, robotTypes[typeToBuild])) {
								rc.build(dirToBuild, robotTypes[typeToBuild]);
								unitsOfTypeBuilt[typeToBuild]++;
								addedRobot = true;
								break;
							} else {
								dirToBuild = dirToBuild.rotateLeft();
							}
						}
					} else if (rc.isCoreReady()) {
						if(neutralWithinRange.length > 0) {
							Nav.goTo(neutralWithinRange[0].location);
						}
						if(rc.isCoreReady()) {
							MapLocation[] partsLocations = rc.sensePartLocations(SIGHT_RANGE);
							if(partsLocations.length > 0) {
								int maxindex = 0;
								for(int i = partsLocations.length; --i > 0; ) {
									if(rc.senseParts(partsLocations[i]) > rc.senseParts(partsLocations[maxindex])) {
										maxindex = i;
									}
								}
								Nav.goTo(partsLocations[maxindex]);
							}
						}
						if(rc.isCoreReady()) {
							moveSomewhere();
						}
					}
				}
				if (rc.isCoreReady()) {
					int rot = (int)(Math.random() * 8);
					Direction dirToMove = Direction.EAST;
					for (int i = 0; i < rot; ++i)
						dirToMove = dirToMove.rotateLeft();

					for (int i = 0; i < 8; ++i) {
						if (rc.canMove(dirToMove)) {
							Nav.goTo(myLocation.add(dirToMove)); break;
						}

						dirToMove = dirToMove.rotateLeft();
					}
				}
				break;
		}
	}

	private static void processSignals() throws GameActionException {
		// processes radio signals:
		//     receives strategy requests and assigns strategies
		//     gets neutral locations
		//     gets parts locations
		int strategyRequest = Radio.getInitialStrategyRequest();
		while(strategyRequest != -1) {
			Radio.broadcastTuneCommand(strategyRequest, 30, 6);
			if(!rc.canSenseRobot(strategyRequest)) {
				strategyRequest = Radio.getInitialStrategyRequest();
				continue;
			}
			RobotInfo requestingRobot = rc.senseRobot(strategyRequest);
			int requestingRobotTypeInt = 0;
			switch(requestingRobot.type) {
				case ARCHON:
					requestingRobotTypeInt = 0;
					break;
				case GUARD:
					requestingRobotTypeInt = 1;
					break;
				case SCOUT:
					requestingRobotTypeInt = 2;
					break;
				case SOLDIER:
					requestingRobotTypeInt = 3;
					break;
				case TURRET:
				case TTM:
					requestingRobotTypeInt = 4;
					break;
				case VIPER:
					requestingRobotTypeInt = 5;
					break;
				default:
					break;
			}
			Radio.broadcastStrategyAssignment(determineStrategy(requestingRobotTypeInt), 10);
			strategyRequest = Radio.getInitialStrategyRequest();
		}
		IdAndMapLocation newNeutral = null, newParts = null;
		newNeutral = Radio.getNeutralLocation(); newParts = Radio.getPartsLocation();
		while(newNeutral != null) {
			neutralQueue.add(newNeutral.location);
			newNeutral = Radio.getDefendLocation();
		}
		while(newParts != null) {
			partsQueue.add(newParts.location);
			newParts = Radio.getPartsLocation();
		}
	}

	private static void moveSomewhere() throws GameActionException {
		while(!neutralQueue.isEmpty()) {
			MapLocation next = neutralQueue.element();
			if(rc.isCoreReady()) {
				Nav.goTo(next);
			}
			if(rc.canSense(next)) {
				neutralQueue.remove();
			}
			return;
		}
		if(!partsQueue.isEmpty()) {
			MapLocation next = partsQueue.element();
			if(rc.isCoreReady()) {
				Nav.goTo(next);
			}
			if(rc.canSense(next)) {
				partsQueue.remove();
			}
			return;
		}
		if(den != null) {
			Nav.goTo(den.location);
		}
	}


	private static int determineStrategy(int robotType) throws GameActionException {
		switch(robotType) {
			case 0:
			case 1:
				return 1;
				// return 2 - (unitsOfTypeBuilt[robotType] % 3) % 2; // 0 defend, 1 attack
			case 2:
				return strategy; // 0 turret defense, 1 roam
			case 3:
				return 1;
				// return 2 - (unitsOfTypeBuilt[robotType] % 3) % 2; // 0 defend, 1 attack
			case 4:
				return strategy; // 0 turret defense, 1 roam
			case 5:
				return strategy;
			default:
				return 100;
		}
	}
}
