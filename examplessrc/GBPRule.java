public class GBPRule {
  public enum Direction {In, Out, Bi}
  public Direction direction;
  public int startPort;
  public int endPort;
  public GBPAction action;
}
