package freshstart;

import battlecode.common.*;
import java.util.*;

public class Soldier extends Bot {
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			action();
			Clock.yield();
		}
	}

	private static void init() throws GameActionException {
		// initializes soldier
		personalHQ = myLocation;
	}

	private static void action() throws GameActionException {
		updateHealth();
		Radio.process();
		myLocation = rc.getLocation();
		processSignals();
		
		if(Combat.target != null && myLocation.distanceSquaredTo(Combat.target) < 10) {
			RobotInfo robotAtTarget = rc.senseRobotAtLocation(Combat.target);
			if(robotAtTarget == null || robotAtTarget.team == Team.NEUTRAL || robotAtTarget.team == myTeam) {
				setTarget(personalHQ); // nothing to see here, go home
			}
		}

		Combat.action(); // micro attacks

		Radio.clear();
	}

	private static void processSignals() throws GameActionException {
		IdAndMapLocation newDefend = null, newMove = null; int clearDefend = -1; int clearOrders = -1;
		newDefend = Radio.getDefendLocation(); newMove = Radio.getMoveLocation(); clearDefend = Radio.getClearDefend(); clearOrders = Radio.getClear();
		IdAndMapLocation newHQ = Radio.getMoveCampLocation();
		if(newHQ != null) {
			personalHQ = newHQ.location;
		}
		if(newMove != null) {
			setTarget(newMove.location);
		}
	}
	private static void setTarget(MapLocation loc) throws GameActionException {
		Combat.target = loc;
	}
}
