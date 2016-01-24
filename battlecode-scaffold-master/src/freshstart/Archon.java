package freshstart;

import battlecode.common.*;
import java.util.*;

public class Archon extends Bot {
	private static int forcedMoveCounter = 0; // when Archon is forced to move in a fixed direction
	private static Direction forcedMoveDir;

	private static boolean shouldBuildScoutsInitially = true;
	private static MapLocation[] initialEnemyArchonLocations;
	private static MapLocation[] initialMyArchonLocations;

	private static MapLocation[] dens = new MapLocation[100];
	private static boolean[] denVisited = new boolean[100];
	private static int numberOfDens = 0;

	private static MapLocation[] neutrals = new MapLocation[100];
	private static boolean[] neutralVisited = new boolean[100];
	private static int numberOfNeutrals = 0;

	private static MapLocation[] parts = new MapLocation[100];
	private static boolean[] partsVisited = new boolean[100];
	private static int numberOfParts = 0;

	// can have duplicates, since archons move; this functions more as a cyclic stack
	private static MapLocation[] enemyArchons = new MapLocation[100];
	private static boolean[] enemyArchonVisited = new boolean[100];
	private static int numberOfEnemyArchons = 0;

	// for running away
	private static int turnsSinceEnemySeen = 100;
	private static int consecutiveTurnsOfEnemy = 0;

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

	private static int[] unitsOfTypeBuilt = {1, 0, 0, 0, 0, 0}; // includes self and activated units

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

