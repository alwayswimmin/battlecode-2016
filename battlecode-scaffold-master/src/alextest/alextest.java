package alextest;

import battlecode.common.*;
import java.util.Random;

public class alextest {

  /**
   * run() is the method that is called when a robot is instantiated in the Battlecode world.
   * If this method returns, the robot dies!
   **/
  @SuppressWarnings("unused")
  public static void run(RobotController rc) {
    // You can instantiate variables here.
    Direction[] directions = {
      Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
      Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST
    };
    RobotType[] robotTypes = {
      RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
      RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET
    };
    Random rand = new Random(rc.getID());
    int myAttackRange = 0;
    Team myTeam = rc.getTeam();
    Team enemyTeam = myTeam.opponent();

    if (rc.getType() == RobotType.ARCHON) {
      while(true){
        try {
          if (rc.isCoreReady()) {
            RobotType turret = RobotType.TURRET;
            if (rc.hasBuildRequirements(turret)) {
              Direction dirToBuild = directions[rand.nextInt(8)];
              for (int i = 0; i < 8; i++) {
                if (rc.canBuild(dirToBuild, turret)) {
                  rc.build(dirToBuild, turret);
                  break;
                } else {
                  dirToBuild = dirToBuild.rotateLeft();
                }
              }
            }
          }
        } catch (Exception e) {
          System.out.println(e.getMessage());
          e.printStackTrace();
        }
      }
    } else if (rc.getType() != RobotType.TURRET) {
    } else if (rc.getType() == RobotType.TURRET) {
      try {
        myAttackRange = rc.getType().attackRadiusSquared;
      } catch (Exception e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
      }
      while (true) {
        try {
          if (rc.isWeaponReady()) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            if (enemiesWithinRange.length > 0) {
              for (RobotInfo enemy : enemiesWithinRange) {
                if (rc.canAttackLocation(enemy.location)) {
                  rc.attackLocation(enemy.location);
                  break;
                }
              }
            } else if (zombiesWithinRange.length > 0) {
              for (RobotInfo zombie : zombiesWithinRange) {
                if (rc.canAttackLocation(zombie.location)) {
                  rc.attackLocation(zombie.location);
                  break;
                }
              }
            }
          }
          Clock.yield();
        } catch (Exception e) {
          System.out.println(e.getMessage());
          e.printStackTrace();
        }
      }
    }
  }
}
