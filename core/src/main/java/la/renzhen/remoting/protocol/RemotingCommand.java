package la.renzhen.remoting.protocol;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import la.renzhen.remoting.RemotingException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

@Accessors(chain = true)
public class RemotingCommand implements Serializable {

    private static final int RESPONSE = 0b10;
    private static final int ONEWAY = 0b01;

    public static AtomicInteger requestIdMaker = new AtomicInteger(0);

    //@formatter:off

    @Setter @Getter int id = 0;

    @Setter @Getter int code;

    @Setter @Getter private int version = 0;

    @Setter(AccessLevel.PROTECTED) @Getter private int flag;

    @Setter @Getter transient byte[] headers;
    @Setter @Getter transient byte[] body;

    //@formatter:on

    public RemotingCommand() {
    }

    public RemotingCommand(int code) {
        this.code = code;
    }

    public RemotingCommand makeResponse() {
        this.flag |= RESPONSE;
        return this;
    }

    public RemotingCommand makeOneway() {
        this.flag |= ONEWAY;
        return this;
    }

    public boolean isResponse() {
        return (flag & RESPONSE) > 0;
    }

    public boolean isOneway() {
        return (flag & ONEWAY) > 0;
    }

    public boolean isSuccess() {
        return code == RemotingSysResponseCode.SUCCESS;
    }

    public RemotingCommand setCustomHeader(CommandCustomHeader headers) {
        if (headers == null) {
            this.headers = null;
        } else {
            if (!headers.checkFields()) {
                throw new RemotingException(RemotingException.Type.Command,
                        "Missing required field:" + headers.getClass().getName());
            }
            this.headers = JSON.toJSONBytes(headers);
        }
        return this;
    }

    public <T extends CommandCustomHeader> T getCustomHeader(Class<T> headerClass) {
        if (this.headers == null || this.headers.length == 0) {
            return null;
        }
        return JSON.parseObject(this.headers, headerClass);
    }

    public RemotingCommand setError(int code, String message) {
        this.code = code;
        this.body = message.getBytes(Charsets.UTF_8);
        return this;
    }

    public String getError() {
        if (isSuccess()) {
            return null;
        }
        return new String(body, Charsets.UTF_8);
    }

    public static RemotingCommand request(int code) {
        return new RemotingCommand(code)
                .setId(requestIdMaker.incrementAndGet());
    }

    public static RemotingCommand request(int code, CommandCustomHeader header) {
        return new RemotingCommand(code)
                .setId(requestIdMaker.incrementAndGet()).setCustomHeader(header);
    }

    public static RemotingCommand oneway(int code) {
        return new RemotingCommand(code).makeOneway()
                .setId(requestIdMaker.incrementAndGet());
    }

    public static RemotingCommand error(int errorCode, String message) {
        return new RemotingCommand().makeResponse().setError(errorCode, message);
    }

    public static RemotingCommand error(RemotingCommand cmd, int errorCode, String message) {
        return new RemotingCommand().makeResponse().setId(cmd.getId()).setError(errorCode, message);
    }

    public static RemotingCommand response(RemotingCommand cmd) {
        return new RemotingCommand(RemotingSysResponseCode.SUCCESS).makeResponse().setId(cmd.getId());
    }

    public static RemotingCommand response() {
        return new RemotingCommand(RemotingSysResponseCode.SUCCESS).makeResponse();
    }
}