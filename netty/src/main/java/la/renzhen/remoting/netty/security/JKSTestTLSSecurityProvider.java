package la.renzhen.remoting.netty.security;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import la.renzhen.remoting.netty.security.SecurityProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-30 20:05
 */
public class JKSTestTLSSecurityProvider implements SecurityProvider {

    protected boolean server;

    public JKSTestTLSSecurityProvider(boolean server) {
        this.server = server;
    }

    public ChannelHandler initChannel(SocketChannel ch) {
        String path = "/Users/haiker/Documents/project/myself/JavaWork/remoting/netty/src/main/resources/certs/test";

        if (server) {
            String sChatPath = path + "/serverStore.jks";
            SSLEngine engine = getServerContext(sChatPath, sChatPath).createSSLEngine();
            engine.setUseClientMode(false);//设置服务端模式
            engine.setNeedClientAuth(true);//需要客户端验证

            return new SslHandler(engine);
        } else {
            String cChatPath = path + "/clientStore.jks";
            SSLEngine engine = getClientContext(cChatPath, cChatPath).createSSLEngine();
            engine.setUseClientMode(true);
            return new SslHandler(engine);
        }
    }

    private static final String PROTOCOL = "TLS";

    private static SSLContext SERVER_CONTEXT;//服务器安全套接字协议

    private static SSLContext CLIENT_CONTEXT;//客户端安全套接字协议


    public static SslContext getServerContext2(String pkPath, String caPath) {
        InputStream in = null;
        InputStream tIN = null;

        try {
            //密钥管理器
            KeyManagerFactory kmf = null;
            if (pkPath != null) {
                KeyStore ks = KeyStore.getInstance("JKS");
                in = new FileInputStream(pkPath);
                ks.load(in, "nettyDemo".toCharArray());

                kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, "nettyDemo".toCharArray());
            }
            //信任库
            TrustManagerFactory tf = null;
            if (caPath != null) {
                KeyStore tks = KeyStore.getInstance("JKS");
                tIN = new FileInputStream(caPath);
                tks.load(tIN, "nettyDemo".toCharArray());
                tf = TrustManagerFactory.getInstance("SunX509");
                tf.init(tks);
            }

            //SERVER_CONTEXT = SSLContext.getInstance(PROTOCOL);
            //初始化此上下文
            //参数一：认证的密钥      参数二：对等信任认证  参数三：伪随机数生成器 。 由于单向认证，服务端不用验证客户端，所以第二个参数为null
            //SERVER_CONTEXT.init(kmf.getKeyManagers(), tf.getTrustManagers(), null);

            return SslContextBuilder.forServer(kmf).trustManager(tf).protocols(PROTOCOL).build();

        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }

            if (tIN != null) {
                try {
                    tIN.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tIN = null;
            }
        }
    }


    public static SSLContext getServerContext(String pkPath, String caPath) {
        if (SERVER_CONTEXT != null) return SERVER_CONTEXT;
        InputStream in = null;
        InputStream tIN = null;

        try {
            //密钥管理器
            KeyManagerFactory kmf = null;
            if (pkPath != null) {
                KeyStore ks = KeyStore.getInstance("JKS");
                in = new FileInputStream(pkPath);
                ks.load(in, "nettyDemo".toCharArray());

                kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, "nettyDemo".toCharArray());
            }
            //信任库
            TrustManagerFactory tf = null;
            if (caPath != null) {
                KeyStore tks = KeyStore.getInstance("JKS");
                tIN = new FileInputStream(caPath);
                tks.load(tIN, "nettyDemo".toCharArray());
                tf = TrustManagerFactory.getInstance("SunX509");
                tf.init(tks);
            }

            SERVER_CONTEXT = SSLContext.getInstance(PROTOCOL);
            //初始化此上下文
            //参数一：认证的密钥      参数二：对等信任认证  参数三：伪随机数生成器 。 由于单向认证，服务端不用验证客户端，所以第二个参数为null
            SERVER_CONTEXT.init(kmf.getKeyManagers(), tf.getTrustManagers(), null);

        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }

            if (tIN != null) {
                try {
                    tIN.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tIN = null;
            }
        }

        return SERVER_CONTEXT;
    }


    public static SslContext getClientContext2(String pkPath, String caPath) {
        InputStream in = null;
        InputStream tIN = null;
        try {
            KeyManagerFactory kmf = null;
            if (pkPath != null) {
                KeyStore ks = KeyStore.getInstance("JKS");
                in = new FileInputStream(pkPath);
                ks.load(in, "nettyDemo".toCharArray());
                kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, "nettyDemo".toCharArray());
            }

            TrustManagerFactory tf = null;
            if (caPath != null) {
                KeyStore tks = KeyStore.getInstance("JKS");
                tIN = new FileInputStream(caPath);
                tks.load(tIN, "nettyDemo".toCharArray());
                tf = TrustManagerFactory.getInstance("SunX509");
                tf.init(tks);
            }

            /// CLIENT_CONTEXT = SSLContext.getInstance(PROTOCOL);
            //初始化此上下文
            //参数一：认证的密钥      参数二：对等信任认证  参数三：伪随机数生成器 。 由于单向认证，服务端不用验证客户端，所以第二个参数为null
            //CLIENT_CONTEXT.init(kmf.getKeyManagers(), tf.getTrustManagers(), null);

            return SslContextBuilder.forClient().protocols(PROTOCOL).keyManager(kmf).trustManager(tf).build();
        } catch (Exception e) {
            throw new Error("Failed to initialize the client-side SSLContext");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }

            if (tIN != null) {
                try {
                    tIN.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tIN = null;
            }
        }
    }


    public static SSLContext getClientContext(String pkPath, String caPath) {
        if (CLIENT_CONTEXT != null) return CLIENT_CONTEXT;

        InputStream in = null;
        InputStream tIN = null;
        try {
            KeyManagerFactory kmf = null;
            if (pkPath != null) {
                KeyStore ks = KeyStore.getInstance("JKS");
                in = new FileInputStream(pkPath);
                ks.load(in, "nettyDemo".toCharArray());
                kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, "nettyDemo".toCharArray());
            }

            TrustManagerFactory tf = null;
            if (caPath != null) {
                KeyStore tks = KeyStore.getInstance("JKS");
                tIN = new FileInputStream(caPath);
                tks.load(tIN, "nettyDemo".toCharArray());
                tf = TrustManagerFactory.getInstance("SunX509");
                tf.init(tks);
            }

            CLIENT_CONTEXT = SSLContext.getInstance(PROTOCOL);
            //初始化此上下文
            //参数一：认证的密钥      参数二：对等信任认证  参数三：伪随机数生成器 。 由于单向认证，服务端不用验证客户端，所以第二个参数为null
            CLIENT_CONTEXT.init(kmf.getKeyManagers(), tf.getTrustManagers(), null);

        } catch (Exception e) {
            throw new Error("Failed to initialize the client-side SSLContext");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }

            if (tIN != null) {
                try {
                    tIN.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tIN = null;
            }
        }

        return CLIENT_CONTEXT;
    }
}
