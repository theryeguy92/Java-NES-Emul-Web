package gui.inputs;

/**
 * Key or Gamepad Button and contains its internal ID
 */
public class KeyTuple {

    public final int keyID;
    public final String keyName;

    /**
     * Create a new KeyTuple
     *
     * @param keyID the key ID
     * @param keyName the key name
     */
    KeyTuple(int keyID, String keyName) {
        this.keyID = keyID;
        this.keyName = keyName;
    }
}
