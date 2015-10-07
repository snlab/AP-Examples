import maple.core.*;
import static maple.core.Route.*;

public class SPBadHosts extends MapleFunction {

  public MapleSet<Long> badHosts = newSet("BadHosts", Long.class);

  MapleMap<Long, SwitchPort> hostLocationMap =
      newMap("hostlocations", Long.class, SwitchPort.class, false);

  @Override
  public Route onPacket(Packet p) {

    learn(p);

    if (badHosts.contains(p.ethSrc()) || badHosts.contains(p.ethDst())) {
      return nullRoute;
    }

    SwitchPort dstLoc = hostLocationMap.get(p.ethDst());
    if (null == dstLoc) {
      return Route.route(minSpanningTree(), edgePorts());
    }
    return Route.route(shortestPath(p.ingressPort(), dstLoc), dstLoc);
  }

  public void learn(Packet p) {
    hostLocationMap.put(p.ethSrc(), p.ingressPort());  
  }
}
