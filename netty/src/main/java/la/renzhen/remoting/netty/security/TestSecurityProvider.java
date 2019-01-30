package la.renzhen.remoting.netty.security;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.SneakyThrows;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 16:15
 */
public class TestSecurityProvider implements SecurityProvider {

    private boolean client;

    public TestSecurityProvider(boolean isClient) {
        this.client = isClient;
    }

    @SneakyThrows
    private ChannelHandler clientHandler(SocketChannel ch) {
        SslContext sslContext = SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        return sslContext.newHandler(ch.alloc());
    }

    @SneakyThrows
    private ChannelHandler serverHandler(SocketChannel ch) {
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        SslContext sslContext = SslContextBuilder
                .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.OPTIONAL)
                .build();
        return sslContext.newHandler(ch.alloc());
    }

    @Override
    public ChannelHandler initChannel(SocketChannel ch) {
        if (client) {
            return clientHandler(ch);
        } else {
            return serverHandler(ch);
        }
    }
}