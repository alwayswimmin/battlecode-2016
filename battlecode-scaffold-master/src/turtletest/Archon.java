package turtletest;

import battlecode.common.*;
import java.util.*;

public class Archon extends Bot {
	private static int scoutsBuilt = 0;
	private static Direction[] directions = new Direction[8];
	private static int forcedMoveCounter = 0; //when I'm forcing to Archon to override moves and go in a specific direction for a while
	private static Direction forcedMoveDir;

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
		personalHQ = rc.getLocation();
		neutralQueue = new LinkedList<MapLocation>();
		partsQueue = new LinkedList<MapLocation>();
		directions[0] = Direction.EAST;
		for (int i = 1; i < 8; ++i) {
			directions[i] = directions[i-1].rotateLeft();
		}
	}

	private static MapLocation neutralLocation = null;
	private static MapLocation partsLocation = null;

	private static LinkedList<MapLocation> neutralQueue;
	private static LinkedList<MapLocation> partsQueue;

	private static int radiusLimit = 4;

	private static double distanceBetween(MapLocation a, MapLocation b) {
		return (a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y);
	}

	private static int distToWall(MapLocation a, Direction dir) throws GameActionException {
		MapLocation b = a;
		for (int i = 0; i < 4; ++i) {
			b = b.add(dir);
			if (!rc.onTheMap(b))
				return i+1;
		}

		return 1000; //represents "very fat", out of SIGHT_RANGE
	}

	private static int rotationsTo(Direction a, Direction b) throws GameActionException {
		Direction c = a;

		for (int i = 0; i < 8; ++i) {
			if (c == b)
				return Math.min(i, 8-i);
			c = c.rotateLeft();
		}

		return 0;
	}

	private static IdAndMapLocation den = null;

	private static int turnsSinceEnemySeen = 100;

	private static RobotType[] robotTypes = {
		RobotType.ARCHON,
		RobotType.GUARD,
		RobotType.SCOUT,
		RobotType.SOLDIER,
		RobotType.TURRET,
		RobotType.VIPER
	};
	// 0: ARCHON
	// 1: GUARD
	// 2: SCOUT
	// 3: SOLDIER
	// 4: TURRET/TTM
	// 5: VIPER
	private static int[] unitsOfTypeBuilt = {1, 0, 0, 0, 0, 0};
	// includes activation.

	private static void action() throws GameActionException {
		// take my turn
		myLocation = rc.getLocation();
		if (forcedMoveCounter > 0 && rc.isCoreReady()) {
			forcedMoveCounter--;
			if (rc.canMove(forcedMoveDir)) {
				rc.move(forcedMoveDir); return;
			} else if (rc.canMove(forcedMoveDir.rotateLeft())) {
				rc.move(forcedMoveDir.rotateLeft()); return;
			} else if (rc.canMove(forcedMoveDir.rotateRight())) {
				rc.move(forcedMoveDir.rotateRight()); return;
			} else {
				forcedMoveCounter = 0;
			}
		}

		RobotInfo[] hostileWithinRange = rc.senseHostileRobots(myLocation, SIGHT_RANGE);
		RobotInfo[] neutralWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, Team.NEUTRAL);
		RobotInfo[] friendWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, rc.getTeam());

		for (int i = 0; i < friendWithinRange.length; ++i) {
			if (friendWithinRange[i].health != friendWithinRange[i].maxHealth && 
					(ATTACK_RANGE > distanceBetween(rc.getLocation(), friendWithinRange[i].location)) 
					&& (friendWithinRange[i].type != RobotType.ARCHON)) {
				rc.repair(friendWithinRange[i].location); break;
			}
		}

		int canActivate = -1;

		for (int i = 0; i < neutralWithinRange.length; ++i) {
			if (distanceBetween(rc.getLocation(), neutralWithinRange[i].location) < 3) {
				canActivate = i;
			}
		}

		double enemycenterx = 0, enemycentery = 0;
		MapLocation enemycenter = null;

		if(hostileWithinRange.length != 0) {
			if(turnsSinceEnemySeen > 5) {
				Radio.broadcastDefendLocation(myLocation, 1000);
			}
			turnsSinceEnemySeen = 0;
			for(int i = hostileWithinRange.length; --i >= 0; ) {
				enemycenterx += hostileWithinRange[i].location.x;
				enemycentery += hostileWithinRange[i].location.y;
			}
			enemycenter = new MapLocation((int) (enemycenterx / hostileWithinRange.length + (enemycenterx > 0 ? 1 : -1)), (int) (enemycentery / hostileWithinRange.length + (enemycentery > 0 ? 1 : -1)));
		} else {
			turnsSinceEnemySeen++;
			if(turnsSinceEnemySeen == 15) {
				Radio.broadcastClearDefend(1000);
			}

			den = Radio.getDenLocation();

			if(den != null) {
				Radio.broadcastMoveLocation(den.location, 1000);
			}
		}

		int typeToBuild;
		boolean addedRobot;
		switch(strategy) {
			case -1:
				if(rc.getTeamParts() >= 300) {
					strategy = 0;
				} else {
					strategy = 1;
				}
				action();
				break;
			case 0:
				processSignals();

				typeToBuild = scoutsBuilt++ < 2 ? 4 : Math.random() > 0.8 ? 2 : 4;
				// 0: ARCHON
				// 1: GUARD
				// 2: SCOUT
				// 3: SOLDIER
				// 4: TURRET/TTM
				// 5: VIPER

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
					} else if(enemycenter != null && !enemycenter.equals(myLocation)) {
						MapLocation dest = new MapLocation(4 * myLocation.x - 3 * enemycenter.x, 4 * myLocation.y - 3 * enemycenter.y);
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

				typeToBuild = unitsOfTypeBuilt[2] < 1 ? 2 : Math.random() > 0.9 ? 2 : Math.random() > 0.8 ? 1 : 3;
				// 0: ARCHON
				// 1: GUARD
				// 2: SCOUT
				// 3: SOLDIER
				// 4: TURRET/TTM
				// 5: VIPER

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
					} else if(enemycenter != null && !enemycenter.equals(myLocation)) {
						MapLocation dest = new MapLocation(4 * myLocation.x - 3 * enemycenter.x, 4 * myLocation.y - 3 * enemycenter.y);
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
