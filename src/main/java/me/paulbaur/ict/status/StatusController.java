package me.paulbaur.ict.status;

import lombok.RequiredArgsConstructor;
import me.paulbaur.ict.probe.domain.ProbeResult;
import me.paulbaur.ict.probe.service.ProbeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatusController {

    private final ProbeService probeService;

    @GetMapping("/api/status")
    public ProbeResult getStatus() {
        return probeService.getLatestStatus();
    }
}
