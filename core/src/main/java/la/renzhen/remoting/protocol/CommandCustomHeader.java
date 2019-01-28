package la.renzhen.remoting.protocol;


import la.renzhen.remoting.RemotingException;

import java.io.Serializable;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-26 20:51
 */
public interface CommandCustomHeader extends Serializable {

    /**
     * Check for mandatory fields, if the required field does not exist, throw {@link RemotingException.Type#Command} exception
     *
     * @return Is the field correct?
     *
     * @throws RemotingException {@link RemotingException.Type#Command} exception
     */
    default boolean checkFields() throws RemotingException {
        return true;
    }
}