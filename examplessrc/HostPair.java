public class HostPair implements Comparable<HostPair> {
  public long host1;
  public long host2;

  public HostPair() {}
  public HostPair (long host1, long host2) {
    this.host1 = host1;
    this.host2 = host2;
  }

  @Override
  public String toString() {
    return "HostPair [host1=" + host1 + ", host2=" + host2 + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 5557;
    int result = 1;
    result = prime * result + (int) host1;
    result = prime * result + (int) host2;
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (null == other) { return false; }
    HostPair other2 = (HostPair) other;
    return host1==other2.host1 && host2==other2.host2;
  }


  @Override
  public int compareTo(HostPair other) {
    if (host1 < other.host1) return -1;
    if (host1 == other.host1) {
      if (host2 < other.host2) return -1;
      if (host1 == other.host1) return 0;
      return 1;
    }
    return 1;
  }

}
