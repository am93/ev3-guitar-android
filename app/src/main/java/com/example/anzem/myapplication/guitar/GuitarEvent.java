package com.example.anzem.myapplication.guitar;

/**
 * Represents a guitar event sent from EV3.
 * Contains info about note height, pick action and arm position.
 *
 * @author Jure Jesensek
 */
public class GuitarEvent {

    /**
     * Enums containing an octave of notes (from C5 to C6) and their corresponding MIDI number.
     * <p />
     * Uses "english" notation concerning B/H note: C, C#, D, D#, E, F, F#, G, G#, A, <b>A#</b>,
     * <b>B</b>, C.
     */
    public enum Note {

        /** C5. */
        C5(60),
        /** C#5. */
        Csh5(61),
        /** D5. */
        D5(62),
        /** D#5. */
        Dsh5(63),
        /** E5. */
        E5(64),
        /** F5. */
        F5(65),
        /** F#5. */
        Fsh5(66),
        /** G5. */
        G5(67),
        /** G#5. */
        Gsh5(68),
        /** A5. */
        A5(69),
        /** A#5 (English notation). */
        Ash5(70),
        /** B5 (English notation). */
        B5(71),
        /** C6. */
        C6(72),
        /** C#6. */
        Csh6(73),
        /** Error note. */
        ERROR(-1);

        /** MIDI number. */
        public final int midiNumber;

        /**
         * Constructor.
         * @param value MIDI number.
         */
        Note(int value) {
            this.midiNumber = value;
        }
    }

    /**
     * Position of the octave arm.
     */
    public enum ArmPosition {
        /** Lowest position. No effect. */
        NORMAL,
        /** Lower middle position. Raise notes for an octave. */
        OCTAVE_UP,
        /** Upper middle position. Use C pentatonic scale. */
        PENTATONIC,
        /** Highest position. Use pitch bends. */
        SLIDE;

        /** Lowest allowed position of octave arm. */
        private static final int ARM_LOWEST_POSITION = 65;
        /** Highest allowed position of octave arm. */
        private static final int ARM_HIGHEST_POSITION = -5;

        public static ArmPosition toArmPosition(int rotation) {
            // normalize values
            if (rotation > ARM_LOWEST_POSITION) {
                rotation = ARM_LOWEST_POSITION;
            } else if (rotation < ARM_HIGHEST_POSITION) {
                rotation = ARM_HIGHEST_POSITION;
            }
            // move the range to [0, ...]
            rotation += -ARM_HIGHEST_POSITION;
            switch (rotation * ArmPosition.values().length / (ARM_LOWEST_POSITION - ARM_HIGHEST_POSITION + 1)) {
                case 0: return SLIDE;
                case 1: return PENTATONIC;
                case 2: return OCTAVE_UP;
                case 3: return NORMAL;
                default:
                    System.err.println("Arm position out of range - returning NORMAL.");
                    return NORMAL;
            }
        }
    }

    /** Enum representing musical and MIDI note. */
    public final Note note;
    /** Is note being played (picked). */
    public final boolean played;
    /** Signals that the octave arm on EV3 is raised. */
    public final ArmPosition armPosition;
    /** Pitch bend on a note. */
    public final int pitchBend;

    /** The value used for signalling that a note is being played on EV3. */
    private static final int PICKED = 0;

    /** Closest slider position on EV3's neck (highest note). */
    private static final int NECK_HIGHEST_POSITION = 1;
    /** Furthest slider position on EV3's neck (lowest note). */
    private static final int NECK_LOWEST_POSITION = 70;

    /** Default pitch bend amount - no bend. */
    public static final int PITCH_BEND_DEFAULT = 8192;
    /** Maximum allowed pitch bend - up or down a semitone. */
    private static final int PITCH_BEND_MAX_BEND = 4096;
    /** Lowest pitch bend amount - down a semitone. */
    private static final int PITCH_BEND_DOWN_LIMIT = PITCH_BEND_DEFAULT - PITCH_BEND_MAX_BEND;
    /** Highest pitch bend amount - up a semitone. */
    private static final int PITCH_BEND_UP_LIMIT = PITCH_BEND_DEFAULT + PITCH_BEND_MAX_BEND;

    /** MIDI number difference in an octave. */
    public static final int OCTAVE_MODIFIER = 12;

    /** Constructs a new GuitarEvent containing {@link Note#ERROR} note. */
    public GuitarEvent() {
        this.note = Note.ERROR;
        this.played = false;
        this.armPosition = ArmPosition.NORMAL;
        this.pitchBend = PITCH_BEND_DEFAULT;
    }

    /**
     * Constructs a new GuitarEvent object.
     * @param distance received slider distance on guitar neck.
     * @param played is the guitar "string" being "plucked".
     * @param armPosition rotation on octave arm.
     */
    public GuitarEvent(int distance, int played, int armPosition) {
        this.played = played == PICKED;
        this.armPosition = ArmPosition.toArmPosition(armPosition);

        if (distance < NECK_HIGHEST_POSITION) {
            distance = NECK_HIGHEST_POSITION;
        } else if (distance > NECK_LOWEST_POSITION) {
            distance = NECK_LOWEST_POSITION;
        }
        // without ERROR note
        final int NUMBER_OF_NOTES = this.armPosition == ArmPosition.PENTATONIC ? 5 : Note.values().length - 1;
        final int noteBucket = (int) Math.ceil((double) distance * NUMBER_OF_NOTES / (NECK_LOWEST_POSITION - NECK_HIGHEST_POSITION));

        if (this.armPosition == ArmPosition.PENTATONIC) {

            switch (noteBucket) {
                case 1: this.note = Note.C6; break;
                case 2: this.note = Note.A5; break;
                case 3: this.note = Note.G5; break;
                case 4: this.note = Note.E5; break;
                case 5: this.note = Note.D5; break;
                case 6: this.note = Note.C5; break;
                default: this.note = Note.ERROR;
            }

        } else {

            switch (noteBucket) {
                case 1: this.note = Note.Csh6; break;
                case 2: this.note = Note.C6; break;
                case 3: this.note = Note.B5; break;
                case 4: this.note = Note.Ash5; break;
                case 5: this.note = Note.A5; break;
                case 6: this.note = Note.Gsh5; break;
                case 7: this.note = Note.G5; break;
                case 8: this.note = Note.Fsh5; break;
                case 9: this.note = Note.F5; break;
                case 10: this.note = Note.E5; break;
                case 11: this.note = Note.Dsh5; break;
                case 12: this.note = Note.D5; break;
                case 13: this.note = Note.Csh5; break;
                case 14: this.note = Note.C5; break;
                default: this.note = Note.ERROR;
            }
        }

        // calculate pitch bend
        final int PITCH_BUCKETS_PER_NOTE = (NECK_LOWEST_POSITION - NECK_HIGHEST_POSITION + 1) / NUMBER_OF_NOTES;
        final int normalised = distance - PITCH_BUCKETS_PER_NOTE * (noteBucket - 1) - PITCH_BUCKETS_PER_NOTE / 2;
        this.pitchBend = PITCH_BEND_DEFAULT - ((PITCH_BEND_DOWN_LIMIT * normalised * 2) / PITCH_BUCKETS_PER_NOTE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GuitarEvent that = (GuitarEvent) o;

        if (played != that.played) return false;
        if (pitchBend != that.pitchBend) return false;
        if (note != that.note) return false;
        return armPosition == that.armPosition;
    }

    @Override
    public String toString() {
        return note + ", " + (played ? "" : "not ") + "played, arm position: " + armPosition.toString() + ", bend: " + pitchBend;
    }
}
