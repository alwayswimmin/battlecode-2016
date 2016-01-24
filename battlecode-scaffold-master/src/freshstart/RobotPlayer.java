package freshstart;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController _rc) throws Exception {
		switch(_rc.getType()) {
			case ARCHON:
				Archon.run(_rc);
				break;
			case GUARD:
				Guard.run(_rc);
				break;
			case SCOUT:
				Scout.run(_rc);
				break;
			case SOLDIER:
				Soldier.run(_rc);
				break;
			case TTM:
			case TURRET:
				Turret.run(_rc);
				break;
			case VIPER:
				Viper.run(_rc);
				break;
			default:
				// this shouldn't happen
				throw new Exception("I am a bad robot.");
		}
	}
}