		initialMyArchonLocations = rc.getInitialArchonLocations(myTeam);
		initialEnemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
		for(int i = 0; i < initialEnemyArchonLocations.length; ++i) {
			if(initialEnemyArchonLocations[i].distanceSquaredTo(myLocation) < 200) {
				shouldBuildScoutsInitially = false;
			}
		}
		if(rc.getZombieSpawnSchedule().getRounds()[0] < 100) {
			shouldBuildScoutsInitially = false;
		}
	}

	private static void action() throws GameActionException {

		// moved pretty far from my initial position; tell units to come to new location rather than report to old
		myLocation = rc.getLocation();
		if(myLocation.distanceSquaredTo(personalHQ) > 35) {
			personalHQ = myLocation;
			Radio.broadcastMoveCampLocation(myLocation, 1000);
		}
		updateVisited();

		// make forced moves if forced move counter is non-zero
		// implemented by yang for running away from corners
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
		RobotInfo[] friendWithinRange = rc.senseNearbyRobots(SIGHT_RANGE, myTeam);

		// processes friends and tries to heal them
		// TODO: some metric for best friend to heal
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
			// TODO: change to run in direction that maximizes distance, to prevent getting trapped against walls
			int enemyCount = hostileWithinRange.length;
			if(turnsSinceEnemySeen > 5) { // waits at least 5 turns between broadcasts
				Radio.broadcastDefendLocation(myLocation, 1000);
			}
			turnsSinceEnemySeen = 0;
			consecutiveTurnsOfEnemy++;
			// finds centroid of visible enemies
			for(int i = enemyCount; --i >= 0; ) {
				enemyCentroidX += hostileWithinRange[i].location.x;
				enemyCentroidY += hostileWithinRange[i].location.y;
			}
			enemyCentroid = new MapLocation(enemyCentroidX / enemyCount, enemyCentroidY / enemyCount);
		} else {
			// no enemies seen
			turnsSinceEnemySeen++;
			consecutiveTurnsOfEnemy = 0;
			if(turnsSinceEnemySeen == 15) {
				Radio.broadcastClearDefend(1000);
				// stop defending 
			}
		}

		// process if a neutral around us can be activated
		int canActivate = -1;
		for(int i = 0; i < neutralWithinRange.length; ++i) {
			if(distanceBetween(rc.getLocation(), neutralWithinRange[i].location) < 3) {
				canActivate = i;
			}
		}

		int typeToBuild;
		boolean addedRobot;

		// get information from scouts, give strategy assignments
		processSignals();

		// 0: ARCHON
		// 1: GUARD
		// 2: SCOUT
		// 3: SOLDIER
		// 4: TURRET/TTM
		// 5: VIPER
		if((rc.getRoundNum() > 200 || shouldBuildScoutsInitially) && (unitsOfTypeBuilt[2] < 1 || Math.random() > 0.95)) {
			typeToBuild = 2;
		} else if(Math.random() > 0.85) {
			typeToBuild = 5;
			// typeToBuild = 3; // actually don't build vipers for now
		} else if(rc.getRoundNum() > 300 && Math.random() > 0.70) {
			// typeToBuild = 1;
			typeToBuild = 3; // actually don't build guards for now, or probably ever
		} else {
			typeToBuild = 3;
		}
		boolean runAwayOverride = consecutiveTurnsOfEnemy % 20 > 15;
		if(runAwayOverride) {
			typeToBuild = 3;
			runAwayOverride = false;
			for(int k = hostileWithinRange.length; --k >= 0; ) {
				if(hostileWithinRange[k].type == RobotType.FASTZOMBIE) {
					// not going to outrun, try to fight off the zombie, or at the very least slow it down
					runAwayOverride = true;
					break;
				}
			}
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
			} else if(!runAwayOverride && enemyCentroid != null && !enemyCentroid.equals(myLocation)) {
				// if enemies are around, runs away from enemies
				// TODO: replace with new enemy avoidance strategy
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
			}
		}
		// idle, so move somewhere strategic
		moveSomewhere();

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
		// process incoming scout information concerning dens
		IdAndMapLocation newDen = Radio.getDenLocation();
		while(newDen != null) {
			boolean isNotReallyNewDen = false;
			for(int i = numberOfDens; --i >= 0; ) {
				if(newDen.location.equals(dens[i])) {
					isNotReallyNewDen = true;
					break;
				}
			}
			if(!isNotReallyNewDen) {
				// yay for double negative
				// add to known den locations
				dens[numberOfDens] = newDen.location;
				denVisited[numberOfDens] = false;
				numberOfDens++;
			}
			// read in another den
			newDen = Radio.getDenLocation();
		}

		// process incoming scout information concerning enemy archons
		IdAndMapLocation newEnemyArchon = Radio.getEnemyArchonLocation();
		while(newEnemyArchon != null) {
			// add to known archon footprint
			enemyArchons[numberOfEnemyArchons] = newEnemyArchon.location;
			enemyArchonVisited[numberOfEnemyArchons] = false;
			numberOfEnemyArchons++;
			// read in another den
			newEnemyArchon = Radio.getEnemyArchonLocation();
		}

		// process incoming scout information concerning neutrals
		IdAndMapLocation newNeutral = Radio.getNeutralLocation(); 
		while(newNeutral != null) {
			boolean isNotReallyNewNeutral = false;
			for(int i = numberOfNeutrals; --i >= 0; ) {
				if(newNeutral.location.equals(neutrals[i])) {
					isNotReallyNewNeutral = true;
					break;
				}
			}
			if(!isNotReallyNewNeutral) {
				// yay for double negative
				// add to known den locations
				neutrals[numberOfNeutrals] = newNeutral.location;
				neutralVisited[numberOfNeutrals] = false;
				numberOfNeutrals++;
			}
			// read in another neutral
			newNeutral = Radio.getNeutralLocation();
		}

		// process incoming scout information concerning parts
		IdAndMapLocation newParts = Radio.getPartsLocation();
		while(newParts != null) {
			boolean isNotReallyNewParts = false;
			for(int i = numberOfParts; --i >= 0; ) {
				if(newParts.location.equals(parts[i])) {
					isNotReallyNewParts = true;
					break;
				}
			}
			if(!isNotReallyNewParts) {
				// yay for double negative
				// add to known den locations
				parts[numberOfParts] = newParts.location;
				partsVisited[numberOfParts] = false;
				numberOfParts++;
			}
			// read in another part location
			newParts = Radio.getPartsLocation();
		}
	}

	private static void updateVisited() throws GameActionException {
		for(int i = numberOfDens; --i >= 0; ) {
			if(rc.canSense(dens[i])) {
				RobotInfo robotAtLocation = rc.senseRobotAtLocation(dens[i]);
				if(robotAtLocation == null || robotAtLocation.type != RobotType.ZOMBIEDEN) {
					denVisited[i] = true;
				}
			}
		}
		for(int i = numberOfParts; --i >= 0; ) {
			if(rc.canSense(parts[i])) {
				double partValue = rc.senseParts(parts[i]);
				if(partValue < 5.0) {
					partsVisited[i] = true;
				}
			}
		}
		for(int i = numberOfNeutrals; --i >= 0; ) {
			if(rc.canSense(neutrals[i])) {
				RobotInfo robotAtLocation = rc.senseRobotAtLocation(neutrals[i]);
				if(robotAtLocation == null || robotAtLocation.team != Team.NEUTRAL) {
					neutralVisited[i] = true;
				}
			}
		}
		for(int i = numberOfEnemyArchons; --i >= 0; ) {
			if(myLocation.distanceSquaredTo(enemyArchons[i]) <= 16) {
				RobotInfo robotAtLocation = rc.senseRobotAtLocation(enemyArchons[i]);
				if(robotAtLocation == null || robotAtLocation.team != enemyTeam) {
					enemyArchonVisited[i] = true;
				}
			}
		}
	}

	private static void moveSomewhere() throws GameActionException {
		// moves towards dens and neutrals and parts received via radio
		int closestDenIndex = -1;
		int bestDistanceToDen = 1000000;
		for(int i = numberOfDens; --i >= 0; ) {
			if(denVisited[i]) {
				// this den was already taken down, don't go there again
				continue;
			}
			int distanceToDen = myLocation.distanceSquaredTo(dens[i]);
			// check to make sure that den targets us and not enemy archons; if it targets enemy archons, usually not worth taking down, unless we're right next to it
			boolean targetsUs = true;
			for(int j = initialEnemyArchonLocations.length; --j >= 0; ) {
				if(initialEnemyArchonLocations[j].distanceSquaredTo(dens[i]) < distanceToDen) {
					targetsUs = false;
					break;
				}
			}
			if(targetsUs || distanceToDen <= 100) { // even if the den targets the enemy, if we're 10 steps away, we should take it down anyway
				if(closestDenIndex == -1 || distanceToDen < bestDistanceToDen) {
					closestDenIndex = i;
					bestDistanceToDen = distanceToDen;
				}
			}
		}

		int closestPartsIndex = -1;
		int bestDistanceToParts = 1000000;
		for(int i = numberOfParts; --i >= 0; ) {
			if(partsVisited[i]) {
				// these parts were already taken, don't go here again
				continue;
			}
			int distanceToParts = myLocation.distanceSquaredTo(parts[i]);
			if(closestPartsIndex == -1 || distanceToParts < bestDistanceToParts) {
				closestPartsIndex = i;
				bestDistanceToParts = distanceToParts;
			}
		}

		int closestNeutralIndex = -1;
		int bestDistanceToNeutral = 1000000;
		for(int i = numberOfNeutrals; --i >= 0; ) {
			if(neutralVisited[i]) {
				// this neutral were already activated, don't go here again
				continue;
			}
			int distanceToNeutral = myLocation.distanceSquaredTo(neutrals[i]);
			if(closestNeutralIndex == -1 || distanceToNeutral < bestDistanceToNeutral) {
				closestNeutralIndex = i;
				bestDistanceToNeutral = distanceToNeutral;
			}
		}

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

		// hard ignore enemies if round is premature
		if(rc.getRoundNum() < 1500) {
			closestEnemyArchonIndex = -1;
			bestDistanceToEnemyArchon = 1000000;
		}

		if(closestDenIndex == -1 && closestPartsIndex == -1 && closestNeutralIndex == -1 && closestEnemyArchonIndex == -1) {
			// nothing scouts have reported that we haven't seen already
			return;
		}

		if(bestDistanceToDen <= 100) {
			// we are close to den, go there first
			seekDen(dens[closestDenIndex]);
			return;
		}

		if(bestDistanceToNeutral <= 100) {
			// we are close to neutral, go there first
			seekNeutral(neutrals[closestNeutralIndex]);
			return;
		}

		if(bestDistanceToDen < bestDistanceToParts) {
			if(bestDistanceToDen < bestDistanceToNeutral) {
				if(bestDistanceToDen < bestDistanceToEnemyArchon) {
					// den is closest object
					seekDen(dens[closestDenIndex]);
					return;
				} else {
					// enemy is closest object
					seekEnemy(enemyArchons[closestEnemyArchonIndex]);
					return;
				}
			} else {
				if(bestDistanceToNeutral < bestDistanceToEnemyArchon) {
					// neutral is closest object
					seekNeutral(neutrals[closestNeutralIndex]);
					return;
				} else {
					// enemy is closest object
					seekEnemy(enemyArchons[closestEnemyArchonIndex]);
					return;
				}
			}
		} else {
			if(bestDistanceToParts < bestDistanceToNeutral) {
				if(bestDistanceToParts < bestDistanceToEnemyArchon) {
					// parts is closest object
					seekParts(parts[closestPartsIndex]);
					return;
				} else {
					// enemy is closest object
					seekEnemy(enemyArchons[closestEnemyArchonIndex]);
					return;
				}
			} else {
				if(bestDistanceToNeutral < bestDistanceToEnemyArchon) {
					// neutral is closest object
					seekNeutral(neutrals[closestNeutralIndex]);
					return;
				} else {
					// enemy is closest object
					seekEnemy(enemyArchons[closestEnemyArchonIndex]);
					return;
				}
			}
		}
	}

	private static MapLocation target = null;

	private static void seekDen(MapLocation location) throws GameActionException {
		if(target == null || !target.equals(location) || rc.getRoundNum() % 50 == 0) {
			Radio.broadcastMoveLocation(location, 100);
			target = location;
		}
		if(rc.isCoreReady()) {
			Nav.goTo(target);
		}
	}

	private static void seekEnemy(MapLocation location) throws GameActionException {
		if(target == null || !target.equals(location) || rc.getRoundNum() % 50 == 0) {
			Radio.broadcastMoveLocation(location, 1000);
			target = location;
		}
		if(rc.isCoreReady()) {
			Nav.goTo(target);
		}
	}

	private static void seekNeutral(MapLocation location)  throws GameActionException {
		if(target == null || !target.equals(location) || rc.getRoundNum() % 50 == 0) {
			Radio.broadcastMoveLocation(location, 35);
			target = location;
		}
		if(rc.isCoreReady()) {
			Nav.goTo(target);
		}
	}

	private static void seekParts(MapLocation location)  throws GameActionException {
		if(target == null || !target.equals(location) || rc.getRoundNum() % 50 == 0) {
			Radio.broadcastMoveLocation(location, 35);
			target = location;
		}
		if(rc.isCoreReady()) {
			Nav.goTo(target);
		}
	}

	private static int determineStrategy(int robotType) throws GameActionException {
		// determines strategies for requesting robots
		switch(robotType) {
			case 0:
			case 1:
				return 1;
				// return 1 - (unitsOfTypeBuilt[robotType] % 3) % 2; // 0 defend, 1 attack
			case 2:
				// return strategy; // 0 turret defense, 1 roam
				return rc.getRoundNum() >= 300 ? Math.random() > 0.6 ? 1 : 2 : 1;
			case 3:
				// return 1;
				return 1 - (unitsOfTypeBuilt[robotType] % 3) % 2; // 0 defend, 1 attack
			case 4:
				return strategy; // 0 turret defense, 1 roam
			case 5:
				return 1;
			default:
				return 100;
		}
	}
}
