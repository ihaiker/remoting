package la.renzhen.remoting.protocol;

import java.nio.ByteBuffer;

/**
 * Data transfer protocol：
 * <code>
 * content  : length +  id  + code   +  (headers.length+version+flag) + headers      +  body
 * bytes    :  int   +  int + short  +   int                          + byte[]       +  byte[]
 * </code>
 *
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 */
public class RemotingCoder {
    public static final int BYTES =
            Integer.BYTES/*length*/
                    + Integer.BYTES /*id*/
                    + Short.BYTES/*code*/
                    + Integer.BYTES/*(headers.length+version+flag)*/;


    public static ByteBuffer encode(RemotingCommand cmd) {
        //length
        int headerLength = 0;
        if (cmd.headers != null) {
            headerLength = cmd.headers.length;
        }
        int bodyLength = 0;
        if (cmd.body != null) {
            bodyLength = cmd.body.length;
        }
        int length = BYTES + headerLength + bodyLength;

        ByteBuffer result = ByteBuffer.allocate(length);
        result.putInt(length);
        result.putInt(cmd.getId());
        result.putShort((short) cmd.getCode());
        int three = (headerLength << (8 + 2)) | (cmd.getVersion() << 2) | cmd.getFlag();
        result.putInt(three);
        if (headerLength > 0) {
            result.put(cmd.headers);
        }
        if (bodyLength > 0) {
            result.put(cmd.body);
        }
        result.flip();
        return result;
    }

    public static RemotingCommand decode(ByteBuffer buf) {
        int length = buf.getInt();
        int id = buf.getInt();
        int code = buf.getShort();

        int three = buf.getInt();
        int flag = three & 0b11;
        int version = (three >> 2) & 0b1111_1111;
        int headLength = three >> 10;
        int bodyLength = length - headLength - BYTES;
        byte[] header = null, body = null;
        if(headLength > 0){
            header = new byte[headLength];
            buf.get(header);
        }
        if(bodyLength > 0){
            body = new byte[bodyLength];
            buf.get(body);
        }
        return new RemotingCommand(code).setId(id).setVersion(version)
                .setFlag(flag).setHeaders(header).setBody(body);
    }
}
