package la.renzhen.remoting;

import com.google.common.collect.Lists;
import la.renzhen.remoting.protocol.CommandCustomHeader;
import la.renzhen.remoting.protocol.RemotingCoder;
import la.renzhen.remoting.protocol.RemotingCommand;
import la.renzhen.remoting.protocol.RemotingSysResponseCode;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-26 17:21
 */
public class CommandTest {

    @Test
    public void testRequestAndResponse() {
        RemotingCommand request = RemotingCommand.request(1);
        assert !request.isResponse();
        assert !request.isOneway();
        assert request.getCode() == 1;
        assert request.getId() == 1;

        RemotingCommand response = RemotingCommand.response(request);
        assert response.isSuccess();
        assert response.isResponse();
        assert request.getId() == response.getId();
        assert response.getCode() == RemotingSysResponseCode.SUCCESS;

        response.setError(RemotingSysResponseCode.BUSY,"[BUSS]...");
        assert response.isResponse();
        assert !response.isSuccess();
        assert response.getError().equals("[BUSS]...");

        RemotingCommand oneway = RemotingCommand.oneway(2);
        assert !oneway.isResponse();
        assert oneway.isOneway();
        assert oneway.getCode() == 2;
        assert oneway.getId() == 2;
    }

    @Data
    public static class User implements CommandCustomHeader {
        String name;
        String address;
        String email;
        int age;

        List<String> Like;
        Map<String, String> tags;
    }

    User user = null;
    @Before
    public void initUser() {
        user = new User();
        user.setName("haiker");
        user.setAddress("Beijing,北京");
        user.setEmail("wo@renzhen.la");
        user.setAge(34);
        user.setLike(Lists.newArrayList("钱", "权", "人")); //够庸俗吧，人就是月没有什么越想得到什么，

        HashMap<String, String> tags = new HashMap<>();
        tags.put("java", "Primary school student");
        tags.put("go", "will use");
        tags.put("data,ai", "what......,haddop");
        user.setTags(tags);
    }
    @Test
    public void testEncodeAndDecode(){
        RemotingCommand request = RemotingCommand.request(1);
        request.setVersion(14);
        request.setCustomHeaders(user);

        ByteBuffer out = RemotingCoder.encode(request);

        RemotingCommand deRequest = RemotingCoder.decode(out);

        assert !deRequest.isResponse();
        assert !deRequest.isOneway();
        assert 1 == deRequest.getCode();
        assert deRequest.getVersion() == 14;

        User deUser = deRequest.getCustomHeaders(User.class);
        assert user.getName().equals(deUser.getName());
    }
}
