// @formatter:off
package eden.common.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *  An {@code OutputMixer} mixes {@code OutputSources} into a {@code
 *  SourceDataLine} for a singular audio output. A channel is denoted by an
 *  {@code int} indexed from {@code 0}. A dead {@code OutputMixer} does not
 *  permit further operations.
 *  <p>
 *  This implementation is designed for signed PCM streams. Later versions of
 *  this class may include support for unsigned streams.
 *
 *  @author     Brendon
 *  @version    u0r1, 11/26/2018.
 */
public class OutputMixer implements Runnable {

//~~PUBLIC CLASS CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Default buffer size in bytes */
    public static final int DEFAULT_BUFFER_SIZE = 4800;

    /** Default {@code OutputSource} capacity */
    public static final byte DEFAULT_CHANNELS = 8;


//~~OBJECT CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Audio data arrangement definition */
    private final AudioFormat format;

    /** AudioManipulator whose audio manipulation capabilities are to be used */
    private final AudioManipulator manipulator;

    /** Data line to which mixed audio data are to be written */
    private final SourceDataLine line;

    /** Audio streams to be read from, manipulated, and mixed altogether. */
    private final OutputSource[] sources;

    /** Channel audio manipulation parameters */
    private final ChannelDSPData[] dspData;

    /** Master audio manipulation parameters */
    private final ChannelDSPData thisDspData;

    /** Duration to sleep the mixing thread in milliseconds */
    private final long napLength;

    /** Size per sample in bytes */
    private final byte bytes;


//~~OBJECT FIELDS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Exception defining the death of this OutputMixer */
    private Exception deathCause;

    /** Read audio data to be manipulated then mixed into bufferMixd */
    private byte[] bufferRead;

    /** Mixed audio data to be written to line */
    private byte[] bufferMixd;

    /** Next channel to be pointed by channel-searching methods */
    private int sourcesIndex;

    /**
     *  Indicates both whether this OutputMixer is at solo mode, and the channel
     *  to isolate during so.
     */
    private short solo;

    /** Indicates whether the operations of this OutputMixer are paused */
    private boolean hold;

