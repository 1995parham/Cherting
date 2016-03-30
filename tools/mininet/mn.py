#!/usr/bin/env python
"""
mn.py: Create simple Data Center like topology.

Usage (build 2**n host in edge):
    Directly running this script:
        sudo python mn.py

"""

from mininet.topo import Topo


class DataCenterTopology(Topo):
    """
    Data Center topology

    The topology has a single core switch. There are 2 ** (n - 2)
    aggregate switches connected into core switch and there are
    2 ** (n - 1) edge switches connected to hosts and aggregate switch
                                    c1
                            a1              a2
                        e1      e2      e3      e4
                    h1      h2      h3      h4      h5
    """

    def build(self, n=2):
        core = self.addSwitch("core")
        a = []
        e = []
        h = []
        for i in range(2 ** (n - 2)):
            a.append(self.addSwitch("a{}".format(i)))
            self.addLink(core, a[len(a) - 1])
        for i in range(2 ** (n - 1)):
            e.append(self.addSwitch("e{}".format(i)))
            self.addLink(a[i / 2], e[len(e) - 1])
        for i in range(2 ** n):
            h.append(self.addHost("h{}".format(i)))
            self.addLink(e[i / 2], h[len(h) - 1])


def example(n=2):
    """Simple example that exercises DataCenterTopology"""

    net = Mininet(topo=DataCenterTopology(n))
    net.start()
    CLI(net)
    net.stop()


if __name__ == '__main__':
    import sys

    from mininet.net import Mininet
    from mininet.cli import CLI
    from mininet.log import setLogLevel

    setLogLevel('info')

    if len(sys.argv) >= 2:
        example(sys.argv[1])
    else:
        example()
