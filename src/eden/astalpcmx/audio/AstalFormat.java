// @formatter:off
package eden.astalpcmx.audio;

import javax.sound.sampled.AudioFormat;

/**
 *  This class provides definitions for Astal audio formats.
 *
 *  @author     Brendon
 *  @version    u0r0, 01/17/2019.
 */
public class AstalFormat {

//~~PUBLIC CLASS CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** P16 (16-bit PCM) files */
    public static final AudioFormat P16 = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100,
        16,
        2,
        4,
        44100,
        true
    );


//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** To prevent instantiations of this class */
    private AstalFormat(){}
}
