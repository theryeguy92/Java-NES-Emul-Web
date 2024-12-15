package core;

import gui.lwjgui.NEmuSContext;
import gui.lwjgui.windows.APUViewer;
import gui.lwjgui.windows.AudioOutput;
import gui.lwjgui.windows.AudioSettings;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.ugens.Function;
import net.beadsproject.beads.ugens.WaveShaper;
import utils.Dialogs;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;

public class AudioEngine {

    private final AudioContext ac; // Encapsulation: Manages audio processing context
    private final JavaSoundAudioIO jsaIO; // Encapsulation: Handles audio I/O interface
    private final List<AudioOutput> validOutputs; // Encapsulation: List of valid audio outputs
    private AudioOutput selectedOutput; // Encapsulation: Currently selected audio output

    public AudioEngine(NEmuSContext emulatorContext) {
        jsaIO = new JavaSoundAudioIO(); // Encapsulation: Initialize audio I/O
        validOutputs = new ArrayList<>(); // Encapsulation: Initialize list of valid outputs

        verifyValidOutputs(); // Factory Method Pattern: Finds and adds valid audio outputs
        if (validOutputs.size() == 0)
            Dialogs.showError("No suitable Audio Output found", "NEmuS was unable to find a suitable Audio Output, and therefore can't run properly :(");

        selectIO(validOutputs.get(0)); // Strategy Pattern: Selects the first valid output
        AudioSettings.linkSoundIO(this); // Observer Pattern: Link the audio settings with the audio engine

        ac = new AudioContext(jsaIO); // Encapsulation: Initialize the audio context
        emulatorContext.nes.setSampleFreq((int) ac.getSampleRate()); // Bridge Pattern: Connect NES context with audio settings

        Function audioProcessor = new Function(new WaveShaper(ac)) {
            public float calculate() {
                if (emulatorContext.emulation_running) {
                    boolean sample_ready = false;
                    while (!sample_ready)
                        sample_ready = emulatorContext.nes.clock(APUViewer.hasInstance());
                }
                return emulatorContext.emulation_running ? (float) emulatorContext.nes.final_audio_sample : 0;
            }
        };
        ac.out.addInput(audioProcessor); // Decorator Pattern: Add functionality to audio context output
        ac.start(); // Start audio processing
    }

    private void verifyValidOutputs() {
        AudioContext checker = new AudioContext(jsaIO); // Encapsulation: Temporary audio context for checking

        int index = 0;
        // Factory Method Pattern: For each Mixer, check if it's a valid output and add it to the list
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                jsaIO.selectMixer(index); // Strategy Pattern: Set the mixer based on index
                checker = new AudioContext(jsaIO);
                final boolean[] valid = {false};
                // Anonymous Inner Class: Function to check if the mixer is valid
                Function check = new Function(new WaveShaper(checker)) {
                    public float calculate() {
                        valid[0] = true;
                        return 0;
                    }
                };
                checker.out.addInput(check);
                checker.start();

                Thread.sleep(200);

                if (valid[0])
                    validOutputs.add(new AudioOutput(index, info)); // Add valid output to the list
                checker.stop();
            } catch (InterruptedException ignored) {}
            index++;
        }
    }

    public List<AudioOutput> getValidOutputs() {
        return validOutputs; // Encapsulation: Provides access to the list of valid outputs
    }

    public void start() {
        ac.start(); // Facade Pattern: Simplified method to start audio processing
    }

    public void stop() {
        ac.stop(); // Facade Pattern: Simplified method to stop audio processing
    }

    public AudioOutput getSelectedOutput() {
        return selectedOutput; // Encapsulation: Accessor for the selected output
    }

    public void selectIO(AudioOutput io) {
        selectedOutput = io; // Encapsulation: Set the selected audio output
        jsaIO.selectMixer(io.getId()); // Strategy Pattern: Select the mixer based on the chosen output
    }
}
