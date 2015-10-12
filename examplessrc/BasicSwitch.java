import maple.core.*;

public class BasicSwitch extends MapleFunction {
	
  MapleMap<Long, SwitchPort> hostLocationMap =
      newMap("hostlocations", Long.class, SwitchPort.class, false);

  @Override
  protected Route onPacket(Packet p) {
    learn(p);
    return forward(p);
  }
  
  private Route forward(Packet p) {
    SwitchPort dstLoc = hostLocationMap.get(p.ethDst());
    if (null == dstLoc) {
      return Route.route(minSpanningTree(), edgePorts());
    } else {
      return Route.route(shortestPath(p.ingressPort(), dstLoc), dstLoc);
    }
  }

  public void learn(Packet p) {
    hostLocationMap.put(p.ethSrc(), p.ingressPort());  
  }

}
