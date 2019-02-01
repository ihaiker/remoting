package la.renzhen.remoting.core.auth;

import la.renzhen.remoting.RemotingAuth;
import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.commons.MD5Util;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 19:58
 */
public class MD5Auth<Channel> implements RemotingAuth<Channel> {

    @Getter @Setter private String salt = "Remoting";

    @Override
    public String encode(RemotingChannel<Channel> channel, String authUsername, String authPassword) {
        StringBuilder sb = new StringBuilder();
        sb.append(getSalt()).append(channel.getModule());
        sb.append(getSalt()).append(channel.getUnique());
        sb.append(getSalt()).append(authUsername);
        sb.append(getSalt()).append(authPassword);
        return MD5Util.getMD5String(sb.toString());
    }

}
