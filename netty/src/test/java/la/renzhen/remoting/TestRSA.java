package la.renzhen.remoting;

import la.renzhen.remoting.netty.security.InternalSecurityProvider;
import la.renzhen.remoting.netty.utils.RSAUtils;
import org.junit.Test;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 22:27
 */
public class TestRSA {

    @Test
    public void testKeyGen() throws Exception {
        RSAUtils.generate("./src/main/resources/certs");
    }
}
