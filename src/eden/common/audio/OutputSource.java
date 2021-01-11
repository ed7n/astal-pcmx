// @formatter:off
package eden.common.audio;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;


/**
 *  An {@code OutputSource} holds a stream of audio data and reads from it as
 *  necessary. The underlying {@code InputStream} must support its {@code mark}
 *  and {@code reset} methods in order for jumping operations to work. A dead
 *  {@code OutputSource} does not permit further operations.
 *
 *  @author     Brendon
 *  @version    u0r1, 11/26/2018.
 */
public class OutputSource {

//~~OBJECT CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Stream of audio data to be read from */
    protected final InputStream stream;

    /** Audio data arrangement definition */
    private final AudioFormat format;

    /** Amount of data from the start of the InputStream to its end in bytes */
    private final int streamSize;


//~~OBJECT FIELDS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Exception defining the death of this OutputSource */
    private Exception deathCause;

    /**
     *  Amount of data from the start of the InputStream to where the mark is
     *  set in bytes
     */
    private int markPosition;

    /**
     *  Indicates whether the InputStream resets to its start once it reaches
     *  its end
     */
    protected boolean loop;

    /** Indicates whether the InputStream has reached its end */
    protected boolean done;

    /**
     *  Indicates whether the InputStream either was thrown an IOException, or
     *  has been closed, preventing further operations.
     */
    protected boolean dead;


//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Makes an {@code OutputSource} with the given {@code InputStream} and
     *  {@code AudioFormat}
     *
     *  @throws     IOException
     *              If an I/O error occurs
     */
    public OutputSource(InputStream stream, AudioFormat format)
        throws IOException
    {
        this(stream, format, false);
    }

    /**
     *  Makes an {@code OutputSource} with the given {@code InputStream},
     *  {@code AudioFormat}, and loop flag.
     *
     *  @throws     IOException
     *              If an I/O error occurs
     */
    public OutputSource(InputStream stream, AudioFormat format, boolean loop)
        throws IOException
    {
        this.stream       = stream;
        this.format       = format;
        this.streamSize   = stream.available();
        this.deathCause   = null;
        this.markPosition = 0;
        this.loop         = makeLoop(loop);
        this.done         = false;
        this.dead         = false;
    }


//~~PUBLIC OBJECT METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Reads from the {@code InputStream} of this {@code OutputSource} as much
     *  data the given buffer can hold.
     *
     *  @return     The actual amount of data read in bytes;
     *
     *              {@code -1}
     *              If this {@code OutputSource} has went or is dead
     */
    public int read(byte[] buffer) {
        if (this.dead) {
            return -1;
        } else if (buffer == null || (this.done && !this.loop)) {
            return 0;
        }
        try {
            if (this.loop) {
                return readSeamlessly(buffer);
            }
            int readSize = this.stream.read(buffer, 0, buffer.length);

            if (readSize < buffer.length) {
                this.done = true;
                zero(buffer, readSize < 0 ? 0 : readSize);
            } else if (this.stream.available() == 0) {
                this.done = true;
            }
            return readSize;
        } catch (IOException e) {
            die(e);
            return -1;
        }
    }

    /**
     *  Skips over the given amount of data from the {@code InputStream} of
     *  this {@code OutputSource} in bytes.
     *  <p>
     *  Caution with PCM streams: When calling this method prior to mark, ensure
     *  that {@code amount} is divisible by the frame size. Otherwise the
     *  resulting audio turns into noise upon looping. However, if {@code
     *  amount} is divisible by half the frame size, then the resulting audio
     *  remains intact except that its channels are swapped. The frame size of
     *  an {@code OutputSource} can be retrieved by invoking {@code
     *  getFormat().getFrameSize()}.
     */
    public void skip(long amount) {
        if (this.dead) {
            return;
        }
        try {
            if (this.loop) {
                skipSeamlessly(amount);
                return;
            }
            while (amount > 0) {
                amount -= this.stream.skip(amount);

                if (this.stream.available() == 0) {
                    this.done = true;
                    return;
                }
            }
            updateDone();
        } catch (IOException e) {
            die(e);
        }
    }

    /**
     *  Marks the current {@code InputStream} position of this {@code
     *  OutputSource}
     */
    public void mark() {
        if (this.dead) {
            return;
        }
        try {
            this.markPosition = this.streamSize - this.stream.available();
        } catch (IOException e) {
            die(e);
        }
    }

    /**
     *  Jumps to the marked {@code InputStream} position of this {@code
     *  OutputSource}
     */
    public void jumpToMark() {
        if (this.dead) {
            return;
        }
        try {
            this.stream.reset();
            skip(this.markPosition);
        } catch (IOException e) {
            die(e);
        }
    }

    /**
     *  Jumps to the starting {@code InputStream} position of this {@code
     *  OutputSource}
     */
    public void jumpToStart() {
        if (this.dead) {
            return;
        }
        try {
            this.stream.reset();
            updateDone();
        } catch (IOException e) {
            die(e);
        }
    }

