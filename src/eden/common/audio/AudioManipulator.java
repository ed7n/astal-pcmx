// @formatter:off
package eden.common.audio;


/**
 *  An {@code AudioManipulator} manipulates PCM audio data. It is designed to
 *  work with signed values. Therefore, the commonly-used unsigned 8-bit PCM
 *  does not work here for now. Future releases of this class will cater the
 *  aforementioned limitations.
 *
 *  @author     Brendon
 *  @version    u0r1, 11/26/2018.
 */
public class AudioManipulator {

//~~PRIVATE CLASS CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Used with the & operator to retain bits in Fs and discard bits in 0s */
    private static final int[] BIT_MASK = {
        0x00000000,
        0x000000FF,
        0x0000FFFF,
        0x00FFFFFF,
        0xFFFFFFFF
    };


//~~OBJECT FIELDS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Maximum sample value */
    private long maxValue;

    /** Minimum sample value */
    private long minValue;

    /** Size of a sample in bytes */
    private byte bytes;

    /** Size of a sample in bits */
    private byte bits;

    /** Indicates whether the endianness of output byte[]s is big */
    private boolean bigEndian;


//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Makes an {@code AudioManipulator} that manipulates 16-bit little endian
     *  audio samples
     */
    public AudioManipulator() {
        this((byte) 16, false);
    }

    /**
     *  Makes an {@code AudioManipulator} with the given bit depth and the
     *  big endian flag set to false
     */
    public AudioManipulator(byte bitDepth) {
        this(bitDepth, false);
    }

    /**
     *  Makes an {@code AudioManipulator} with the given bit depth and big
     *  endian flag
     *
     *  @param      bitDepth
     *              Size of a sample in bits. Current limitations of this class
     *              requires this value to be divisible by 8. Otherwise, it will
     *              be rounded down to the nearest valid value.
     */
    public AudioManipulator(byte bitDepth, boolean bigEndian) {
        if (bitDepth % 8 != 0) {
            bitDepth = (byte) (bitDepth - bitDepth % 8);
        }
        if (bitDepth < 8) {
            bitDepth = 8;
        } else if (bitDepth > 32) {
            bitDepth = 32;
        }
        this.maxValue  = (long) (Math.pow(2, bitDepth - 1) -  1);
        this.minValue  = (long) (Math.pow(2, bitDepth - 1) * -1);
        this.bytes     = (byte) (bitDepth / 8);
        this.bits      = bitDepth;
        this.bigEndian = bigEndian;
    }


//~~PUBLIC OBJECT METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Returns an {@code int} conversion of a sample whose value is represented
     *  in the given {@code byte[]}, formatted in the endianness of this {@code
     *  AudioManipulator}, starting from the given index.
     */
    public int bytesToInt(byte[] bytes, int index) {
        return bytesToInt(bytes, index, this.bigEndian);
    }

    /**
     *  Returns an {@code int} conversion of a sample whose value is represented
     *  in the given {@code byte[]}, formatted in the given endianness, starting
     *  from the given index.
     */
    public int bytesToInt(byte[] bytes, int index, boolean bigEndian) {
        // ack: https://stackoverflow.com/a/18219399
        int out = 0;

        for (int i = 0; i < this.bytes; i++) {
            out |= !bigEndian ?
                (bytes[index + i] & 0xFF) << (8 * i) :
                (bytes[index + this.bytes - 1 - i] & 0xFF) << (8 * i);
        }
        if ((!bigEndian && bytes[index + this.bytes - 1] < 0) ||
             (bigEndian && bytes[index] < 0))
        {
            out = ~out + 1;
            out &= BIT_MASK[this.bytes];
            out = ~out + 1;
            out |= 0x80000000;
        }
        return out;
    }

    /** Amplifies the given sample by the given factor */
    public long amplify(long sample, float factor) {
        return Math.round(sample * factor);
    }

    /** Mixes samples {@code a} and {@code b} */
    public long mix(long a, long b) {
        return a + b;
    }

    /** Clips the given sample within the value bounds */
    public int clip(long sample) {
        if (sample > this.maxValue) {
            return (int) this.maxValue;
        }
        if (sample < this.minValue){
            return (int) this.minValue;
        }
        return (int) sample;
    }

    /**
     *  Represents the given value in the given {@code byte[]}, formatted in the
     *  endianness of this {@code AudioManipulator}, starting from the given
     *  index.
     */
    public void intToBytes(byte[] bytes, int value, int index) {
        // ack: https://stackoverflow.com/a/2183259
        for (int i = 0; i < this.bytes; i++) {
            if (this.bigEndian) {
                bytes[index + this.bytes - 1 - i] = (byte) (value >> (8 * i));
            } else {
                bytes[index + i] = (byte) (value >> (8 * i));
            }
        }
    }

    /**
     *  Fills the given {@code byte[]} with zeros. This is equivalent to calling
     *  {@code Arrays.fill(bytes, (byte) 0)}.
     */
    public void zero(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
        }
    }


//~~~~ACCESSORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Returns the size of a sample in bits for this {@code AudioManipulator}
     */
    public byte getBits() {
        return this.bits;
    }


//~~~~PREDICATES~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Returns whether the endianness of output byte[]s is big */
    public boolean isBigEndian() {
        return this.bigEndian;
    }
}