    /**
     *  Indicates whether the SourceDataLine was thrown a
     *  LineUnavailableException, preventing further operations.
     */
    private boolean dead;


//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Makes an {@code OutputMixer} that holds {@value #DEFAULT_CHANNELS}
     *  {@code OutputSources}, outputs CDDA-equivalent audio, and uses {@value
     *  #DEFAULT_BUFFER_SIZE}-byte buffers.
     */
    public OutputMixer() {
        this(DEFAULT_CHANNELS, Format.CDDA, DEFAULT_BUFFER_SIZE);
    }

    /**
     *  Makes an {@code OutputMixer} with the given number of {@code
     *  OutputSources}
     */
    public OutputMixer(byte channels) {
        this(channels, Format.CDDA, DEFAULT_BUFFER_SIZE);
    }

    /**
     *  Makes an {@code OutputMixer} with the given number of {@code
     *  OutputSources} and {@code AudioFormat}
     */
    public OutputMixer(byte channels, AudioFormat format) {
        this(channels, format, DEFAULT_BUFFER_SIZE);
    }

    /**
     *  Makes an {@code OutputMixer} with the given number of {@code
     *  OutputSources}, buffer size in bytes, and {@code AudioFormat}
     *
     *  @param      channels
     *              Maximum number of {@code OutputSources} this {@code
     *              OutputMixer} can attach to. Range: {@code [1, *]}, otherwise
     *              {@value #DEFAULT_CHANNELS} will be used.
     *
     *  @param      bufferSize
     *              Size of the mixed audio data buffer in bytes. {@code
     *              bufferSize > 0 && bufferSize % 48}, otherwise the nearest
     *              valid value will be used.
     */
    public OutputMixer(byte channels, AudioFormat format, int bufferSize) {
        if (bufferSize <= 0 || bufferSize % 48 != 0) {
            bufferSize -= bufferSize % 48;
        }
        if (channels <= 0) {
            channels = DEFAULT_CHANNELS;
        }
        this.format = format;

        this.manipulator  = new AudioManipulator((byte)
            (format.getSampleSizeInBits()), format.isBigEndian()
        );
        this.line         = makeLine();
        this.sources      = new OutputSource[channels];
        this.dspData      = makeDSPData();
        this.thisDspData  = new ChannelDSPData();

        this.napLength    = (long) Math.ceil(500 * (double)
            bufferSize / (format.getFrameSize() * format.getFrameRate())
        );
        this.bytes      = (byte) (format.getFrameSize() / format.getChannels());
        this.deathCause   = null;
        this.solo         = -1;
        this.dead         = false;
        this.bufferRead   = new byte[bufferSize];
        this.bufferMixd   = makeZeroBytes(bufferSize);
        this.sourcesIndex = 0;
        this.hold         = true;
    }


//~~PUBLIC OBJECT METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Runs this {@code OutputMixer}. This causes it to start reading,
     *  manipulating and mixing audio data from its {@code OutputSources} into
     *  its {@code SourceDataLine}.
     */
    public void run() {
        if (this.dead) {
            return;
        }
        int channel;
        int solo;
        boolean mute;
        this.line.start();

        while (!Thread.currentThread().isInterrupted() && !this.dead) {
            solo = this.solo;
            mute = this.thisDspData.isMuted();
            checkHold();

            for (channel = 0; channel < this.sources.length; channel++) {
                if (this.sources[channel] == null ||
                    this.sources[channel].isDone())
                {
                    continue;
                }
                if (this.dspData[channel].isMuted() ||
                   (solo >= 0 && channel != solo))
                {
                    this.sources[channel].skip(this.bufferRead.length);
                    continue;
                }
                this.sources[channel].read(this.bufferRead);

                if (!mute) {
                    manipulate(channel);
                }
            }
            manipulate();
            checkLine();
            this.line.write(this.bufferMixd, 0, this.bufferMixd.length);
            this.manipulator.zero(this.bufferMixd);
        }
        this.line.stop();
    }

    /**
     *  Attaches an {@code OutputSource} to a free channel on this {@code
     *  OutputMixer}
     *
     *  @param      source
     *              {@code OutputSource} to be attached. Passing {@code null}
     *              makes this method ineffective.
     *
     *  @return     The channel to which the given {@code OutputSource}
     *              attaches;
     *
     *              {@code -1}
     *              If there are no free channels;
     *
     *              {@code -2}
     *              If there is a fatal error, as this value should never be
     *              returned.
     */
    public int attach(OutputSource source) {
        int channel = getFreeChannel();

        if (channel < 0) {
            return channel;
        }
        try {
            attach(source, channel);
            return channel;
        } catch (ChannelNotFreeException e) {
            return -2;
        }
    }

    /**
     *  Attaches an {@code OutputSource} to the given channel on this {@code
     *  OutputMixer}
     *
     *  @param      source
     *              {@code OutputSource} to be attached. Passing {@code null}
     *              frees the given channel.
     *
     *  @param      channel
     *              Channel to which the given {@code OutputSource} is to be
     *              attached
     *
     *  @throws     IndexOutOfBoundsException
     *              If the given channel is out of range
     *
     *  @throws     ChannelNotFreeException
     *              If the given channel is currently attached to an existing
     *              {@code OutputSource}
     */
    public void attach(OutputSource source, int channel) throws
        IndexOutOfBoundsException,
        ChannelNotFreeException
    {
        validateChannel(channel);
        if (this.sources[channel] != null) {
            throw new ChannelNotFreeException(channel);
        }
        this.sources[channel] = source;
    }

    /**
     *  Attaches an {@code OutputSource} to the given channel on this {@code
     *  OutputMixer}, replacing its previously attached {@code OutputSource}.
     *
     *  @param      source
     *              {@code OutputSource} to be attached. Passing {@code null}
     *              frees the given channel.
     *
     *  @param      channel
     *              Channel to which the given {@code OutputSource} is to be
     *              attached
     *
     *  @return     The previous {@code OutputSource} attached to the given
     *              channel;
     *
     *              {@code null}
     *              Implies that the given channel was previously free
     *
     *  @throws     IndexOutOfBoundsException
     *              If the given channel is out of range
     */
    public OutputSource attachAndReplace(OutputSource source, int channel)
        throws IndexOutOfBoundsException
    {
        validateChannel(channel);
        OutputSource out = this.sources[channel];
        this.sources[channel] = source;
        return out;
    }

    /**
     *  Detaches the {@code OutputSource} from the given channel on this {@code
     *  OutputMixer} and returns it
     *
     *  @throws     IndexOutOfBoundsException
     *              If the given channel is out of range
     */
    public OutputSource detach(int channel) throws IndexOutOfBoundsException {
        validateChannel(channel);
        OutputSource out = getChannel(channel);

        if (out == null) {
            return null;
        }
        this.sources[channel] = null;
        return out;
    }


    /** Detaches all {@code OutputSources} from this {@code OutputMixer} */
    public void detachAll() {
        for (int i = 0; i < this.sources.length; i++) {
            this.sources[i] = null;
        }
    }

    /**
     *  Rewinds all {@code OutputSources} of this {@code OutputMixer} to their
     *  starting positions
     */
    public void rewindAll() {
        for (OutputSource s : this.sources) {
            if (s != null) {
                s.jumpToStart();
            }
        }
    }

    /**
     *  Resets the audio manipulation parameters of all channels on this {@code
     *  OutputMixer}
     */
    public void resetAllDSP() {
        for (ChannelDSPData d : this.dspData) {
            d.reset();
        }
    }

    /**
     *  Closes the {@code SourceDataLine} of this {@code OutputMixer}, enabling
     *  the dead flag.
     */
    public void close() {
        this.line.stop();
        this.line.close();
        die(new LineUnavailableException("Line closed"));
    }


//~~~~ACCESSORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Returns the next channel on this {@code OutputMixer} that is not
     *  attached to an {@code OutputSource}
     *
     *  @return    Next free channel;
     *
     *              {@code -1}
     *              If all channels are not free
     */
    public int getFreeChannel() {
        int out = nextChannel(this.sourcesIndex, true);

        if (out >= 0) {
            this.sourcesIndex = (out + 1) % this.sources.length;
        }
        return out;
    }

    /**
     *  Returns the next channel on this {@code OutputMixer} that is attached
     *  to an {@code OutputSource}
     *
     *  @return     Next attached channel;
     *
     *              {@code -1}
     *              If all channels are free
     */
    public int getAttachedChannel() {
        int out = nextChannel(this.sourcesIndex, false);

        if (out >= 0) {
            this.sourcesIndex = (out + 1) % this.sources.length;
        }
        return out;
    }

    /**
     *  Returns the number of {@code OutputSources} this {@code OutputMixer}
     *  can attach to
     */
    public int getCapacity() {
        return this.sources.length;
    }

    /**
     *  Returns the current channel attachment setup of this {@code OutputMixer}
     */
    public OutputSource[] getChannels() {
        return sources.clone();
    }

    /**
     *  Returns the {@code OutputSource} attached to a given channel on this
     *  {@code OutputMixer}
     *
     *  @throws     IndexOutOfBoundsException
     *              If the given channel is out of range
     */
    public OutputSource getChannel(int channel) throws IndexOutOfBoundsException
    {
        validateChannel(channel);
        return this.sources[channel];
    }

    /** Returns the audio manipulation parameters of this {@code OutputMixer} */
    public ChannelDSPData getDspData() {
        return this.thisDspData;
    }

    /**
     *  Returns the audio manipulation parameters of a given channel on this
     *  {@code OutputMixer}
     *
     *  @throws     IndexOutOfBoundsException
     *              If the given channel is out of range
     */
    public ChannelDSPData getDspData(int channel) throws
        IndexOutOfBoundsException
    {
        validateChannel(channel);
        return this.dspData[channel];
    }

    /**
     *  Returns the channel to isolate when this {@code OutputMixer} is at solo
     *  mode
     *
     *  @return     solo;
     *              A negative value implies that this {@code OutputMixer} is
     *              not at solo mode
     */
    public short getSolo() {
        return (short) this.solo;
    }

    /**
     *  Returns the {@code Exception} defining the death of this {@code
     *  OutputMixer}
     *
     *  @return     Cause of death;
     *
     *              {@code null}
     *              If this {@code OutputMixer} is not dead
     */
    public Exception getDeathCause() {
        return this.deathCause;
    }


//~~~~MUTATORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Sets the solo mode flag and channel of this {@code OutputMixer}
     *
     *  @param      solo
     *              Channel to isolate during solo mode. Passing a negative
     *              value disables solo mode.
     */
    public void setSolo(short solo) {
        this.solo = solo;
    }

    /** Sets whether the operations of this {@code OutputMixer} are paused */
    public synchronized void setHold(boolean hold) {
        this.hold = hold;
        notifyAll();
    }


//~~~~PREDICATES~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Returns whether the given channel is not attached to an {@code
     *  OutputSource}
     *
     *  @throws     IndexOutOfBoundsException
     *              If the given channel is out of range
     */
    public boolean isChannelFree(int channel) throws IndexOutOfBoundsException {
        validateChannel(channel);
        return this.sources[channel] == null;
    }

