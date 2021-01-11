// @formatter:off
package eden.common.audio;

import javax.sound.sampled.AudioFormat;


/**
 *  This class provides definitions for common consumer audio formats.
 *
 *  @author     Brendon
 *  @version    u0r1, 11/26/2018.
 */
public class Format {

//~~PUBLIC CLASS CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Compact Disc Digital Audio (CDDA), known simply as CD. */
    public static final AudioFormat CDDA = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100,
        16,
        2,
        4,
        44100,
        false
    );

    /** Digital Audio Tape (DAT), also the standard for high-fidelity audio. */
    public static final AudioFormat DAT = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        48000,
        16,
        2,
        4,
        48000,
        false
    );

    /** "Hi-Res" audio, a mythical format that so-called audiophiles prefer. */
    public static final AudioFormat HI_RES = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        96000,
        24,
        2,
        4,
        96000,
        false
    );

    /** Digital telephony: A-law */
    public static final AudioFormat TELEPHONE_A = new AudioFormat(
        AudioFormat.Encoding.ALAW,
        8000,
        8,
        1,
        1,
        8000,
        false
    );

    /** Digital telephony: µ-law */
    public static final AudioFormat TELEPHONE_U = new AudioFormat(
        AudioFormat.Encoding.ULAW,
        8000,
        8,
        1,
        1,
        8000,
        false
    );


//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** To prevent instantiations of this class */
    private Format(){}
}
