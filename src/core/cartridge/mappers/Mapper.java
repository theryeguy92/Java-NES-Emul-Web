package core.cartridge.mappers;

import core.ppu.Mirror;
import utils.IntegerWrapper;

public abstract class Mapper {

    final int nb_PRG_banks;  // Number of Program ROM Banks
    final int nb_CHR_banks;  // Number of Character ROM Banks

    /**
     * @param nPRGBanks number of Program ROM Banks
     * @param nCHRBanks number of Character ROM Banks
     */
    Mapper(int nPRGBanks, int nCHRBanks) {
        this.nb_PRG_banks = nPRGBanks;
        this.nb_CHR_banks = nCHRBanks;
    }

    /**
     * @param addr   the CPU Address to map
     * @param mapped the Wrapper where to store the Mapped Address
     * @param data   the data read from the mapped address
     * @return Whether or not the Address was mapped
     */
    public abstract boolean cpuMapRead(int addr, IntegerWrapper mapped, IntegerWrapper data);

    /**
     * @param addr   the CPU Address to map
     * @param mapped the Wrapper where to store the Mapped Address
     * @param data   the data to write
     * @return Whether or not the Address was mapped
     */
    public abstract boolean cpuMapWrite(int addr, IntegerWrapper mapped, int data);

    /**
     * @param addr   the PPU Address to map
     * @param mapped the Wrapper where to store the Mapped Address
     * @param data   the data read from the mapped address
     * @return Whether or not the Address was mapped
     */
    public abstract boolean ppuMapRead(int addr, IntegerWrapper mapped, IntegerWrapper data);

    /**
     * @param addr   the PPU Address to map
     * @param mapped the Wrapper where to store the Mapped Address
     * @param data   the data to write
     * @return if the Address was mapped
     */
    public abstract boolean ppuMapWrite(int addr, IntegerWrapper mapped, int data);

    /**
     * @return the current mirroring mode
     */
    public Mirror mirror() {
        return Mirror.HARDWARE;
    }

    /**
     * @return false if not overridden
     */
    public boolean irqState() { return false; }


    public void irqClear() {}


    public void notifyScanline() {}

    /**
     * @param addr the address the PPU has read from
     */
    public void updateLatch(int addr) {}


    public void reset() {}

    /**
     * @return does the Cartridge has internal RAM, false if not overridden
     */
    public boolean hasRAM() {
        return false;
    }

    /**
     * @return the internal RAM, null if not present
     */
    public byte[] getRAM() {
        return null;
    }
}
