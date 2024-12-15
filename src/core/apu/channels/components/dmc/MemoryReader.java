package core.apu.channels.components.dmc;

import core.NES;

/**
 * This class represents the interface between the DMC Channel and the RAM
 */
public class MemoryReader {

    // Dependency Injection: Injecting NES instance into MemoryReader
    private final NES bus;

    private int currentAddress = 0x00;
    private int bytesRemaining = 0x00;

    /**
     * Create a new MemoryReader connected to a NES
     *
     * @param bus the NES to read from
     */
    public MemoryReader(NES bus) {
        this.bus = bus;
    }

    /**
     * Get the next sample
     *
     * @return the next sample of the sequence, 0 if finished
     */
    public int getSample() {
        if (bytesRemaining > 0) {
            bytesRemaining--;
            int sample = bus.cpuRead(currentAddress, false);
            // Update the current address within the 0x8000 - 0x8FFF range
            currentAddress = ((currentAddress + 1) & 0x0FFF) | 0x8000;
            return sample;
        }
        return 0;
    }

    // Encapsulation: Providing public methods to access and modify private fields
    public int getCurrentAddress() {
        return currentAddress;
    }

    public void setCurrentAddress(int currentAddress) {
        this.currentAddress = currentAddress;
    }

    public int getBytesRemaining() {
        return bytesRemaining;
    }

    public void setBytesRemaining(int bytesRemaining) {
        this.bytesRemaining = bytesRemaining;
    }
}

























/*
package core.apu.channels.components.dmc;

import core.NES;

*/
/**
 * This class represents the interface between the DMC Channel and the RAM
 *//*

public class MemoryReader {

    // Dependency Injection: Injecting NES instance into MemoryReader
    private final NES bus;

    public int current_address = 0x00;
    public int bytes_remaining = 0x00;

    */
/**
     * Create a new MemoryReader connected to a NES
     *
     * @param bus the NES to read from
     *//*

    public MemoryReader(NES bus) {
        this.bus = bus;
    }

    */
/**
     * Get the next sample
     *
     * @return the next sample of the sequence, 0 if finished
     *//*

    public int getSample() {
        if (bytes_remaining > 0) {
            bytes_remaining--;
            int sample = bus.cpuRead(current_address, false);
            // Update the current address within the 0x8000 - 0x8FFF range
            current_address = ((current_address + 1) & 0x0FFF) | 0x8000;
            return sample;
        }
        return 0;
    }

}
*/
