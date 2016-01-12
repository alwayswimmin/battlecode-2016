package seekdentest;

import battlecode.common.*;

public class Viper extends Bot {
	public static void run(RobotController _rc) throws GameActionException {
		Bot.init(_rc);
		while(true) {
			action();
		}
	}
	private static void init() throws GameActionException {
		// things that run for the first time

	}
	private static void action() throws GameActionException {
		// take my turn

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

		Clock.yield();
	}
}
