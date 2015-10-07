import maple.core.*;
import static maple.core.Route.*;
import maple.routing.*;
import java.util.List;
import java.util.Set;

public class ECMP extends MapleFunction {

  MapleMap<Long, SwitchPort> hostLocationMap =
    newMap("hostlocations", Long.class, SwitchPort.class);

  public Route onPacket(Packet p) {
    learn(p);
    SwitchPort srcLoc = p.ingressPort();
    Set<Long> switches = switches();
    Set<Link> links = links();
    Set<SwitchPort> edgePorts = edgePorts();
    Digraph gr = Digraph.unitDigraph(switches, links);
    SwitchPort dstLoc = hostLocationMap.get(p.ethDst());

    if (null == dstLoc) { 

      // Destination location unknown, so broadcast everywhere.
      return route(gr.minSpanningTree(), edgePorts);

    } else {

      int hash = (int) (p.ethSrc() + p.ethDst());
      List<Link> path = gr.symmetricShortestPath(srcLoc.getSwitch(), dstLoc.getSwitch(), hash);
      return route(path, dstLoc);

    }
  }
  public void learn(Packet p) {
    hostLocationMap.put(p.ethSrc(), p.ingressPort());  
  }
}
