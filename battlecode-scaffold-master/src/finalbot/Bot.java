// bot template inherited by all robots
// inspired by https://github.com/TheDuck314/battlecode2015/blob/master/teams/zephyr26_final/Bot.java
package finalbot;

import battlecode.common.*;

public class Bot {

	public static RobotController rc;

	protected static Team myTeam;
	protected static Team enemyTeam;
	protected static MapLocation personalHQ; // where to go when not on mission
	protected static int strategy;

	public static RobotInfo INFO;

	public static RobotType TYPE;
	public static int ID;
	public static int SIGHT_RANGE;
	public static int ATTACK_RANGE;
	public static double MAX_HEALTH;

	public static Direction[] directions = new Direction[8];
	public static double previousHealth = -1, currentHealth = -1;

	protected static MapLocation myLocation; // bot classes are responsible for keeping this up to date

	public static double distanceBetween(MapLocation a, MapLocation b) {
		// returns distance between two map locations
		return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
	}

	public static int distToWall(MapLocation a, Direction dir) throws GameActionException {
		// returns distance to wall in a given direction
		MapLocation b = a;
		for (int i = 0; i < 4; ++i) {
			b = b.add(dir);
			if (!rc.onTheMap(b))
				return i+1;
		}

		return 1000; //represents "very far", out of SIGHT_RANGE
	}

	public static int rotationsTo(Direction a, Direction b) throws GameActionException {
		Direction c = a;

		for (int i = 0; i < 8; ++i) {
			if (c == b)
				return Math.min(i, 8-i);
			c = c.rotateLeft();
		}

		return 0;
	}

	//did Bot take damage last turn?

	public static boolean tookDamage() throws GameActionException {
		return ((previousHealth == -1) ? false : (previousHealth > rc.getHealth()));
	}

	//update health counters after the turn
	public static void updateHealth() throws GameActionException {
		previousHealth = currentHealth;
		currentHealth = rc.getHealth();
	}

