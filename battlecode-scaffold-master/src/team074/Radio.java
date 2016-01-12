package team074;

import battlecode.common.*;
import java.util.*;

// Channel List
// ---
// 0: instruct unit message1 to tune in to channel message2
// 1: enemy encountered
// 2: zombie den encountered
// 3: neutral encountered
// 4: parts encountered
// 5: move camp
// 6: move order
// 7: defend order
// 8: clear defend order
// ...
// 30: unit-specific strategy assignment
// 31: unit-specific move order
// 32: no message

class MySignal {
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
// turns signals into broadcasts with channels
	public static Queue<MySignal>[] channelQueue = new Queue[33]; // 32 is for no message
	public static Queue<MySignal> enemySignal;

	public static void init() throws GameActionException {
		for(int channel = 33; --channel >= 0; ) {
			channelQueue[channel] = new LinkedList<MySignal>();
		}
		enemySignal = new LinkedList<MySignal>();
	}
	// five bits for channel, 27 bits for message1, 32 bits for message2
	public static void broadcast(int channel, int message1, int message2, int radius) throws GameActionException {
		rc.broadcastMessageSignal((channel << 27) | message1, message2, radius);
		// System.out.println("broadcast " + ((channel << 27) + message1) + " " + message2);
	}

	public static void process() throws GameActionException {
		Signal[] incomingSignals = rc.emptySignalQueue();
		for(int i = incomingSignals.length; --i >= 0; ) {
			if(incomingSignals[i].getTeam() == myTeam) {
				int[] message = incomingSignals[i].getMessage();
				if(message == null) {
					channelQueue[32].add(new MySignal(incomingSignals[i].getID(), incomingSignals[i].getLocation(), 0, -1));
				} else {
					channelQueue[message[0] >>> 27].add(new MySignal(incomingSignals[i].getID(), incomingSignals[i].getLocation(), message[0] ^ ((message[0] >>> 27) << 27), message[1]));
				}
			} else {
				enemySignal.add(new MySignal(incomingSignals[i].getID(), incomingSignals[i].getLocation(), (incomingSignals[i].getMessage() == null ? 0 : 1), -1)); // we don't care about the message, but if message1 == 1 then the enemy is an Archon or Scout
			}
		}
	}

	public static void clear() throws GameActionException {
		init();
	}

	public static void broadcastDenLocation(MapLocation denLocation, int radius) throws GameActionException {
		broadcast(2, denLocation.x + 16000, denLocation.y + 16000, radius);
	}

	public static IdAndMapLocation getDenLocation() throws GameActionException {
		if(channelQueue[2].isEmpty()) {
			return null;
		}
		MySignal signal = channelQueue[2].remove();
		return new IdAndMapLocation(signal.id, new MapLocation(signal.message1 - 16000, signal.message2 - 16000));
	}

	public static void broadcastNeutralLocation(MapLocation neutralLocation, int radius) throws GameActionException {
		broadcast(3, neutralLocation.x + 16000, neutralLocation.y + 16000, radius);
	}

	public static IdAndMapLocation getNeutralLocation() throws GameActionException {
		if(channelQueue[3].isEmpty()) {
			return null;
		}
		MySignal signal = channelQueue[3].remove();
		return new IdAndMapLocation(signal.id, new MapLocation(signal.message1 - 16000, signal.message2 - 16000));
	}

	public static void broadcastPartsLocation(MapLocation partsLocation, int radius) throws GameActionException {
		broadcast(2, partsLocation.x + 16000, partsLocation.y + 16000, radius);
	}

	public static IdAndMapLocation getPartsLocation() throws GameActionException {
		if(channelQueue[4].isEmpty()) {
			return null;
		}
		MySignal signal = channelQueue[4].remove();
		return new IdAndMapLocation(signal.id, new MapLocation(signal.message1 - 16000, signal.message2 - 16000));
	}

	public static void broadcastMoveLocation(MapLocation dest, int radius) throws GameActionException {
		broadcast(6, dest.x + 16000, dest.y + 16000, radius);
	}

	public static IdAndMapLocation getMoveLocation() throws GameActionException {
		if(channelQueue[6].isEmpty()) {
			return null;
		}
		MySignal signal = channelQueue[6].remove();
		return new IdAndMapLocation(signal.id, new MapLocation(signal.message1 - 16000, signal.message2 - 16000));
	}

	public static void broadcastDefendLocation(MapLocation dest, int radius) throws GameActionException {
		broadcast(7, dest.x + 16000, dest.y + 16000, radius);
	}

	public static IdAndMapLocation getDefendLocation() throws GameActionException {
		if(channelQueue[7].isEmpty()) {
			return null;
		}
		MySignal signal = channelQueue[7].remove();
		return new IdAndMapLocation(signal.id, new MapLocation(signal.message1 - 16000, signal.message2 - 16000));
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
		if(!channelQueue[0].isEmpty()) {
			MySignal signal = channelQueue[0].remove();
			if(signal.message1 == ID) {
				return signal.message2;
			}
		}
		return -1;
	}
}
