package la.renzhen.remoting;

import la.renzhen.remoting.netty.NettyRemoting;
import la.renzhen.remoting.netty.NettyRemotingServer;
import la.renzhen.remoting.netty.security.InternalSecurityProvider;
import la.renzhen.remoting.netty.security.TestSecurityProvider;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:07
 */
public class SecurityTestTest extends RemotingNettyTest {

    @Override
    public void registerTestProcessor(NettyRemoting remoting) {
        super.registerTestProcessor(remoting);
        
        boolean server = remoting instanceof NettyRemotingServer;
        //remoting.setSecurityProvider(new InternalSecurityProvider(server));
        //remoting.setSecurityProvider(new TestSecurityProvider(server));

//        String path = "/Users/haiker/Documents/project/myself/JavaWork/remoting";
//        if (server) {
//            remoting.setSecurityProvider(ch -> {
//                String sChatPath = path + "/netty/src/main/resources/conf/twoway/sChat.jks";
//                SSLEngine engine = SecureChatSslContextFactory.getClientContext(sChatPath, sChatPath).createSSLEngine();
//                engine.setUseClientMode(false);//设置服务端模式
//                engine.setNeedClientAuth(true);//需要客户端验证
//                return new SslHandler(engine);
//            });
//        } else {
//            remoting.setSecurityProvider(ch -> {
//                String cChatPath = path + "/netty/src/main/resources/conf/twoway/cChat.jks";
//                SSLEngine engine = other.twoway.SecureChatSslContextFactory.getClientContext(cChatPath, cChatPath).createSSLEngine();
//                engine.setUseClientMode(true);
//                return new SslHandler(engine);
//            });
//        }

//        oneway
//        if (server) {
//            remoting.setSecurityProvider(ch -> {
//                String sChatPath = path + "/netty/src/main/resources/conf/oneway/sChat.jks";
//                SSLEngine engine = SecureChatSslContextFactory.getServerContext(sChatPath).createSSLEngine();
//                engine.setUseClientMode(false);//设置为服务器模式
//                //engine.setNeedClientAuth(false);//不需要客户端认证，默认为false，故不需要写这行。
//                return new SslHandler(engine);
//            });
//        } else {
//            remoting.setSecurityProvider(ch -> {
//                String cChatPath = path + "/netty/src/main/resources/conf/oneway/cChat.jks";
//                SSLEngine engine = SecureChatSslContextFactory.getClientContext(cChatPath).createSSLEngine();//创建SSLEngine
//                engine.setUseClientMode(true);//客户方模式
//                return new SslHandler(engine);
//            });
//        }
    }

    @Test
    public void testSecurity() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("security");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver security".equals(response.getStringHeaders());
    }
}
