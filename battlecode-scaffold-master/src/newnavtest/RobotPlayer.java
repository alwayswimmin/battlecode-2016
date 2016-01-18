package newnavtest;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer extends Bot {
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		while(true) {
			myLocation = rc.getLocation();
			if(rc.isCoreReady()) {
				Nav.goTo(new MapLocation(172, 304));
			}
			Clock.yield();
		}
    }
}
