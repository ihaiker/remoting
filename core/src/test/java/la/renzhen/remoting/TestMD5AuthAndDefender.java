package la.renzhen.remoting;

import la.renzhen.remoting.core.auth.MD5AuthDefender;
import la.renzhen.remoting.core.auth.MD5Auth;
import la.renzhen.remoting.protocol.ClientInfoHeader;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.util.Map;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-02-01 17:18
 */
public class TestMD5AuthAndDefender {

    class TestChannel implements RemotingChannel<String> {
        @Override
        public String getUnique() {
            return "test";
        }

        @Override
        public Map<String, String> getAttrs() {
            return null;
        }

        @Override
        public String getModule() {
            return "remoting";
        }

        @Override
        public String address() {
            return null;
        }

        @Override
        public String getChannel() {
            return null;
        }

        @Override
        public boolean isOK() {
            return false;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public void writeAndFlush(RemotingCommand command, ChannelWriterListener<String> writerListener) {

        }
    }

    RemotingAuth<String> auth = new MD5Auth<>();
    MD5AuthDefender<String> defender = new MD5AuthDefender<>(auth);

    @Test
    public void testAuthAndDefender() {
        RemotingChannel<String> channel = new TestChannel();
        String authUsername = "haiker";
        String authPassword = defender.getUserAuthPassword(authUsername);
        String authSign = auth.encode(channel, authUsername, authPassword);

        ClientInfoHeader header = new ClientInfoHeader();
        header.setUnique(channel.getUnique()).setModule(channel.getModule()).setAttrs(channel.getAttrs());
        header.setAuthUsername(authUsername);
        header.setAuthSign(authSign);
        assert defender.checked(channel,header);
    }
}
