package navtest;

import battlecode.common.*;

public class InitialNav {
	private static RobotController rc;
	public static int broadcastRadius = 1000;
	public static MapLocation myRallyPoint;
	private static Signal[] signalQueue;

	public static void broadcastPosition() throws GameActionException {
		// for archon
		rc.broadcastSignal(broadcastRadius);
	}

	public static void setRallyPoint() throws GameActionException {
		// note that rally point may differ for various individual robots
		signalQueue = rc.emptySignalQueue();
		for(int i = signalQueue.length; --i >= 0; ) {
			
		}
	}
}
