/*
 * In The Name Of God
 * ========================================
 * [] File Name : ChertHandler.java
 *
 * [] Creation Date : 06-01-2016
 *
 * [] Created By : Parham Alvani (parham.alvani@gmail.com)
 * =======================================
*/
package home.parham.cherting;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;

public class ChertHandler implements PacketProcessor, FlowRuleListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private FlowRuleService flowRuleService;
    private TopologyService topologyService;
    private HostService hostService;
    private ApplicationId id;

    private HashMap<FlowId, Long> flowTimeStamps;
    private double averageProcessingTime;
    private double averageWaitingTime;
    private int n;

    public ChertHandler(ApplicationId id, FlowRuleService flowRuleService,
                        TopologyService topologyService, HostService hostService) {
        this.id = id;
        this.flowRuleService = flowRuleService;
        this.topologyService = topologyService;
        this.hostService = hostService;
        this.flowTimeStamps = new HashMap<>();
        this.averageProcessingTime = 0;
        this.averageWaitingTime = 0;
        this.n = 0;
    }

    @Override
    public void process(PacketContext context) {
        /* Record packet in time (Waiting time) */
        long tw = context.time();
        /* Record process starting time (processing time) */
        long tp = System.nanoTime();

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

        /*
         * Topology processing
         */
        Set<Host> dstHosts = this.hostService.getHostsByMac(ethPkt.getDestinationMAC());
        for (Host dstHost : dstHosts) {
            this.topologyService.getPaths(this.topologyService.currentTopology(),
                    pkt.receivedFrom().deviceId(), dstHost.location().deviceId());
        }

        /*
         * Let's build the rule for it's source.
         */
        FlowRule.Builder fb = DefaultFlowRule.builder();
        /* General flow information */
        fb.forDevice(pkt.receivedFrom().deviceId());
        fb.makePermanent();
        fb.withPriority(10);
        fb.fromApp(this.id);
        /* Flow selection */
        TrafficSelector.Builder sb = DefaultTrafficSelector.builder();
        sb.matchEthDst(ethPkt.getSourceMAC());
        fb.withSelector(sb.build());
        /* Flow treatment */
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        tb.setOutput(pkt.receivedFrom().port());
        fb.withTreatment(tb.build());
        /* Flow applying */
        FlowRule f = fb.build();
        this.flowTimeStamps.put(f.id(), tw);
        this.flowRuleService.applyFlowRules(f);

        /* Time measurements */
        long diff = System.nanoTime() - tp;
        /* Ignores out of range response times */
        if (diff > 1000000) {
            diff = (long) this.averageProcessingTime;
        }
        this.averageProcessingTime = this.n * this.averageProcessingTime + diff;
        this.averageWaitingTime = this.n * this.averageWaitingTime + System.currentTimeMillis() - tw;
        this.averageProcessingTime /= (this.n + 1);
        this.averageWaitingTime /= (this.n + 1);
        this.n++;
        log.info("% RSTime: " + this.averageProcessingTime + " % n: " + (this.n - 1));
        log.info("$ W Time: " + this.averageWaitingTime + " $ n: " + (this.n - 1));
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

    @Override
    public void event(FlowRuleEvent flowRuleEvent) {
        if (flowRuleEvent.type() != FlowRuleEvent.Type.RULE_ADDED)
            return;

        /* Record flow addition time */
        long t2 = flowRuleEvent.time();

        long t1 = t2;

        if (this.flowTimeStamps.containsKey(flowRuleEvent.subject().id()))
            t1 = this.flowTimeStamps.get(flowRuleEvent.subject().id());

        log.info("$ " + (t2 - t1) + " $");
    }
}
