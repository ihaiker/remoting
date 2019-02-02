package la.renzhen.remoting.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 20:08
 */
@Data
@Accessors(chain = true)
public class ClientInfoHeader implements CommandCustomHeader {

    String unique;

    String module;

    Map<String, String> attrs;

    public ClientInfoHeader addAttr(String attrKey, String attrValue) {
        if (attrs == null) {
            attrs = new HashMap<>();
        }
        attrs.put(attrKey, attrValue);
        return this;
    }
}
