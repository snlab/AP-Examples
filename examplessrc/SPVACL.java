import maple.core.*;
import static maple.core.Route.*;
import maple.extra.*;
import java.util.LinkedList;
import java.io.*;

import com.google.gson.reflect.*;

public class SPVACL extends MapleFunction {

  Variable<LinkedList<ACRule>> acl;
  MapleMap<Long, SwitchPort> hostLocationMap;

  // Variable<LinkedList<MonitorRule>> monitor;

  public SPVACL() throws FileNotFoundException {
    acl = newVariable("acl", 
                      new LinkedList<ACRule>(), 
                      new TypeToken<LinkedList<ACRule>>() {}.getType()
                      );
    hostLocationMap = newMap("hostlocations", Long.class, SwitchPort.class);
    /*
    monitor = newVariable("monitor", 
                          new LinkedList<MonitorRule>(), 
                          new TypeToken<LinkedList<MonitorRule>>() {}.getType()
                          );
    */
  }

  @Override
  public Route onPacket(Packet p) {

    learn(p);

    ACRule.Action aclAction = ACL.matches(acl.read(),p);
    if (ACRule.Action.DENY == aclAction) return nullRoute;

    LinkedList<SwitchPort> destPorts = new LinkedList<SwitchPort>();

    SwitchPort dstLoc = hostLocationMap.get(p.ethDst());
    if (null == dstLoc) { 
      destPorts.addAll(edgePorts());
    } else {
      destPorts.add(dstLoc);
    }
    /*
    for (MonitorRule mrule : monitor.read()) {
      if (mrule.predicate.matches(p)) {
        destPorts.addAll(mrule.destinations);
      }
    }
    */

    if (destPorts.size() == 1) {
      return Route.route(shortestPath(p.ingressPort(), dstLoc), dstLoc);
    } else {
      return Route.route(minSpanningTree(), destPorts);
    }
    
  }

  public void learn(Packet p) {
    hostLocationMap.put(p.ethSrc(), p.ingressPort());  
  }

}
