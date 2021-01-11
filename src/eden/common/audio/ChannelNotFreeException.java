// @formatter:off
package eden.common.audio;


/**
 *  A {@code ChannelNotFreeException} is thrown when a channel (perhaps on a
 *  {@code Mixer}) is currently attached to an existing {@code Source},
 *  preventing another {@code Source} from attaching to it. Detach its existing
 *  {@code Source} first before attaching another.
 *
 *  @author     Brendon
 *  @version    u0r1, 11/26/2018.
 */
public class ChannelNotFreeException extends RuntimeException {

//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Makes a {@code ChannelNotFreeException} with the given channel as its
     *  detail message
     */
    public ChannelNotFreeException(int channel) {
        super(Integer.toString(channel));
    }
}
