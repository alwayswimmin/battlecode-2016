package final;

import battlecode.common.*;
import java.util.*;

public class Guard extends Bot {
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		init();
		while(true) {
			try {
				action();
			} catch(Exception e) {
				
			}
			Clock.yield();
		}
	}

	private static void init() throws GameActionException {
		// initializes soldier
		personalHQ = myLocation;
	}

	private static void action() throws GameActionException {
		updateHealth();
		update();
		Radio.process();
		myLocation = rc.getLocation();
		processSignals();
		
		if(Combat.target != null && myLocation.distanceSquaredTo(Combat.target) < SIGHT_RANGE) {
			RobotInfo robotAtTarget = rc.senseRobotAtLocation(Combat.target);
			if(rc.senseParts(Combat.target) < 1 && (robotAtTarget == null || robotAtTarget.team == Team.NEUTRAL || robotAtTarget.team == myTeam)) {
				setTarget(personalHQ); // nothing to see here, go home
			}
		}

		if(Combat.target != null) {
			rc.setIndicatorString(0, Combat.target.x + ", " + Combat.target.y);
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
