// class for managing signaling and communication over channels
package turtletest;

import battlecode.common.*;
import java.util.*;

// Channel List
// =========================================================
// 0: instruct unit message1 to tune in to channel message2
// 1: enemy encountered
// 2: zombie den encountered
// 3: neutral encountered
// 4: parts encountered
// 5: move camp
// 6: move order
// 7: defend order
// 8: clear defend order
// 9: turret attack order
// ...
// 30: unit-specific strategy assignment
// 31: unit-specific move order
// 32: no message

class MySignal {
	// our version of Signal
	public int id;
	public MapLocation location;
	public int message1;
	public int message2;
	public MySignal(int _id, MapLocation _location, int _message1, int _message2) {
		id = _id;
		location = _location;
		message1 = _message1;
		message2 = _message2;
	}
}

class IdAndMapLocation {
	public int id;
	public MapLocation location;
	public IdAndMapLocation(int _id, MapLocation _location) {
		id = _id;
		location = _location;
	}
}

public class Radio extends Bot {
	public static Queue<MySignal>[] channelQueue = new Queue[33];
	public static Queue<MySignal> enemySignal;

	public static void init() throws GameActionException {
		// initializes channel queues
		for(int channel = 33; --channel >= 0; ) {
			channelQueue[channel] = new LinkedList<MySignal>();
		}
		enemySignal = new LinkedList<MySignal>();
	}

	public static void broadcast(int channel, int message1, int message2, int radius) throws GameActionException {
		// broadcasts message to a specified channel
		// uses 5 bits for channel, 27 bits for message1, and 32 bits for message2
		rc.broadcastMessageSignal((channel << 27) | message1, message2, radius);
	}

	public static void broadcastLocation(int channel, MapLocation location, int radius) throws GameActionException {
		// broadcasts location to a specified channel
		broadcast(channel, location.x + 16000, location.y + 16000, radius);
	}

	public static IdAndMapLocation getLocation(int channel) throws GameActionException {
		// reads location from a specified channel
		if(channelQueue[channel].isEmpty()){
			return null;
		}
		MySignal signal = channelQueue[channel].remove();
		MapLocation location = new MapLocation(signal.message1 - 16000, signal.message2 - 16000);
		return new IdAndMapLocation(signal.id, location);
	}

	public static void process() throws GameActionException {
		// processes all new signals and assigns them to channels
		Signal[] incomingSignals = rc.emptySignalQueue();
		for(int i = incomingSignals.length; --i >= 0; ) {
			int id = incomingSignals[i].getID();
			MapLocation location = incomingSignals[i].getLocation();
			int[] message = incomingSignals[i].getMessage();
			if(incomingSignals[i].getTeam() == myTeam) {
				if(message == null) {
					channelQueue[32].add(new MySignal(id, location, 0, -1));
				} else {
					int channel = message[0] >>> 27;
					message[0] ^= (channel << 27);
					MySignal mysignal = new MySignal(id, location, message[0], message[1]);
					channelQueue[channel].add(mysignal);
				}
			} else {
				// if message1 is 1 then the enemy is an Archon or Scout
				MySignal mysignal = new MySignal(id, location, message == null ? 0 : 1, -1);
				enemySignal.add(mysignal);
			}
		}
	}

	public static void clear() throws GameActionException {
		// reinitializes Radio
		init();
	}

	public static void broadcastDenLocation(MapLocation denLocation, int radius) throws GameActionException {
		broadcastLocation(2, denLocation, radius);
	}

	public static IdAndMapLocation getDenLocation() throws GameActionException {
		return getLocation(2);
	}

	public static void broadcastNeutralLocation(MapLocation neutralLocation, int radius) throws GameActionException {
		broadcastLocation(3, neutralLocation, radius);
	}

	public static IdAndMapLocation getNeutralLocation() throws GameActionException {
		return getLocation(3);
	}

	public static void broadcastPartsLocation(MapLocation partsLocation, int radius) throws GameActionException {
		broadcastLocation(4, partsLocation, radius);
	}

	public static IdAndMapLocation getPartsLocation() throws GameActionException {
		return getLocation(4);
	}

	public static void broadcastMoveLocation(MapLocation dest, int radius) throws GameActionException {
		broadcastLocation(6, dest, radius);
	}

	public static IdAndMapLocation getMoveLocation() throws GameActionException {
		return getLocation(6);
	}

	public static void broadcastMoveCampLocation(MapLocation dest, int radius) throws GameActionException {
		broadcastLocation(5, dest, radius);
	}

	public static IdAndMapLocation getMoveCampLocation() throws GameActionException {
		return getLocation(5);
	}

	public static void broadcastDefendLocation(MapLocation dest, int radius) throws GameActionException {
		broadcastLocation(7, dest, radius);
	}

	public static IdAndMapLocation getDefendLocation() throws GameActionException {
		return getLocation(7);
	}

	public static void broadcastClearDefend(int radius) throws GameActionException {
		broadcast(8, 0, 0, radius);
	}

	public static int getClearDefend() throws GameActionException {
		if(channelQueue[8].isEmpty()) {
			return -1;
		}
		MySignal signal = channelQueue[8].remove();
		return signal.id;
	}

	public static void broadcastTurretAttack(MapLocation enemy, int radius) throws GameActionException {
		broadcastLocation(9, enemy, radius);
	}

	public static IdAndMapLocation getTurretAttack() throws GameActionException {
		return getLocation(9);
	}

	public static void broadcastInitialStrategyRequest(int radius) throws GameActionException {
		rc.broadcastSignal(radius);
	}

	public static int getInitialStrategyRequest() throws GameActionException {
		if(channelQueue[32].isEmpty()) {
			return -1;
		}
		MySignal signal = channelQueue[32].remove();
		return signal.id;

	}

	public static void broadcastStrategyAssignment(int strategy, int radius) throws GameActionException {
		broadcast(30, strategy, 0, radius);
	}

	public static int getStrategyAssignment() throws GameActionException {
		if(channelQueue[30].isEmpty()) {
			return -1;
		}
		MySignal signal = channelQueue[30].remove();
		return signal.message1;
	}

	public static void broadcastTuneCommand(int id, int channel, int radius) throws GameActionException {
		broadcast(0, id, channel, radius);
	}

	public static int getTuneCommand() throws GameActionException {
		if(channelQueue[0].isEmpty()) {
			return -1;
		}
		MySignal signal = channelQueue[0].remove();
		if(signal.message1 == ID) {
			return signal.message2;
		} else {
			return -1;
		}
	}
}