    /**
     *  Closes the {@code InputStream} of this {@code OutputSource}, enabling
     *  the dead flag.
     */
    public void close() {
        if (this.dead) {
            return;
        }
        try {
            this.stream.close();
            die(new IOException("Stream closed"));
        } catch (IOException e) {
            die(e);
        }
    }


//~~~~ACCESSORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Returns the audio data arrangement definition of this {@code
     *  OutputSource}
     */
    public AudioFormat getFormat() {
        return this.format;
    }

    /**
     *  Returns the amount of data from the start of the {@code InputStream} of
     *  this {@code OutputSource} to its end in bytes
     */
    public int getStreamSize() {
        return this.streamSize;
    }

    /**
     *  Returns the amount of data from the start of the {@code InputStream} of
     *  this {@code OutputSource} to where the mark is set in bytes
     */
    public int getMarkPosition() {
        return this.markPosition;
    }

    /**
     *  Returns the amount of data available in the {@code InputStream} of this
     *  {@code OutputSource} to be read or skipped over in bytes
     *
     *  @return     Amount of data available in bytes;
     *
     *              {@code -1}
     *              If this {@code OutputSource} has went or is dead
     */
    public int getAvailable() {
        if (this.dead) {
            return -1;
        }
        try {
            return this.stream.available();
        } catch (IOException e) {
            die(e);
            return -1;
        }
    }

    /**
     *  Returns the amount of data from the start of the {@code InputStream} of
     *  this {@code OutputSource} to its current position in bytes
     */
    public int getPosition() {
        if (this.dead) {
            return -1;
        }
        try {
            return this.streamSize - this.stream.available();
        } catch (IOException e) {
            die(e);
            return -1;
        }
    }

    /** Returns the length of this {@code OutputSource} in seconds */
    public double getDurationSecond() {
        return (double) this.streamSize /
            (this.format.getFrameSize() * this.format.getFrameRate());
    }

    /** Returns the elapsed time for this {@code OutputSource} in seconds */
    public double getElapsedSecond() {
        if (this.dead) {
            return 0;
        }
        try {
            return (double) (this.streamSize - this.stream.available()) /
                (this.format.getFrameSize() * this.format.getFrameRate());
        } catch (IOException e) {
            die (e);
            return 0;
        }
    }

    /** Returns the percentual progress for this {@code OutputSource} */
    public double getElapsedPercent() {
        if (this.dead) {
            return 0;
        }
        try {
            return (double)
                (this.streamSize - this.stream.available()) / this.streamSize;
        } catch (IOException e) {
            die(e);
            return 0;
        }
    }

    /**
     *  Returns the {@code Exception} defining the death of this {@code
     *  OutputSource}
     *
     *  @return     Cause of death;
     *
     *              {@code null}
     *              If this {@code OutputSource} is not dead
     */
    public Exception getDeathCause() {
        return this.deathCause;
    }


//~~~~MUTATORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Sets whether the {@code InputStream} of this {@code OutputSource}
     *  resets to its start once it reaches its end
     *
     *  @return     {@code true}
     *              If the loop flag is set, and looping is supported;
     *
     *              {@code false}
     *              If the loop flag is unset, or looping is unsupported
     */
    public boolean setLoop(boolean loop) {
        this.loop = loop && this.stream.markSupported();
        return this.loop;
    }

    /**
     *  Toggles whether the {@code InputStream} of this {@code OutputSource}
     *  resets to its start once it reaches its end, and returns its new
     *  value.
     */
    public boolean toggleLoop() {
        this.loop = !this.loop && this.stream.markSupported();
        return this.loop;
    }


//~~~~PREDICATES~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Returns whether the {@code InputStream} of this {@code OutputSource}
     *  resets to its start once it reaches its end
     */
    public boolean isLoop() {
        return this.loop;
    }

    /**
     *  Returns whether the {@code InputStream} of this {@code OutputSource}
     *  has reached its end
     */
    public boolean isDone() {
        return this.done;
    }

    /**
     *  Returns whether the {@code InputStream} of this {@code OutputSource}
     *  was thrown an {@code IOException}, preventing further operations.
     */
    public boolean isDead() {
        return this.dead;
    }


//~~PRIVATE OBJECT METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Reads from the InputStream of this OutputSource, and stores as much data
     *  the given buffer can hold, jumping to the marked InputStream position as
     *  necessary.
     *
     *  @return     The actual amount of data read in bytes
     *
     *  @throws     IOException
     *              If a read operation fails or is interrupted
     */
    private int readSeamlessly(byte[] buffer) throws IOException {
        int readSize = 0;

        if (this.stream.available() == 0) {
            this.stream.reset();
            skipSeamlessly(this.markPosition);
        }
        while (true) {
            readSize += this.stream.read(
                buffer, readSize, buffer.length - readSize
            );
            if (readSize >= buffer.length) {
                break;
            }
            this.stream.reset();
            skipSeamlessly(this.markPosition);
        }
        return readSize;
    }

    /**
     *  Fills the rest of the given byte[] with zeros starting from bytes[index]
     */
    protected void zero(byte[] bytes, int index) {
        while (index < bytes.length) {
            bytes[index++] = 0;
        }
    }

    /**
     *  Skips over the given amount of data from the InputStream of this
     *  OutputSource in bytes, jumping to the marked InputStream position as
     *  necessary.
     */
    private void skipSeamlessly(long amount) throws IOException {
        while (amount > 0) {
            amount -= this.stream.skip(amount);

            if (this.stream.available() == 0) {
                this.stream.reset();
                amount += this.markPosition;
            }
        }
        updateDone();
    }

    /**
     *  Updates the end-of-stream flag depending on the amount of data available
     *  in the stream to be read or skipped over
     */
    private void updateDone() throws IOException {
        this.done = this.stream.available() == 0 && !this.loop;
    }

    /** Marks this OutputSource dead with the given Exception as its cause */
    protected void die(Exception exception) {
        this.dead = true;
        this.deathCause = exception;
    }


//~~~~CONSTRUCTOR HELPERS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Initializes the InputStream for looping
     *
     *  @return     true
     *              If the loop flag is set, and looping is supported;
     *
     *              false
     *              If the loop flag is unset, or looping is unsupported
     */
    private boolean makeLoop(boolean loop) {
        try {
            if (this.stream.markSupported()) {
                this.stream.mark(this.stream.available() + 1);
                return loop;
            }
        } catch (IOException e) {
            die(e);
        }
        return false;
    }
}
