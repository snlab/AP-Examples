import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import maple.core.*;

public class YSN extends MapleFunction {

  // CONSTANTS
  final long FW_MAC_ADDRESS = 4L;
  SwitchPort fwLocation = new SwitchPort(2,3);

  /*
  final long FW_MAC_ADDRESS = 0x782BCBB43153L;  // WORKSTATION
  SwitchPort fwLocation = new SwitchPort(0x3c94d54fd280L,37841);
  */

  HashSet<SwitchPort> serviceFunctions = new HashSet<SwitchPort>();
  enum SecurityGroup { Campus, Science };

  // Network state: host locations
  MapleMap<Long, SwitchPort> hostLocationMap =
      newMap("hostlocations", Long.class, SwitchPort.class, false);

  // Policy Model: security groups, campus whitelist, science collaborators.
  MapleMap<Long, SecurityGroup> secGroupMap = 
      newMap("secGroupMap", Long.class, SecurityGroup.class, true);

  MapleSet<Long> campusWhitelist = newSet("campusWhitelist", Long.class);

  MapleMap<Long, String> hostLabMap = newMap("hostLabMap", Long.class, String.class);

  MapleSet<HostPair> collaborators = newSet("collaborators", HostPair.class);



  public YSN() {
    serviceFunctions.add(fwLocation);
    hostLocationMap.put(FW_MAC_ADDRESS, fwLocation);
  }

  @Override
  protected Route onPacket(Packet p) {

    learn(p);

    if (p.ethDst() == 0xffffffffffffL) {
      return forwardTo(p, null);
    }

    SecurityGroup srcGroup = secGroupMap.get(p.ethSrc());
    SecurityGroup dstGroup = secGroupMap.get(p.ethDst());

    if (null == srcGroup || null == dstGroup) { 
      return Route.nullRoute;
    }
    else if (srcGroup == SecurityGroup.Science && 
             dstGroup == SecurityGroup.Science) {

      if (inSameGroup(p)) { 
        return forwardTo(p, hostLocationMap.get(p.ethDst())); 
      } else if (areCollaborators(p)) {
        return forwardTo(p, hostLocationMap.get(p.ethDst()));
      } else {
        return Route.nullRoute;
      }

    }

    else if (srcGroup == SecurityGroup.Campus && 
             dstGroup == SecurityGroup.Campus) {
      return forwardTo(p, hostLocationMap.get(p.ethDst()));
    }

    else if (isWhitelisted(p)) {
        return forwardTo(p, hostLocationMap.get(p.ethDst()));
    }

    else { 
      if (p.ingressPort().equals(fwLocation)) {
        return forwardTo(p, hostLocationMap.get(p.ethDst()));
      } else {
        return forwardTo(p, fwLocation);
      }
    }
  }

  boolean isWhitelisted(Packet p) {
    return 
        campusWhitelist.contains(p.ethSrc()) || 
        campusWhitelist.contains(p.ethDst());
  }

  boolean areCollaborators(Packet p) {
    return 
        collaborators.contains(new HostPair(p.ethSrc(), p.ethDst())) &&
        collaborators.contains(new HostPair(p.ethDst(), p.ethSrc()));
  }

  boolean inSameGroup(Packet p) {
    String g1 = hostLabMap.get(p.ethSrc());
    String g2 = hostLabMap.get(p.ethDst());

    if (null == g1 || null == g2) { return false; }
    else return g1.equals(g2);
  }

  Route forwardTo(Packet p, SwitchPort dp) {
    if (null == dp) {
      Set<SwitchPort> ports = edgePorts();
      ports.remove(fwLocation);
      return Route.route(minSpanningTree(), ports);
    }
    List<Link> path = shortestPath(p.ingressPort(), dp);
    return Route.route(path, dp);
  }

  public void learn(Packet p) {
    if (!serviceFunctions.contains(p.ingressPort())) {
      hostLocationMap.put(p.ethSrc(), p.ingressPort()); 
    } 
  }

  public void onSwitchDown(long switchID) {
    for (Map.Entry<Long, SwitchPort> e : hostLocationMap.getMap().entrySet()) {
      if (e.getValue().getSwitch() == switchID) {
        hostLocationMap.delete(e.getKey());
      }
    }
  }
}