    /**
     *  Returns whether the given channel is valid within this {@code
     *  OutputMixer}
     */
    public boolean isValidChannel(int channel) {
        return (channel >= 0) && (channel < this.sources.length);
    }

    /** Returns both whether this {@code OutputMixer} is at solo mode */
    public boolean isSolo() {
        return this.solo >= 0;
    }

    /**
     *  Returns whether the {@code SourceDataLine} of this {@code OutputMixer}
     *  was thrown a {@code LineUnavailableException}, preventing further
     *  operations.
     */
    public boolean isDead() {
        return this.dead;
    }

    /** Returns whether the operations of this {@code OutputMixer} are paused */
    public boolean isHold() {
        return this.hold;
    }


//~~PRIVATE OBJECT METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Throws an IndexOutOfBoundsException if the given channel is invalid */
    private void validateChannel(int channel) throws IndexOutOfBoundsException {
        if (!isValidChannel(channel)) {
            throw new IndexOutOfBoundsException(Integer.toString(channel));
        }
    }

    /**
     *  Returns the next channel from the given channel with the given property
     */
    private int nextChannel(int from, boolean free) {
        int i = from;

        do {
            if ((free && this.sources[i % this.sources.length] == null) ||
               (!free && this.sources[i % this.sources.length] != null))
            {
                return i % this.sources.length;
            }
        } while (++i % this.sources.length != from);
        return -1;
    }

