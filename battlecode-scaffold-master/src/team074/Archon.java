package team074;

import battlecode.common.*;
import java.util.*;

public class Archon extends Bot {
	private static int scoutsBuilt = 0;

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
	}

	private static MapLocation neutralLocation = null;
	private static MapLocation partsLocation = null;

	private static LinkedList<MapLocation> neutralQueue;
	private static LinkedList<MapLocation> partsQueue;

	private static double distanceBetween(MapLocation a, MapLocation b) {
		return (a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y);
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

		processSignals();

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

		int typeToBuild = scoutsBuilt++ < 1 ? 2 : (Math.random() > 0.1 ? 3 : 1);
		// 0: ARCHON
		// 1: GUARD
		// 2: SCOUT
		// 3: SOLDIER
		// 4: TURRET/TTM
		// 5: VIPER

		boolean addedRobot = false;
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
			} else if (rc.hasBuildRequirements(robotTypes[typeToBuild])) {
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
					rc.move(dirToMove); break;
				}

				dirToMove = dirToMove.rotateLeft();
			}
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
				return 2 - (unitsOfTypeBuilt[robotType] % 3) % 2; // 0 defend, 1 attack
			case 2:
				return 100;
			case 3:
				return 2 - (unitsOfTypeBuilt[robotType] % 3) % 2; // 0 defend, 1 attack
			case 4:
			case 5:
			default:
				return 100;
		}
	}
}
