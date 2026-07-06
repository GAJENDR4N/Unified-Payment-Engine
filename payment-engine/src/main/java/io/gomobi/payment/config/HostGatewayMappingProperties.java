package io.gomobi.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a "host" provider code (PROVIDER_CONFIGURATION.PROVIDER_CODE, e.g.
 * MAYBANK, TNG, GOPAY, PAYOK - the bank/e-wallet/switch a transaction is
 * routed to) onto the technical gateway adapter that actually integrates
 * with it (FIUU, PAYOK, CURLEC).
 * Several hosts can share one gateway (e.g. FIUU/Razer processes both
 * MAYBANK and TNG DuitNow QR traffic today). Backed by
 * payment.host-gateway-mapping in application.yml so new hosts can be
 * onboarded without a code change.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "payment")
public class HostGatewayMappingProperties {

    private Map<String, String> hostGatewayMapping = new HashMap<>();

}