	public static MapLocation attackLocation(RobotInfo[] ZombieInfo, RobotInfo[] EnemyInfo, MapLocation[] ScoutInfo) {
		boolean prioritizeArchon = rc.getRoundNum() >= 2900;

		if(rc.getType() == RobotType.TURRET) {

			MapLocation attackLocation = null;
			MapLocation myLocation2 = rc.getLocation();
			double distanceTo = 1000000000, x;

			if(prioritizeArchon) {

				for (int i = 0; i < EnemyInfo.length; ++i) {
					x = distanceBetween(myLocation2, EnemyInfo[i].location);
	
					if (EnemyInfo[i].type == RobotType.ARCHON && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
						if (x < distanceTo) {
							distanceTo = x;
							attackLocation = EnemyInfo[i].location;
						}
					}
				}
				if (attackLocation != null)
					return attackLocation;

			}


			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.TURRET && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.VIPER && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.SOLDIER && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.GUARD && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;


			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.RANGEDZOMBIE && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;
/*
			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.FASTZOMBIE && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.STANDARDZOMBIE && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;
			
			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.BIGZOMBIE && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;
*/
// instead, just use default zombie attacks except for ranged
			
			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

// --end modification--
			if(!prioritizeArchon) {

				for (int i = 0; i < EnemyInfo.length; ++i) {
					x = distanceBetween(myLocation2, EnemyInfo[i].location);
	
					if (EnemyInfo[i].type == RobotType.ARCHON && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
						if (x < distanceTo) {
							distanceTo = x;
							attackLocation = EnemyInfo[i].location;
						}
					}
				}
				if (attackLocation != null)
					return attackLocation;

			}
			
			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;


			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.ZOMBIEDEN && (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < ScoutInfo.length; ++i) {
				x = distanceBetween(myLocation2, ScoutInfo[i]);

				if (ATTACK_RANGE >= x && GameConstants.TURRET_MINIMUM_RANGE <= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ScoutInfo[i];
					}
				}
			}

			return attackLocation;

		} else {
			MapLocation attackLocation = null;
			MapLocation myLocation2 = rc.getLocation();
			double distanceTo = 1000000000, x;

			if(prioritizeArchon) {

				for (int i = 0; i < EnemyInfo.length; ++i) {
					x = distanceBetween(myLocation2, EnemyInfo[i].location);
	
					if (EnemyInfo[i].type == RobotType.ARCHON && (ATTACK_RANGE >= x)) {
						if (x < distanceTo) {
							distanceTo = x;
							attackLocation = EnemyInfo[i].location;
						}
					}
				}

			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.TURRET && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.VIPER && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.SOLDIER && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (EnemyInfo[i].type == RobotType.GUARD && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;



			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.RANGEDZOMBIE && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;
/*
			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.FASTZOMBIE && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.STANDARDZOMBIE && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;
			
			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.BIGZOMBIE && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;
*/

// instead, just use default zombie attacks except for ranged
			
			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ATTACK_RANGE >= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

// --end modification--

			if(!prioritizeArchon) {

				for (int i = 0; i < EnemyInfo.length; ++i) {
					x = distanceBetween(myLocation2, EnemyInfo[i].location);
	
					if (EnemyInfo[i].type == RobotType.ARCHON && (ATTACK_RANGE >= x)) {
						if (x < distanceTo) {
							distanceTo = x;
							attackLocation = EnemyInfo[i].location;
						}
					}
				}

			}
			
			for (int i = 0; i < EnemyInfo.length; ++i) {
				x = distanceBetween(myLocation2, EnemyInfo[i].location);

				if (ATTACK_RANGE >= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = EnemyInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ZombieInfo[i].type == RobotType.ZOMBIEDEN && (ATTACK_RANGE >= x)) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < ZombieInfo.length; ++i) {
				x = distanceBetween(myLocation2, ZombieInfo[i].location);

				if (ATTACK_RANGE >= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ZombieInfo[i].location;
					}
				}
			}

			if (attackLocation != null)
				return attackLocation;

			for (int i = 0; i < ScoutInfo.length; ++i) {
				x = distanceBetween(myLocation2, ScoutInfo[i]);

				if (ATTACK_RANGE >= x) {
					if (x < distanceTo) {
						distanceTo = x;
						attackLocation = ScoutInfo[i];
					}
				}
			}

			return attackLocation;
		}
	}

	public static void tighten(SafetyPolicy policy) throws GameActionException {
		RobotInfo[] friends = rc.senseNearbyRobots(100000, myTeam);
		int friendCentroidX = 0, friendCentroidY = 0, friendCount = friends.length;
		if(friendCount == 0) {
			return;
		}
		for(int i = friends.length; --i >= 0; ) {
			friendCentroidX += friends[i].location.x;
			friendCentroidY += friends[i].location.y;
		}
		MapLocation friendCentroid = new MapLocation(friendCentroidX / friendCount, friendCentroidY / friendCount);
		if(friendCentroid.equals(myLocation)) {
			return;
		}
		if(rc.isCoreReady()) {
			Nav.goTo(friendCentroid, policy);
		}
	}

	public static void tighten() throws GameActionException {
		tighten(null);
	}

	protected static void init(RobotController _rc) throws GameActionException {
		// initializes bot fields
		rc = _rc;

		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		strategy = -1;

		myLocation = rc.getLocation();

		Radio.init();

		TYPE = rc.getType();
		ID = rc.getID();
		SIGHT_RANGE = TYPE.sensorRadiusSquared;
		ATTACK_RANGE = TYPE.attackRadiusSquared;
		MAX_HEALTH = TYPE.maxHealth;

		update();

		directions[0] = Direction.EAST;
		for (int i = 1; i < 8; ++i) {
			directions[i] = directions[i-1].rotateLeft();
		}
	}

	protected static void update() throws GameActionException {
		TYPE = rc.getType();
		INFO = new RobotInfo(rc.getID(), rc.getTeam(), TYPE, rc.getLocation(), rc.getCoreDelay(), rc.getWeaponDelay(), TYPE.attackPower, rc.getHealth(), TYPE.maxHealth, rc.getZombieInfectedTurns(), rc.getViperInfectedTurns());
	}
}
