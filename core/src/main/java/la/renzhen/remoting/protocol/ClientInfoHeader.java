package la.renzhen.remoting.protocol;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 20:08
 */
@Setter
@Getter
@Accessors(chain = true)
public class ClientInfoHeader implements CommandCustomHeader {

    String unique;

    String module;

    Map<String, String> attrs;

    String authUsername;

    String authSign;

    public ClientInfoHeader addAttr(String attrKey, String attrValue) {
        if (attrs == null) {
            attrs = new HashMap<>();
        }
        attrs.put(attrKey, attrValue);
        return this;
    }
}
