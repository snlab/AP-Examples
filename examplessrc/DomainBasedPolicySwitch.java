import java.util.*;
import maple.core.*;
import java.io.IOException;
import java.net.UnknownHostException;
import org.xbill.DNS.*;

public class DomainBasedPolicySwitch extends MapleFunction {
	
  MapleMap<Long, SwitchPort> hostLocationMap =
      newMap("hostlocations", Long.class, SwitchPort.class, false);

  MapleMap<String,Integer> policy =
      newMap("policy", String.class, Integer.class);

  // TODO: Needs to be enhanced to cope with >1 address per name.
  MapleMap<String, Integer> domainMap = 
      newMap("domainMap", String.class, Integer.class);
  MapleMap<Integer, String> rdomainMap = 
      newMap("rdomainMap", Integer.class, String.class);
	
  // Constants
  public final int UP_PORT = 1;
  public final int ToS_GOOD = 4;
  final String DNS_IP_STRING = "192.168.0.8";
  final int CONTROLLER_DATA_IP = IPv4.toIPv4Address("192.168.0.101");

  @Override
  protected Route onPacket(Packet p) {
    
    learn(p);

    if (p.satisfies(Assertion.ipSrcIn(IPv4.toIPv4Address(DNS_IP_STRING),32), 
                    Assertion.udpSrcEquals(53))) 
    {
      learnDomainMapping(p);
    }

    List<Mod> mods = new LinkedList<Mod>();

    if (p.ingressPort().portID != UP_PORT) {
      if (1 == desiredQuality(p)) {
        mods.add(Mod.setIPTypeOfService(ToS_GOOD));
      }		
    }
    
    return forward(p, mods);
  }
  
  private Route forward(Packet p, List<Mod> mods) {
    SwitchPort dstLoc = hostLocationMap.get(p.ethDst());
    if (null == dstLoc) {
      return Route.route(null, edgePorts(), mods);
    }
    return Route.route(null, dstLoc, mods);
  }

  
  private int desiredQuality(Packet p) {
    if (p.satisfies(Assertion.isIPv4())) {
      String name = rdomainMap.get(p.ipDst());
      if (null == name) return 1;
      Integer qual = policy.get(name);
      if (null == qual) return 1;
      return qual;
    }
    return 1;
  }

  public void learn(Packet p) {
    hostLocationMap.put(p.ethSrc(), p.ingressPort());  
  }

  public void learnDomainMapping(Packet p) {
    Ethernet frame = p.getFrame();
    IPv4 packet = (IPv4) frame.getPayload();
    UDP segment = (UDP) packet.getPayload();
    Message dnsMessage = ((DNS) segment.getPayload()).message;
    handleDNSResponse(dnsMessage);
  }

  public void handleDNSResponse(Message dnsMessage) {
    Header dnsHeader = dnsMessage.getHeader();
    if (dnsHeader.getOpcode() == DNS.DNS_OP_CODE_QUERY && 
        dnsHeader.getRcode() == 0 &&
        dnsHeader.getCount(1) > 0)
    {
      for (Record record : dnsMessage.getSectionArray(1)) {
        if (record instanceof ARecord) {
          ARecord arecord = (ARecord) record;
          String name = arecord.getName().toString();
          int ip = IPv4.toIPv4Address(arecord.getAddress().getAddress());
          Integer ip_old = domainMap.get(name);
          domainMap.put(name, ip);
          if (null != ip_old && ip_old != ip) {
            rdomainMap.delete(ip_old);
          }
          rdomainMap.put(ip, name);
        }
      }
    }
  }
}
