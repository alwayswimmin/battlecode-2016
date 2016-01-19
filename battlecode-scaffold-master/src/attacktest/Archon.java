package attacktest;

import battlecode.common.*;
import java.util.*;

public class Archon extends Bot {
	private static int scoutsBuilt = 0;
	private static int forcedMoveCounter = 0; // when Archon is forced to move in a fixed direction
	private static Direction forcedMoveDir;
	private static MapLocation neutralLocation = null;
	private static MapLocation partsLocation = null;
	private static MyQueue<MapLocation> moveQueue;
	private static MyQueue<MapLocation> neutralQueue;
	private static MyQueue<MapLocation> partsQueue;

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

	private static int[] unitsOfTypeBuilt = {1, 0, 0, 0, 0, 0}; // includes activated units

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
		// initializes Archon
		personalHQ = rc.getLocation();
		neutralQueue = new MyQueue<MapLocation>();
		partsQueue = new MyQueue<MapLocation>();
		
		moveQueue = new MyQueue<MapLocation>();
		MapLocation[] initialEnemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
		for(int i = 0; i < initialEnemyArchonLocations.length; ++i) {
			moveQueue.add(initialEnemyArchonLocations[i]);
		}
	}

	private static void action() throws GameActionException {
		// takes turn in following order:
		//     makes forced moves if necessary
		//     heals friends
		//     processes enemies (and possibly calls to defend)
		//     strategy 0: builds turrets and scouts
		//         processes signals
		//         activates neutrals if possible
		//         runs away from enemies if necessary
		//         tries to build new robots
		//         moves randomly without wandering too far from personal HQ
		//     strategy 1:
		//         processes signals
		//         activates neutrals if possible
		//         runs away from enemies if necessary
		//         tries to build new robots
		//         tries to move to parts and neutrals
		//         moves randomly

		myLocation = rc.getLocation();
		if(rc.getRoundNum() == 600) {
			personalHQ = myLocation;
		}
		// make forced moves if forced move counter is non-zero
		if(forcedMoveCounter > 0 && rc.isCoreReady()) {
			forcedMoveCounter--;
			if(rc.canMove(forcedMoveDir)) {
				rc.move(forcedMoveDir);
				return;
			} else if(rc.canMove(forcedMoveDir.rotateLeft())) {
				rc.move(forcedMoveDir.rotateLeft());
				return;
			} else if(rc.canMove(forcedMoveDir.rotateRight())) {
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
		for(int i = 0; i < friendWithinRange.length; ++i) {
			if(friendWithinRange[i].health != friendWithinRange[i].maxHealth && 
					(ATTACK_RANGE > distanceBetween(rc.getLocation(), friendWithinRange[i].location)) 
					&& (friendWithinRange[i].type != RobotType.ARCHON)) {
				rc.repair(friendWithinRange[i].location);
				break;
			}
		}

		int enemyCentroidX = 0, enemyCentroidY = 0;
		MapLocation enemyCentroid = null;

		if(hostileWithinRange.length != 0) {
			// if enemies in range, call friends to defend
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
		for(int i = 0; i < neutralWithinRange.length; ++i) {
			if(distanceBetween(rc.getLocation(), neutralWithinRange[i].location) < 3) {
				canActivate = i;
			}
		}

		int typeToBuild;
		boolean addedRobot;
		switch(strategy) {
			case -1:
				// assign a strategy to Archon
				
/*
				if(rc.getTeamParts() >= 300) {
					strategy = 0;
				} else {
					strategy = 1;
				}
*/
				strategy = 1;
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
				if(scoutsBuilt++ < 2){ // incrementing here seems buggy
					typeToBuild = 4;
				} else if(Math.random() > 0.8){
					typeToBuild = 2;
				} else {
					typeToBuild = 4;
				}
				addedRobot = false;
				if(rc.isCoreReady()) {
					// activates neutrals if possible
					if(canActivate > -1) {
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
						// if enemies are around, runs away from enemies
						int dx = myLocation.x - enemyCentroid.x;
						int dy = myLocation.y - enemyCentroid.y;
						MapLocation dest = new MapLocation(myLocation.x + 3 * dx, myLocation.y + 3 * dy);
						boolean[] closeTo = {false, false, false, false};
						Direction goTo = directions[1];
						for(int i = 0; i < 8; i += 2) {
							closeTo[i/2] = (distToWall(rc.getLocation(), directions[i]) <= 2);
						}
						for(int i = 0; i < 4; ++i) {
							if(closeTo[i] && closeTo[(i + 1) % 4]){
								goTo = directions[(2 * i + 4) % 8];
							}
						}
						MapLocation opt1 = new MapLocation(0, 0).add(goTo);
						MapLocation opt2 = new MapLocation(0, 0).add(goTo.rotateLeft().rotateLeft());
						Direction bestDirection;
						if(distanceBetween(opt1, dest) <= distanceBetween(opt2, dest)){
							bestDirection = goTo;
						} else {
							bestDirection = goTo.rotateLeft().rotateLeft();
						}
						if(goTo != directions[1] && rc.canMove(bestDirection)) {
							rc.move(bestDirection);
							forcedMoveDir = bestDirection;
							forcedMoveCounter = 8;
						} else {
							Nav.goTo(dest, new SPAll(hostileWithinRange));
						}
					} else if(rc.hasBuildRequirements(robotTypes[typeToBuild])) {
						// tries to build new robots
						Direction dirToBuild = Direction.EAST;
						if((rc.getRoundNum() <= 20 || rc.getRoundNum() > 600) && rc.getTeamParts() > 131){
							for(int i = 0; i < 8; i++) {
								MapLocation buildLocation = myLocation.add(dirToBuild);
								if(rc.canBuild(dirToBuild, robotTypes[typeToBuild]) && (buildLocation.x + buildLocation.y) % 2 == 1) {
									rc.build(dirToBuild, robotTypes[typeToBuild]);
									unitsOfTypeBuilt[typeToBuild]++;
									addedRobot = true;
									break;
								} else {
									dirToBuild = dirToBuild.rotateLeft();
								}
							}
						}
					}
					// moves randomly without wandering too far from personal HQ
					if(rc.isCoreReady() && personalHQ != null) {
						if(rc.isCoreReady()) {
							int rot = (int)(Math.random() * 8);
							Direction dirToMove = Direction.EAST;
							for(int i = 0; i < rot; ++i){
								dirToMove = dirToMove.rotateLeft();
							}
							for(int i = 0; i < 8; i++) {
								MapLocation dest = myLocation.add(dirToMove);
								if(rc.canMove(dirToMove) && dest.distanceSquaredTo(personalHQ) < radiusLimit) {
									Nav.goTo(dest);
									break;
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
				if(unitsOfTypeBuilt[2] < 1 || Math.random() > 0.95) {
					typeToBuild = 2;
				} else if(Math.random() > 0.80) {
					typeToBuild = 5;
				} else if(Math.random() > 0.85) {
					// typeToBuild = 1;
					typeToBuild = 3;
				} else {
					typeToBuild = 3;
				}
				addedRobot = false;
				if(rc.isCoreReady()) {
					// activates neutrals if possible
					if(canActivate > -1) {
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
						// if enemies are around, runs away from enemies
						int dx = myLocation.x - enemyCentroid.x;
						int dy = myLocation.y - enemyCentroid.y;
						MapLocation dest = new MapLocation(myLocation.x + 3 * dx, myLocation.y + 3 * dy);
						Nav.goTo(dest, new SPAll(hostileWithinRange));
					} else if(typeToBuild != -1 && rc.hasBuildRequirements(robotTypes[typeToBuild]) && (rc.getRoundNum() < 550 || rc.getTeamParts() > 130)) {
						// tries to build new robots
						Direction dirToBuild = Direction.EAST;
						for(int i = 0; i < 8; i++) {
							if(rc.canBuild(dirToBuild, robotTypes[typeToBuild])) {
								rc.build(dirToBuild, robotTypes[typeToBuild]);
								unitsOfTypeBuilt[typeToBuild]++;
								addedRobot = true;
								break;
							} else {
								dirToBuild = dirToBuild.rotateLeft();
							}
						}
					} else if(rc.isCoreReady()) {
						// tries to move to neutrals and parts
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
				// moves randomly
				if(rc.isCoreReady()) {
					int rot = (int)(Math.random() * 8);
					Direction dirToMove = Direction.EAST;
					for(int i = 0; i < rot; ++i){
						dirToMove = dirToMove.rotateLeft();
					}
					for(int i = 0; i < 8; ++i) {
						if(rc.canMove(dirToMove)) {
							Nav.goTo(myLocation.add(dirToMove));
							break;
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
		// moves towards neutrals and parts received via radio
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
		if(den != null) {
			Nav.goTo(den.location);
		}
	}


	private static int determineStrategy(int robotType) throws GameActionException {
		// determines strategies for requesting robots
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
				return 1;
			default:
				return 100;
		}
	}
}
