package la.renzhen.remoting;

import la.renzhen.remoting.netty.NettyRemoting;
import la.renzhen.remoting.netty.NettyRemotingServer;
import la.renzhen.remoting.netty.security.StormFrom;
import la.renzhen.remoting.netty.security.jks.InternalSecurityProvider;
import la.renzhen.remoting.netty.security.jks.JKSConfig;
import la.renzhen.remoting.netty.security.jks.JKSKeyStoresSecurityProvider;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:07
 */
public class SecurityBase64Test extends RemotingNettyTest {
    String password = "remoting";

    @Override
    public void registerTestProcessor(NettyRemoting remoting) {
        super.registerTestProcessor(remoting);
        boolean server = remoting instanceof NettyRemotingServer;

        JKSConfig config;
        if (server) {
            String base = "/u3+7QAAAAIAAAABAAAAAQAIcmVtb3RpbmcAAAFoosviAgAABQEwggT9MA4GCisGAQQBKgIRAQEFAASCBOkVu+3eiD+Dg12Ls/H23UE266MwHE6zkStfBN65Tc9XRP8FmQQY3+47fUq6MQ" +
                    "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";
            config = JKSConfig.onewayAuthServer(base, password);
        } else {
            String base = "/u3+7QAAAAIAAAABAAAAAgAIcmVtb3RpbmcAAAFoosvlhAAFWC41MDkAAANHMIIDQzCCAiugAwIBAgIEP2i+HDANBgkqhkiG9w0BAQsFADBSMQowCAYDVQQDDAEqMQswCQYDVQQGEwJDTjEQMA4GA1UEBxMHQmVpamluZzERMA8GA1UEChMIUmVtb3RpbmcxEjAQBgNVBAsTCURldmVsb3BlcjAeFw0xOTAxMzEwNzI0NTdaFw0yOTAxMjgwNzI0NTdaMFIxCjAIBgNVBAMMASoxCzAJBgNVBAYTAkNOMRAwDgYDVQQHEwdCZWlqaW5nMREwDwYDVQQKEwhSZW1vdGluZzESMBAGA1UECxMJRGV2ZWxvcGVyMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkKDZPOSRqiOs5n4amR3JBL1pnP/kH/GkQ7dSKE9COMl1lXp866B0foIBOQQ6ySUmPxIP3PJAjMEF/PCVMM2/DQe+4oidtk9IDbzJ/fOmeRy5bgTrHV5IJzM+9OXIc88RnImYzLKXzmebZWE50/jYNNqv9oZnQjaSofp6+vJhIvsPZ6+F4ZRh42PRnXNl6twLd4AAWiQkK9ZsPJCwNpsDUNl76pyQSZmXa2hGFw7WnXB/URJxQespI35mD0TraHroTMLfWpHCyJMB9XZr10iUfugTXsSQBOS3vX+9Y1Cpm652lBZcriJNRVjQNeVCB2Lh3SQjbbjg0w/MxCQ6IJDuowIDAQABoyEwHzAdBgNVHQ4EFgQUDy2avmyCwkIem5zhKqO/MfSR6NAwDQYJKoZIhvcNAQELBQADggEBAE7g1utDxkKt8JG7+ACxhm8ejOOfaHOB/o/6zE8QiZtuXtpyFkRijVeH0SyM7fmLOf+w370+eimQVSxOBgjchEFxkKOzhW1y/hfr3Bi8f7AgfaPfgbkdQrdV1fzNP6nqJK9oN3MR8itDP7T8aDPkvWJe2zoSBZ/Fur5lI5k+XGTUaQQEAEaeu9g0OGv22hyXnpXymPDBJFLUPPGLjPiJfbx2s2g0fHh/pZezbwGrapkTiO7CkocVsko7SCikaGd7cuHEwAkxiqdHhSOcqrgtLC7LnbKxKm0Vxi6syzxr8FfGL6R8fy50ARMF29aKDL55EtRpbU9aThH/hZHJSpZqUgvZcnijrn4lEnjJoVjdPMfJM1l8tg==";
            config = JKSConfig.onewayAuthClient(base, password);
        }
        config.stormFrom(StormFrom.BASE64);
        remoting.setSecurityProvider(new JKSKeyStoresSecurityProvider(config));
    }

    @Test
    public void testSecurity() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("security");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver security".equals(response.getStringHeaders());
    }
}
