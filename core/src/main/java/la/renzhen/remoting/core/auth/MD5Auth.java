package la.renzhen.remoting.core.auth;

import la.renzhen.remoting.RemotingAuth;
import la.renzhen.remoting.commons.MD5Util;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 19:58
 */
public class MD5Auth implements RemotingAuth {

    @Getter @Setter private String salt = "Remoting";

    @Override
    public String signature( String authUsername, String authPassword) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSalt()).append(authUsername);
        sb.append(getSalt()).append(authPassword);
        return MD5Util.getMD5String(sb.toString());
    }

}
