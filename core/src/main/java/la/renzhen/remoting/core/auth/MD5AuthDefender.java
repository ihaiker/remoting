package la.renzhen.remoting.core.auth;

import la.renzhen.remoting.LoggerSupport;
import la.renzhen.remoting.RemotingAuth;
import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.RemotingDefender;
import la.renzhen.remoting.commons.MD5Util;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Optional;

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
    public boolean checked(RemotingChannel<Channel> channel) {
        String unique = channel.getUnique();
        String module = channel.getModule();
        String username = Optional.ofNullable(channel.getAttrs()).map(s -> s.get(RemotingAuth.AUTH_USERNAME)).orElse("");
        String signature = Optional.ofNullable(channel.getAttrs()).map(s -> s.get(RemotingAuth.AUTH_SIGNATURE)).orElse("");
        log.info("{},{} request auth userName={} sign={} ", module, unique, username, signature);
        String authPassword = getUserAuthPassword(username);
        String checkSign = auth.signature(username, authPassword);
        return checkSign.equals(signature);
    }

    public String getUserAuthPassword(String authUsername) {
        StringBuilder sb = new StringBuilder();
        sb.append(salt).append(authUsername);
        return MD5Util.getMD5String(sb.toString());
    }
}
