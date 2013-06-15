package br.net.ruggeri.qualcommchallenge.alljoyn;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

@BusInterface(name = "br.net.ruggeri.qualcommchallenge.alljoyn.HockeyInterface")
public interface HockeyInterface {

	@BusMethod
	public void ping() throws BusException;;

	@BusMethod
	public void sendPucket(double velocityX, double velocityY, int positionX,
			int from) throws BusException;

}
