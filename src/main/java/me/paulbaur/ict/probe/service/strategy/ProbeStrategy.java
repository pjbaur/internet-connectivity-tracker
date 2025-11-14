package me.paulbaur.ict.probe.service.strategy;

import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;

public interface ProbeStrategy {
    ProbeResult probe(ProbeRequest request);
}
