package me.paulbaur.ict.target.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import me.paulbaur.ict.common.model.ProbeMethod;

@Schema(description = "A network target that is probed for connectivity; represents a host/port combination monitored by the system")
public class Target {

    @Schema(description = "Unique identifier of the target (UUID)", example = "00000000-0000-0000-0000-000000000000")
    private UUID id;

    @Schema(description = "Human-readable label for the target", example = "example.org")
    private String label;

    @Schema(description = "Hostname or IP address of the target", example = "93.184.216.34")
    private String host;

    @Schema(description = "Port number for the target service", example = "80")
    private int port;

    @Schema(description = "Is the targeet enabled?", example = "true")
    private boolean enabled;

    @Schema(description = "Probe method to use for this target (TCP or ICMP)", example = "TCP")
    private ProbeMethod probeMethod;

    @Schema(description = "Probe timeout in milliseconds (optional, uses default if not set)", example = "1000")
    private Integer timeoutMs;

    public Target(UUID id, String label, String host, int port) {
        this.id = id;
        this.label = label;
        this.host = host;
        this.port = port;
        this.enabled = true;
        this.probeMethod = ProbeMethod.TCP; // Default to TCP
        this.timeoutMs = null; // Use default from configuration
    }

    public Target(UUID id, String label, String host, int port, boolean enabled) {
        this.id = id;
        this.label = label;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
        this.probeMethod = ProbeMethod.TCP; // Default to TCP
        this.timeoutMs = null; // Use default from configuration
    }

    public Target(UUID id, String label, String host, int port, boolean enabled, ProbeMethod probeMethod, Integer timeoutMs) {
        this.id = id;
        this.label = label;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
        this.probeMethod = probeMethod != null ? probeMethod : ProbeMethod.TCP;
        this.timeoutMs = timeoutMs;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ProbeMethod getProbeMethod() {
        return probeMethod != null ? probeMethod : ProbeMethod.TCP;
    }

    public void setProbeMethod(ProbeMethod probeMethod) {
        this.probeMethod = probeMethod;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Target target = (Target) o;
        return port == target.port && Objects.equals(id, target.id) && Objects.equals(host, target.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port);
    }

    @Override
    public String toString() {
        return "Target{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", enabled=" + enabled +
                ", probeMethod=" + probeMethod +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}
