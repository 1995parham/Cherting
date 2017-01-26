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

import java.util.HashMap;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
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
import org.onosproject.net.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChertHandler implements PacketProcessor, FlowRuleListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private FlowRuleService flowRuleService;
    private ApplicationId id;

    private HashMap<FlowId, Long> flowTimeStamps;

    public ChertHandler(ApplicationId id, FlowRuleService flowRuleService) {
        this.id = id;
        this.flowRuleService = flowRuleService;
        this.flowTimeStamps = new HashMap<>();
    }

    @Override
    public void process(PacketContext context) {
        /* Record packet in time */
        long t = context.time();

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
         * Let's build the rule for it's source.
         */
        FlowRule.Builder fb = DefaultFlowRule.builder();
        /* General flow information */
        fb.forDevice(pkt.receivedFrom().deviceId());
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
        this.flowTimeStamps.put(f.id(), t);
        this.flowRuleService.applyFlowRules(f);
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

        long t1 = this.flowTimeStamps.get(flowRuleEvent.subject().id());

        log.info("$ %ld $\n", t2 - t1);
    }
}
