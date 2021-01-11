// @formatter:off
package eden.common.audio;

import java.util.Objects;


/**
 *  A {@code ChannelDSPData} defines the mixing and manipulation parameters for
 *  (an) audio channel(s).
 *
 *  @author     Brendon
 *  @version    u0r1, 11/26/2018.
 */
public class ChannelDSPData {

//~~PUBLIC CLASS CONSTANTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Default amplification factor with which a sample is to be multiplied */
    public static final float DEFAULT_AMPLIFICATION = 1;


//~~OBJECT FIELDS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Amplification factor with which a sample is to be multiplied */
    private float amplification;

    /** Indicates whether the defined channel(s) is/are muted */
    private boolean muted;


//~~CONSTRUCTORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Makes a {@code ChannelDSPData} with the default manipulation parameters
     */
    public ChannelDSPData() {
        this(DEFAULT_AMPLIFICATION, false);
    }

    /**
     *  Makes a {@code ChannelDSPData} with the given amplification factor and
     *  mute flag
     */
    public ChannelDSPData(float amplification, boolean muted) {
        this.amplification = amplification;
        this.muted         = muted;
    }

    /** Makes a copy of the given {@code ChannelDSPData} */
    public ChannelDSPData(ChannelDSPData copy) {
        this.amplification = copy.amplification;
        this.muted         = copy.muted;
    }


//~~PUBLIC OBJECT METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Resets this {@code ChannelDSPData} to the default manipulation
     *  parameters
     */
    public void reset() {
        this.amplification = DEFAULT_AMPLIFICATION;
        this.muted = false;
    }


//~~~~ACCESSORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** Returns the amplification factor of this {@code ChannelDSPData} */
    public float getAmplification() {
        return this.amplification;
    }


//~~~~MUTATORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Sets the amplification factor for the defined channels of this {@code
     *  ChannelDSPData}
     */
    public void setAmplification(float amplification) {
        this.amplification = amplification;
    }

    /**
     *  Sets whether the defined channel(s) of this {@code ChannelDSPData}
     *  is/are muted
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    /**
     *  Toggles whether the defined channel(s) of this {@code ChannelDSPData}
     *  is/are muted and returns its new value
     */
    public boolean toggleMuted() {
        this.muted = !this.muted;
        return this.muted;
    }


//~~~~PREDICATES~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  Returns whether the defined channel(s) of this {@code ChannelDSPData}
     *  is/are muted
     */
    public boolean isMuted() {
        return this.muted;
    }


//~~~~OPERATORS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** @inheritDoc */
    @Override
    public boolean equals(Object o) {
        return
            o            == this       ||
           (o            != null       &&
            o.getClass() == getClass() &&
            equals((ChannelDSPData) o));
    }

    /** @inheritDoc */
    @Override
    public int hashCode() {
        return Objects.hash(
            amplification, muted
        );
    }

    /**
     *  Returns whether the given {@code ChannelDSPData} is equivalent to this
     */
    public boolean equals(ChannelDSPData d) {
        return
            d != null &&
            this.amplification == d.amplification &&
            this.muted == d.muted;
    }
}
