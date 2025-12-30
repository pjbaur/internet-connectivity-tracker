package me.paulbaur.ict.probe.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.target.domain.Target;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate probe strategy based on target configuration.
 * Returns either TCP or ICMP strategy depending on the target's probe method.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProbeStrategyFactory {

    private final TcpProbeStrategy tcpProbeStrategy;
    private final IcmpProbeStrategy icmpProbeStrategy;

    /**
     * Get the appropriate probe strategy for the given target.
     *
     * @param target the target to probe
     * @return the probe strategy to use (TCP or ICMP)
     */
    public ProbeStrategy getStrategy(Target target) {
        ProbeMethod method = target.getProbeMethod();

        if (method == null) {
            log.debug("No probe method specified for target, defaulting to TCP",
                    "targetId", target.getId());
            return tcpProbeStrategy;
        }

        return switch (method) {
            case TCP -> tcpProbeStrategy;
            case ICMP -> icmpProbeStrategy;
        };
    }

    /**
     * Get the appropriate probe strategy for the given probe method.
     *
     * @param method the probe method
     * @return the probe strategy to use (TCP or ICMP)
     */
    public ProbeStrategy getStrategy(ProbeMethod method) {
        if (method == null) {
            return tcpProbeStrategy;
        }

        return switch (method) {
            case TCP -> tcpProbeStrategy;
            case ICMP -> icmpProbeStrategy;
        };
    }
}
