import maple.core.*;
import maple.extra.*;
import java.util.LinkedList;

public class MonitorRule {
  public Assertion trafficType;
  public LinkedList<SwitchPort> destinations;
  public MonitorRule(Assertion p, LinkedList<SwitchPort> dests) {
    trafficType = p;
    destinations = dests;
  }
}

