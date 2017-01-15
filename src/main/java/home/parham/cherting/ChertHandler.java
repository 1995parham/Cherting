/*
 * In The Name Of God
 * ========================================
 * [] File Name : L2SwitchingHandler.java
 *
 * [] Creation Date : 06-01-2016
 *
 * [] Created By : Parham Alvani (parham.alvani@gmail.com)
 * =======================================
*/
package home.parham.cherting;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChertHandler implements PacketProcessor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private PacketService packetService;
    private HostService hostService;

    public ChertHandler(HostService hostService, PacketService packetService) {
        this.hostService = hostService;
        this.packetService = packetService;
    }

    @Override
    public void process(PacketContext context) {
        /*
         * Stop processing if the packet has been handled, since we
		 * can't do any more to it.
		*/
        if (context.isHandled()) {
            return;
        }

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        if (ethPkt == null) {
            return;
        }

        if (ethPkt.getDestinationMAC().equals(MacAddress.BROADCAST)) {
            flood(context);
            return;
        }

        HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

		/*
		 * Do we know who this is for? If not, flood and bail.
		 */
        Host dst = hostService.getHost(dstId);
        if (dst == null) {
            flood(context);
            return;
        }

        if (dst.vlan().toShort() == ethPkt.getVlanID()) {
            forwardPacketToDst(context, dst);
        }
    }

    /**
     * Forward packet into destination switch directly, we put it there ! how fun :)
     */
    private void forwardPacketToDst(PacketContext context, Host dst) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
                treatment, context.inPacket().unparsed());
        packetService.emit(packet);
        log.info("sending packet: {}", packet);
    }


    /**
     * Floods the specified packet if permissible.
     */
    private void flood(PacketContext context) {
        packetOut(context, PortNumber.FLOOD);
    }

    /**
     * Sends a packet out the specified port.
     */
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

}
