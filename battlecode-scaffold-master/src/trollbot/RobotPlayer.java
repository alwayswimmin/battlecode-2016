package trollbot;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController _rc) throws Exception {
		// System.out.println(_rc.getZombieSpawnSchedule().getRounds()[0]);
		boolean desert = _rc.getZombieSpawnSchedule().getRounds()[0] == 0;
		boolean swamp = _rc.getZombieSpawnSchedule().getRounds()[0] == 150;
		boolean first_game_was_swamp = _rc.getTeamMemory()[0] == 1;
		if(desert && !first_game_was_swamp) {
		switch(_rc.getType()) {
			case ARCHON:
				OldArchon.run(_rc);
				break;
			case GUARD:
				OldGuard.run(_rc);
				break;
			case SCOUT:
				OldScout.run(_rc);
				break;
			case SOLDIER:
				OldSoldier.run(_rc);
				break;
			case TTM:
			case TURRET:
				OldTurret.run(_rc);
				break;
			case VIPER:
				Viper.run(_rc);
				break;
			default:
				// this shouldn't happen
				throw new Exception("I am a bad robot.");
		}
		} else if(!swamp && !first_game_was_swamp) {
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
		} else {
			// SWAMP
			_rc.setTeamMemory(0, 1);
			while(true) Clock.yield();
		}
	}
}
