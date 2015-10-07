import maple.core.*;
import static maple.core.Assertion.*;
import com.google.gson.reflect.*;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

public class GBP extends MapleFunction {

  public MapleMap<Long, SwitchPort> hostLocationMap =
      newMap("hostlocations", Long.class, SwitchPort.class, false);

  public MapleMap<Long, Integer> hostGroup = 
      newMap("hostGroup", Long.class, Integer.class);

  public MapleMap<Integer, Group> groups = 
      newMap("groups", Integer.class, Group.class);

  public MapleMap<String, GBPRule> contracts =
      newMap("contracts", String.class, GBPRule.class);

  @Override
  protected Route onPacket(Packet p) {
    learn(p);

    if (p.satisfies(ethDstEquals(0xffffffffffffL)) || p.satisfies(isARP())) {
      return forward(p, hostLocationMap.get(p.ethDst()));
    }

    Integer sgid = hostGroup.get(p.ethSrc());
    Integer dgid = hostGroup.get(p.ethDst());

    if (null == sgid || null == dgid) { return Route.nullRoute; }

    Group sg = groups.get(sgid);
    Group dg = groups.get(dgid);

    boolean applies = false;

    for (GBPRule rule : commonContracts(sg.consumedContracts, dg.providedContracts)) {
      if (rule.direction == GBPRule.Direction.In) {
        applies = matchesDstPort(p, rule.startPort, rule.endPort);
      }
      if (rule.direction == GBPRule.Direction.Out) {
        applies = matchesSrcPort(p, rule.startPort, rule.endPort);
      }
      if (rule.direction == GBPRule.Direction.Bi) {
        applies = 
            matchesSrcPort(p, rule.startPort, rule.endPort) || 
            matchesDstPort(p, rule.startPort, rule.endPort);
      }

      if (applies) { return forward(p, rule); }
    }

    for (GBPRule rule : commonContracts(sg.providedContracts, dg.consumedContracts)) {
      if (rule.direction == GBPRule.Direction.In) {
        applies = matchesSrcPort(p, rule.startPort, rule.endPort);
      }
      if (rule.direction == GBPRule.Direction.Out) {
        applies = matchesDstPort(p, rule.startPort, rule.endPort);
      }
      if (rule.direction == GBPRule.Direction.Bi) {
        applies = 
            matchesSrcPort(p, rule.startPort, rule.endPort) || 
            matchesDstPort(p, rule.startPort, rule.endPort);
      }

      if (applies) { return forward(p, rule); }
    }
    return Route.nullRoute;
  }

  boolean matchesDstPort(Packet p, int startPort, int endPort) {
    for (int port = startPort; port <= endPort; port++) {
      if (p.satisfies(tcpDstEquals(port))) return true;
    }
    return false;
  }
  boolean matchesSrcPort(Packet p, int startPort, int endPort) {
    for (int port = startPort; port <= endPort; port++) {
      if (p.satisfies(tcpSrcEquals(port))) return true;
    }
    return false;
  }

  LinkedList<GBPRule> 
  commonContracts(HashSet<String> contracts1, HashSet<String> contracts2) {
    LinkedList<GBPRule> common = new LinkedList<GBPRule>();
    for (String rule1 : contracts1) {
      if (contracts2.contains(rule1)) {
        GBPRule c = contracts.get(rule1);
        common.add(c);
      }
    }
    return common;
  }

  public void learn(Packet p) {
    hostLocationMap.put(p.ethSrc(), p.ingressPort());
  }

  public Route forward(Packet p, GBPRule rule) {
    if (rule.action.permit) {
      Long dst; 
      if (null == rule.action.redirectTo) {
        dst = p.ethDst();
      } else {
        dst = rule.action.redirectTo;
      }
      return forward(p, hostLocationMap.get(dst));

    } else {
      return Route.nullRoute;
    }
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
