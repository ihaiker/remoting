package la.renzhen.remoting.core.auth;

import la.renzhen.remoting.LoggerSupport;
import la.renzhen.remoting.RemotingAuth;
import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.RemotingDefender;
import la.renzhen.remoting.commons.MD5Util;
import la.renzhen.remoting.protocol.ClientInfoHeader;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 21:48
 */
public class MD5AuthDefender<Channel> implements RemotingDefender<Channel>, LoggerSupport {

    final RemotingAuth auth;
    @Setter
    @Getter
    private String salt = "RemotingServer";

    public MD5AuthDefender(@NotNull RemotingAuth auth) {
        this.auth = auth;
    }

    @Override
    public boolean checked(RemotingChannel<Channel> channel, ClientInfoHeader header) {
        String unique = header.getUnique();
        String module = header.getModule();
        String authSign = header.getAuthSign();
        String authUsername = header.getAuthUsername();
        log.info("{},{} request auth userName={} sign={} ", module, unique, authUsername, authSign);

        String authPassword = getUserAuthPassword(authUsername);
        String checkSign = auth.encode(channel, authUsername, authPassword);
        return checkSign.equals(authSign);
    }

    public String getUserAuthPassword(String authUsername) {
        StringBuilder sb = new StringBuilder();
        sb.append(salt).append(authUsername);
        return MD5Util.getMD5String(sb.toString());
    }
}
