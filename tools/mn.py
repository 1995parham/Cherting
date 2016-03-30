#!/usr/bin/env python
# In The Name Of God
# ========================================
# [] File Name : mn.py
#
# [] Creation Date : 15/03/16
#
# [] Created By : Parham Alvani (parham.alvani@gmail.com)
# =======================================
"""
mn.py:
    Create simple Data Center like topology
    and connect it into contorller on remote host.
Usage (example uses IP = 192.168.1.2, n = 2):
    From the command line:
        sudo python mn.py --ip 192.168.1.2 --n 2
"""

from mininet.net import Mininet
from mininet.net import CLI
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.node import OVSSwitch
from mininet.topo import Topo

import argparse
from functools import partial

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
    def build(self, n=2, **params):
        core = self.addSwitch("name=s0")
        aggregate_switchs = []
        edge_switchs = []
        hosts = []
        for i in range(2 ** (n - 2)):
            aggregate_switchs.append(self.addSwitch(name="s%d" % (i + 1)))
            self.addLink(core, aggregate_switchs[len(aggregate_switchs) - 1])

        for i in range(2 ** (n - 1)):
            edge_switchs.append(self.addSwitch(name="s%d" % (i + 1 + 2 ** (n - 2))))
            self.addLink(aggregate_switchs[i / 2], edge_switchs[len(edge_switchs) - 1])

        for i in range(2 ** n):
            hosts.append(self.addHost(name="h%d" % i))
            self.addLink(edge_switchs[i / 2], hosts[len(hosts) - 1])


if __name__ == '__main__':
    PARSER = argparse.ArgumentParser()
    PARSER.add_argument('--ip', dest='ip', help='ONOS Network Controller IP Address', default='127.0.0.1', type=str)
    PARSER.add_argument('--n', dest='n', help='N of the topology :)', default=2, type=int)
    CLI_ARGS = PARSER.parse_args()

    setLogLevel('info')

    SWITCH = partial(OVSSwitch, protocols='OpenFlow13')
    NET = Mininet(topo=DataCenterTopology(n=CLI_ARGS.n), controller=RemoteController('ONOS', ip=CLI_ARGS.ip, port=6633), switch=SWITCH)
    NET.start()
    CLI(NET)
    NET.stop()
