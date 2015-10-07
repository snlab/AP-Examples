import java.util.List;
import java.util.Map;
import java.util.HashMap;

import maple.core.*;
import static maple.core.Assertion.*;

public class SP extends MapleFunction {

  // Control State

  MapleMap<Long, SwitchPort> hostLocationMap = newMap("hostlocations", Long.class, SwitchPort.class);
  MapleSet<Long> badHosts = newSet("BadHosts", Long.class);

  @Override protected Route onPacket(Packet p) {
    // learn
    hostLocationMap.put(p.ethSrc(), p.ingressPort());

    // access control
    if (!permitted(p)) { return Route.nullRoute; }

    // forward
    return forward(p, hostLocationMap.get(p.ethDst()));
  }

  public boolean permitted(Packet p) {
    return !(isSSH(p) || isBadHost(p));
  }

  public boolean isSSH(Packet p) {
    return p.satisfies(tcpDstEquals(22));
  }

  public boolean isBadHost(Packet p) {
    return (badHosts.contains(p.ethSrc()) || badHosts.contains(p.ethDst()));
  }

  public Route forward(Packet p, SwitchPort dstLoc) {
    if (null == dstLoc) {
      return Route.route(minSpanningTree(), edgePorts());
    }
    List<Link> path = shortestPath(p.ingressPort(), dstLoc);
    return Route.route(path, dstLoc);
  }

  public void onSwitchDown(long switchID) {
    for (Map.Entry<Long, SwitchPort> e : hostLocationMap.getMap().entrySet()) {
      if (e.getValue().getSwitch() == switchID) {
        hostLocationMap.delete(e.getKey());
      }
    }
  }
}
