package com.orion.api_gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gateway.whitelist")
public class IpWhitelistProperties {

    /**
     * Whether IP & Origin whitelisting is enabled.
     */
    private boolean enabled = true;

    /**
     * List of allowed IP addresses, host:port combinations, or Origins.
     */
    private List<String> ips = new ArrayList<>(List.of(
            "127.0.0.1:3000",
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1",
            "localhost:3000",
            "localhost"
    ));

    public IpWhitelistProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }
}