    /**
     *  Checks whether the operations of this OutputMixer are supposed to be
     *  paused, and waits if so.
     */
    private synchronized void checkHold() {
        while (this.hold) {
            this.line.flush();

            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     *  Checks whether the SourceDataLine of this OutputMixer is starved for
     *  writing. Using this method should provide similarity of results on
     *  different operating systems, audio hardware, and/or software systems.
     */
    private void checkLine() {
        while (this.line.available() <
              (this.line.getBufferSize() - this.bufferMixd.length))
        {
            try {
                Thread.sleep(this.napLength);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /** Manipulates the mixed audio data in bufferMixd */
    private void manipulate() {
        long sampleMixd;

        for (int i = 0;
            i + this.bytes <= this.bufferMixd.length;
            i += this.bytes)
        {
            sampleMixd = this.manipulator.bytesToInt(
                this.bufferMixd, i,
                this.format.isBigEndian()
            );
            sampleMixd = this.manipulator.amplify(
                sampleMixd,
                this.thisDspData.getAmplification()
            );
            this.manipulator.intToBytes(
                this.bufferMixd, this.manipulator.clip(sampleMixd), i
            );
        }
    }

    /**
     *  Manipulates the read audio data in bufferRead and mixes it into
     *  bufferMixd
     */
    private void manipulate(int channel) {
        long sampleRead;
        long sampleMixd;

        for (int i = 0;
            i + this.bytes <= this.bufferRead.length;
            i += this.bytes)
        {
            sampleRead = this.manipulator.bytesToInt(
                this.bufferRead, i,
                this.sources[channel].getFormat().isBigEndian()
            );
            sampleMixd = this.manipulator.bytesToInt(
                this.bufferMixd, i, this.format.isBigEndian()
            );
            sampleRead = this.manipulator.amplify(
                sampleRead,
                this.dspData[channel].getAmplification()
            );
            sampleMixd = this.manipulator.mix(sampleRead, sampleMixd);

            this.manipulator.intToBytes(
                this.bufferMixd, this.manipulator.clip(sampleMixd), i
            );
        }
    }

    /** Marks this OutputMixer dead with the given Exception as its cause */
    private void die(Exception exception) {
        this.dead = true;
        this.deathCause = exception;
    }


//~~~~CONSTRUCTOR HELPERS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Makes an array of initialized ChannelDSPData */
    private ChannelDSPData[] makeDSPData() {
        ChannelDSPData[] out = new ChannelDSPData[this.sources.length];

        for (byte b = 0; b < out.length; b++) {
            out[b] = new ChannelDSPData();
        }
        return out;
    }

    /** Makes an array of zero bytes */
    private byte[] makeZeroBytes(int bufferSize) {
        byte[] out = new byte[bufferSize];

        for (bufferSize--; bufferSize >=0; bufferSize--) {
            out[bufferSize] = 0;
        }
        return out;
    }

    /** Makes an opened SourceDataLine */
    private SourceDataLine makeLine() {
        try {
            SourceDataLine out = AudioSystem.getSourceDataLine(this.format);
            out.open();
            return out;
        } catch (LineUnavailableException e) {
            die(e);
            return null;
        }
    }
}
