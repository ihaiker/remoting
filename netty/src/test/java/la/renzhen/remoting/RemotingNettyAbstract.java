package la.renzhen.remoting;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import la.renzhen.remoting.netty.*;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 15:46
 */

public abstract class RemotingNettyAbstract implements LoggerSupport {
    protected RemotingServer server;
    protected RemotingClient client;

    ExecutorService executorService = Executors.newFixedThreadPool(2);

    public RemotingServer createRemotingServer() throws InterruptedException {
        NettyServerConfig config = new NettyServerConfig();
        NettyRemotingServer remotingServer = new NettyRemotingServer(config);
        remotingServer.setModule("Server");
        registerTestProcessor(remotingServer);
        return remotingServer;
    }

    public RemotingClient createRemotingClient() {
        NettyClientConfig config = new NettyClientConfig();
        List<String> serverAddress = Lists.newArrayList("127.0.0.1:8888","localhost:8888");
        NettyRemotingClient client = new NettyRemotingClient(serverAddress, config);
        client.setModule("Client");
        registerTestProcessor(client);
        return client;
    }

    public void registerTestProcessor(NettyRemoting remoting) {
        remoting.registerProcessor(0, (channel, request) -> {
            String header = request.getStringHeaders();
            log.info("receiver: {}", header);
            return RemotingCommand.response(request).setStringHeaders("receiver " + header);
        }, executorService);

        remoting.registerProcessor(1, (channel, request) -> {
            String body = new String(request.getBody(), Charsets.UTF_8);
            log.info("receiver body: {}", body);
            return RemotingCommand.response(request).setBody(("receiver body " + body).getBytes(Charsets.UTF_8));
        }, executorService);

        remoting.registerDefaultProcessor((channel, request) -> RemotingCommand.response(request).setHeaders(request.getHeaders()).setBody(request.getBody()), executorService);
    }

    @Before
    public void startup() throws InterruptedException {
        server = createRemotingServer();
        server.startup();
        client = createRemotingClient();
        client.startup();
    }

    @After
    public void destroy() {
        client.shutdown();
        server.shutdown();
    }
}