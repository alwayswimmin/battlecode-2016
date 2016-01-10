package defensetest;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
     @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        // You can instantiate variables here.
        Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
                RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
        Random rand = new Random(rc.getID());
        int myAttackRange = 0;
        Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();
        MapLocation congregate = new MapLocation(154, 298); int ct = 0;
        Direction dirToMove = directions[rand.nextInt(8)];

        if (rc.getType() == RobotType.ARCHON) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
                    int fate = rand.nextInt(1000);
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            if (rc.isCoreReady()) {

            while (true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    dirToMove = directions[rand.nextInt(8)];

                    Direction dirToBuild = directions[rand.nextInt(8)];
                    if (ct%2 == 0 && rc.canMove(dirToMove)) {
                        rc.move(dirToMove); ++ct;
                    } 

                    else {
                        RobotType t = robotTypes[rand.nextInt(7)];

                        for (int i = 0; i < 8; ++i) {

                            MapLocation trash = rc.getLocation().add(dirToBuild);
                            if (ct%10 == 1) {
                                if ((trash.x+trash.y)%2 == 1) {
                                    t = robotTypes[7];
                                } else {
                                    ++ct; continue;
                                }
                            }    

                            if (ct%10 == 3 || ct%10 == 5 || ct%10 == 7 || ct%10 == 9) {
                                if ((trash.x+trash.y)%2 == 1)
                                    continue;
                            }

                            if (rc.canBuild(dirToBuild, t)) {
                                rc.build(dirToBuild, t); ++ct; break;
                            }

                            dirToBuild.rotateLeft();
                        }
                    }

                    Clock.yield();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }} else if (rc.getType() != RobotType.TURRET) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
                int fate = rand.nextInt(1000);
                myAttackRange = rc.getType().attackRadiusSquared;
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while (true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    int fate = rand.nextInt(1000);
/*
                    if (fate % 5 == 3) {
                        // Send a normal signal
                        rc.broadcastSignal(80);
                    }
*/
                    boolean shouldAttack = false;

                    // If this robot type can attack, check for enemies within range and attack one
                    if (myAttackRange > 0) {
                        RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
                        RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
                        if (enemiesWithinRange.length > 0) {
                            shouldAttack = true;
                            // Check if weapon is ready
                            if (rc.isWeaponReady()) {
                                rc.attackLocation(enemiesWithinRange[rand.nextInt(enemiesWithinRange.length)].location);
                            }
                        } else if (zombiesWithinRange.length > 0) {
                            shouldAttack = true;
                            // Check if weapon is ready
                            if (rc.isWeaponReady()) {
                                rc.attackLocation(zombiesWithinRange[rand.nextInt(zombiesWithinRange.length)].location);
                            }
                        }
                    }

                    if (!shouldAttack) {
                        if (rc.isCoreReady()) {
                            if (fate < 600) {
                                // Check the rubble in that direction
                                if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                                    // Too much rubble, so I should clear it
                                    rc.clearRubble(dirToMove);
                                    // Check if I can move in this direction
                                } else if (rc.canMove(dirToMove)) {
                                    // Move
                                    rc.move(dirToMove);
                                } else {
                                    // Choose a random direction to try to move in
                                    dirToMove = directions[fate % 8];
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
        } else if (rc.getType() == RobotType.TURRET) {
            try {
                myAttackRange = rc.getType().attackRadiusSquared;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while (true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    // If this robot type can attack, check for enemies within range and attack one
                    if (rc.isWeaponReady()) {
                        RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
                        RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
                        if (enemiesWithinRange.length > 0) {
                            for (RobotInfo enemy : enemiesWithinRange) {
                                // Check whether the enemy is in a valid attack range (turrets have a minimum range)
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
