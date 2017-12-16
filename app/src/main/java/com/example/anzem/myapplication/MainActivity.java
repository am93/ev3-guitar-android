package com.example.anzem.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.anzem.myapplication.guitar.GuitarEvent;

import org.billthefarmer.mididriver.MidiDriver;

import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements MidiDriver.OnMidiStartListener,
        View.OnTouchListener {

    private Spinner spInstrument;
    private ProgressBar pbNeck;
    private RadioButton rbOctave;
    private RadioButton rbPentatonic;
    private RadioButton rbSlide;

    private MidiDriver midiDriver;
    private byte[] event;
    private int[] config;

    private String[] instruments = {"Piano", "Marimba", "Rock Organ", "Accordion", "Guitar", "Electric Guitar", "Overdriven guitar", "Distortion Guitar", "Acoustic bass"};
    private int[] idxs = {1,13, 19, 22, 25, 27, 30, 31, 33};
    private HashMap<String, Integer> instrVals = new HashMap<>();

    private GuitarEvent oldEvent = null;

    private BluetoothConnectionService  mBluetoothConnection;

    /* -------------------------------------------------------------------
     * INITIALIZATION PART
     * ------------------------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instruments spinner
        spInstrument = (Spinner)findViewById(R.id.spInstruments);
        initInstrumentValues();
        initSpinner();
        spInstrument.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                changeInstrument((byte)idxs[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                return;
            }
        });

        // guitar neck progressbar
        pbNeck = (ProgressBar) findViewById(R.id.pbNeck);

        // modifiers
        rbOctave = (RadioButton) findViewById(R.id.rbOctave);
        rbPentatonic = (RadioButton) findViewById(R.id.rbPentatonic);
        rbSlide = (RadioButton) findViewById(R.id.rbSlide);

        // Instantiate the driver.
        midiDriver = new MidiDriver();
        // Set the listener.
        midiDriver.setOnMidiStartListener(this);

        mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        midiDriver.start();

        // Get the configuration.
        config = midiDriver.config();

        // Print out the details.
        Log.d(this.getClass().getName(), "maxVoices: " + config[0]);
        Log.d(this.getClass().getName(), "numChannels: " + config[1]);
        Log.d(this.getClass().getName(), "sampleRate: " + config[2]);
        Log.d(this.getClass().getName(), "mixBufferSize: " + config[3]);

        changeInstrument((byte)1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        midiDriver.stop();
    }

    private void initInstrumentValues()
    {
        for(int i = 0; i < idxs.length; i++)
        {
            instrVals.put(instruments[i], idxs[i]);
        }
    }

    private void initSpinner()
    {
        spInstrument.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, instruments));
    }

    private void instrumentSelection()
    {
        String ins = (String)spInstrument.getSelectedItem();
        int idx = instrVals.get(ins);
        changeInstrument((byte)idx);
    }

    /* -------------------------------------------------------------------
     * MIDI RELATED
     * ------------------------------------------------------------------- */
    @Override
    public void onMidiStart() {
        Log.d(this.getClass().getName(), "onMidiStart()");
    }

    private void playNote(byte note) {
        // Construct a note ON message for the middle C at maximum velocity on channel 1:
        event = new byte[3];
        event[0] = (byte) (0x90 | 0x00);  // 0x90 = note On, 0x00 = channel 1
        event[1] = note;  // 0x3C = middle C
        event[2] = (byte) 0x7F;  // 0x7F = the maximum velocity (127)

        // Send the MIDI event to the synthesizer.
        midiDriver.write(event);

    }

    private void changeInstrument(byte inst) {
        // Construct a note ON message for the middle C at maximum velocity on channel 1:
        event = new byte[3];
        event[0] = (byte) (0xC0 | 0x00);  // 0x90 = note On, 0x00 = channel 1
        event[1] = (byte) 0x00;  // 0x3C = middle C
        event[2] = inst;  // 0x7F = the maximum velocity (127)

        // Send the MIDI event to the synthesizer.
        midiDriver.write(event);

    }

    private void stopNote(byte note) {
        // Construct a note OFF message for the middle C at minimum velocity on channel 1:
        event = new byte[3];
        event[0] = (byte) (0x80 | 0x00);  // 0x80 = note Off, 0x00 = channel 1
        event[1] = (byte) note;  // 0x3C = middle C
        event[2] = (byte) 0x00;  // 0x00 = the minimum velocity (0)

        // Send the MIDI event to the synthesizer.
        midiDriver.write(event);
    }

    private void setPitchBend(int value) {
        byte lsb = (byte)(value & 0x7F);
        byte msb = (byte)((value >> 7) & 0x7F);
        event = new byte[3];
        event[0] = (byte) (0xD0 | 0x00);  // 0x80 = note Off, 0x00 = channel 1
        event[1] = lsb;  // 0x3C = middle C
        event[2] = msb;  // 0x00 = the minimum velocity (0)

        // Send the MIDI event to the synthesizer.
        midiDriver.write(event);
    }

    /* -------------------------------------------------------------------
     * GUITAR EVENTS
     * ------------------------------------------------------------------- */

    private void visualizeModifier(GuitarEvent.ArmPosition position)
    {
        switch (position)
        {
            case SLIDE:
                rbSlide.setChecked(true);
                break;
            case OCTAVE_UP:
                rbOctave.setChecked(true);
                break;
            case PENTATONIC:
                rbPentatonic.setChecked(true);
                break;
            default:
                rbPentatonic.setChecked(false);
                rbOctave.setChecked(false);
                rbSlide.setChecked(false);
        }
    }

    private Integer[] parseMessage(String message)
    {
        String[] parts = message.split(";");
        Integer[] result = new Integer[parts.length];
        for(int i = 0; i < result.length; i++)
        {
            result[i] = new Integer(parts[i]);
        }

        return result;
    }

    public void processNewGuitarEvent(String message)
    {
        Integer[] receive = parseMessage(message);
        GuitarEvent event = new GuitarEvent(receive[0], receive[1], receive[2]);

        // visualize recieved value
        pbNeck.setProgress(receive[0]);

        if (oldEvent == null || (event.played && !event.equals(oldEvent) && event.note != GuitarEvent.Note.ERROR)) {
            if(oldEvent != null)
                stopNote((byte)oldEvent.note.midiNumber);
            int midiNumber = event.note.midiNumber + (event.armPosition == GuitarEvent.ArmPosition.OCTAVE_UP ? GuitarEvent.OCTAVE_MODIFIER : 0);
            setPitchBend(event.armPosition == GuitarEvent.ArmPosition.SLIDE ? event.pitchBend : GuitarEvent.PITCH_BEND_DEFAULT);
            playNote((byte)midiNumber);
            visualizeModifier(event.armPosition);
        } else if (oldEvent != null && !event.equals(oldEvent)) {
            stopNote((byte) oldEvent.note.midiNumber);
        }
        oldEvent = event;
    }

    /* -------------------------------------------------------------------
     * UTILITY
     * ------------------------------------------------------------------- */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Log.d(this.getClass().getName(), "Motion event: " + event);


        return false;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            processNewGuitarEvent(text);
        }
    };
}