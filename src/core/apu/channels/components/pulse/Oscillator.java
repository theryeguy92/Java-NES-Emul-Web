package core.apu.channels.components.pulse;

/**
 * This class represents an Oscillator and is used to generate a Square Wave of a known frequency, amplitude and duty cycle
 */
public class Oscillator {

    // Singleton pattern applied to harmonics management
    private static HarmonicsManager harmonicsManager = HarmonicsManager.getInstance();

    public float frequency = 0;
    public float duty_cycle = 0;
    public float amplitude = 1;

    /**
     * Set the number of harmonics of the Oscillator
     *
     * @param harmonics the number of harmonics to set
     */
    public static void setHarmonics(int harmonics) {
        harmonicsManager.setHarmonics(harmonics);
    }

    /**
     * Return the number of harmonics of the Oscillator
     *
     * @return the number of harmonics of the Oscillator
     */
    public static int getHarmonics() {
        return harmonicsManager.getHarmonics();
    }

    /**
     * Get the sample at time t
     *
     * @param t the time to sample from
     * @return the sampled value
     */
    public float sample(double t) {
        float a = 0;
        float b = 0;
        float p = duty_cycle * 6.2918530f;
        int harmonics = harmonicsManager.getHarmonics();

        for (float n = 1; n < harmonics; n++) {
            float c = (float) (n * frequency * 6.2918530f * t);
            a -= sin(c) / n;
            b -= sin(c - p * n) / n;
        }
        return (amplitude / 3.14159265f) * (a - b) + amplitude * (1 - duty_cycle);
    }

    /**
     * A faster method of approximating a sinus
     *
     * @param t the value to calculate the sinus of
     * @return the sinus of t
     */
    private float sin(float t) {
        float j = t * 0.15915f;
        j = j - (int) j;
        return 20.785f * j * (j - 0.5f) * (j - 1.0f);
    }
}

/**
 * Singleton class managing the harmonics setting for all Oscillators
 */
class HarmonicsManager {
    private static HarmonicsManager instance;
    private int harmonics = 10;

    private HarmonicsManager() {
    }

    public static HarmonicsManager getInstance() {
        if (instance == null) {
            instance = new HarmonicsManager();
        }
        return instance;
    }

    public void setHarmonics(int harmonics) {
        this.harmonics = harmonics;
    }

    public int getHarmonics() {
        return harmonics;
    }
}
