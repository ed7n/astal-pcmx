// @formatter:off
package eden.astalpcmx.audio;

import eden.common.audio.OutputSource;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;


/**
 *  An {@code AstalOutputSource} holds a stream of Astal "P16" audio data and
 *  reads from it as necessary.
 *  <p>
 *  These non-standard streams alternate between each of the stereo channels per
 *  2048 samples, with the right channel being the first.
 *
 *  @author     Brendon
 *  @version    u0r0, 01/17/2019.
 *
 *  @see        OutputSource
 */
public class AstalOutputSource extends OutputSource {

//~~OBJECT CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Default buffer size in bytes */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /** Channel frame size in bytes */
    public static final int CHANNEL_FRAME_SIZE = 4096;


//~~OBJECT FIELDS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Buffer to parse multiples of two 2048-sample chunks */
    private byte[] buffer;

    /** Left channel byte array offset */
    private int offsetLeft;

    /** Right channel byte array offset */
    private int offsetRight;

    /** Remaining unread bytes in buffer */
    private int remain;

    /** Indicates whether the left channel is to be parsed */
    private boolean channel;


//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Makes an {@code AstalOutputSource} with the given {@code InputStream}
     *  {@code AudioFormat}. For this version, {@code format} is required to be
     *  {@link AstalFormat#P16}.
     *
     *  @throws     IOException
     *              If an I/O error occurs
     */
    public AstalOutputSource(InputStream stream, AudioFormat format)
        throws IOException
    {
        this(stream, format, false);
    }

    /**
     *  Makes an {@code AstalOutputSource} with the given {@code InputStream},
     *  {@code AudioFormat}, and loop flag. For this version, {@code format} is
     *  required to be {@link AstalFormat#P16}.
     *
     *  @throws     IOException
     *              If an I/O error occurs
     */
    public AstalOutputSource(InputStream stream,
                             AudioFormat format,
                                 boolean loop)
        throws IOException
    {
        super(stream, format, loop);
        this.buffer = new byte[DEFAULT_BUFFER_SIZE];
        this.offsetLeft = 0;
        this.offsetRight = 0;
        this.remain = 0;
        this.channel = false;
    }


//~~PUBLIC OBJECT METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Reads and parses the {@code InputStream} of this {@code
     *  AstalOutputSource} as much data the given buffer can hold.
     *
     *  @return     The actual amount of data read and parsed in bytes;
     *
     *              {@code -1}
     *              If this {@code AstalOutputSource} has went or is dead
     */
    @Override
    public int read(byte[] buffer) {
        if (this.dead) {
            return -1;
        } else if (buffer == null || (this.done && !this.loop)) {
            return 0;
        }
        try {
            int offset = 0;

            while (offset < buffer.length) {
                if (this.remain <= 0) {
                    if (this.stream.available() == 0) {
                        if (this.loop) {
                            this.stream.reset();
                        } else {
                            zero(buffer, offset);
                            this.done = true;
                            break;
                        }
                    }
                    this.remain = this.stream.read(
                        this.buffer, 0, this.buffer.length
                    );
                    this.offsetLeft = 0;
                    this.offsetRight = CHANNEL_FRAME_SIZE;
                }
                if (this.channel) {
                    buffer[offset++] = this.buffer[this.offsetLeft++];
                    buffer[offset++] = this.buffer[this.offsetLeft++];
                } else {
                    buffer[offset++] = this.buffer[this.offsetRight++];
                    buffer[offset++] = this.buffer[this.offsetRight++];
                }
                this.remain -= 2;
                this.channel = !this.channel;
            }
            return offset;
        } catch (IOException e) {
            die(e);
            return -1;
        }
    }
}
