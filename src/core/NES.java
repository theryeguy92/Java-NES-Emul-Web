package core;

import core.apu.APU_2A03;
import core.cartridge.Cartridge;
import core.cpu.CPU_6502;
import core.ppu.PPU_2C02;
import utils.IntegerWrapper;

/**
 * This class represents the Bus of the NES
 * it is the Core of the system and controls everything
 */
public class NES {

    private static final long SAVE_INTERVAL = 20000; // Constant for the save interval

    public final int[] controller; // State variable for controller input states
    public double final_audio_sample = 0.0; // State variable for the final audio sample output

    private final byte[] ram; // Encapsulation: Memory for the system RAM
    private final CPU_6502 cpu; // Encapsulation: CPU component
    private final PPU_2C02 ppu; // Encapsulation: PPU component
    private final APU_2A03 apu; // Encapsulation: APU component
    private final int[] controller_state; // Encapsulation: Current state of controllers

    private long next_save = 0; // Encapsulation: Next time to save state
    private long system_ticks = 0; // Encapsulation: System tick counter
    private Cartridge cartridge; // Encapsulation: The currently inserted cartridge
    private int dma_page = 0x00; // Encapsulation: DMA page for transfer
    private int dma_addr = 0x00; // Encapsulation: DMA address for transfer
    private int dma_data = 0x00; // Encapsulation: DMA data buffer
    private boolean dma_transfer = false; // Encapsulation: DMA transfer state
    private boolean dma_dummy = true; // Encapsulation: DMA dummy cycle state
    private double audio_time = 0.0; // Encapsulation: Time accumulator for audio processing
    private double time_per_NES_cycle = 0.0; // Encapsulation: Time per NES clock cycle
    private double time_per_system_sample = 0.0; // Encapsulation: Time per audio sample
    private boolean sound_rendering = true; // Encapsulation: Sound rendering state
    private int dummy_cycle_left = 0; // Encapsulation: Dummy cycles left for the CPU

    /**
     * Create a new Instance of Bus ready to be started
     */
    public NES() {
        ram = new byte[2048]; // Initialize RAM with 2KB size
        for (int i = 0; i < 2048; i++)
            ram[i] = 0x0000;
        cpu = new CPU_6502(); // Initialize the CPU
        ppu = new PPU_2C02(); // Initialize the PPU
        apu = new APU_2A03(this); // Initialize the APU
        controller = new int[2]; // Initialize controller array
        controller_state = new int[2]; // Initialize controller state array
        cpu.connectBus(this); // Mediator Pattern: Connect CPU with the NES bus
    }

    public void setSampleFreq(int sampleRate) {
        time_per_system_sample = 1.0 / (double) sampleRate; // Calculate time per system sample
        time_per_NES_cycle = 1.0 / 5369318.0; // Set time per NES cycle based on system clock rate
    }

    /**
     * Return a Pointer to the CPU instance
     *
     * @return the CPU
     */
    public CPU_6502 getCpu() {
        return cpu; // Accessor for the CPU instance
    }

    /**
     * Return a Pointer to the PPU instance
     *
     * @return the PPU
     */
    public PPU_2C02 getPpu() {
        return ppu; // Accessor for the PPU instance
    }

    public APU_2A03 getApu() {
        return apu; // Accessor for the APU instance
    }

    /**
     * Write a value to the CPU Addressable range
     *
     * @param addr the Address to write to
     * @param data the data to write
     */
    public void cpuWrite(int addr, int data) {
        data &= 0xFF; // Ensure data is 8-bit
        addr &= 0xFFFF; // Ensure address is 16-bit
        if (!cartridge.cpuWrite(addr, data)) { // Bridge Pattern: Cartridge handles its own CPU writes
            if (addr <= 0x1FFF) { // Write to RAM
                ram[addr & 0x07FF] = (byte) data; // RAM mirroring
            } else if (addr <= 0x3FFF) { // Write to PPU Register
                ppu.cpuWrite(addr & 0x0007, data); // PPU register write handling
            } else if (addr <= 0x4013 || addr == 0x4015 || addr == 0x4017) { // Write to APU
                apu.cpuWrite(addr, data); // APU register write handling
            } else if (addr == 0x4014) { // DMA transfer initiation
                dma_page = data; // Set DMA page
                dma_addr = 0; // Reset DMA address
                dma_transfer = true; // Start DMA transfer
            } else if (addr == 0x4016) { // Controller state snapshot
                controller_state[data & 0x1] = controller[data & 0x1]; // Update controller state
            }
        }
    }

    /**
     * Read a value from the CPU Addressable range
     *
     * @param addr     the Address to read from
     * @param readOnly is the reading action allowed to alter CPU/PPU state
     * @return the read value
     */
    public int cpuRead(int addr, boolean readOnly) {
        addr &= 0xFFFF; // Ensure address is 16-bit
        IntegerWrapper data = new IntegerWrapper(); // Wrapper for the read data
        if (!cartridge.cpuRead(addr, data)) { // Bridge Pattern: Cartridge handles its own CPU reads
            if (addr <= 0x1FFF) // Read from RAM
                data.value = ram[addr & 0x07FF]; // RAM mirroring
            else if (addr <= 0x3FFF) // Read from PPU Register
                data.value = ppu.cpuRead(addr & 0x0007, readOnly); // PPU register read handling
            else if (addr == 0x4015)
                data.value = apu.cpuRead(addr, readOnly); // APU register read handling
            else if (addr >= 0x4016 && addr <= 0x4017 && !readOnly) { // Read controllers
                data.value = ((controller_state[addr & 0x0001] & 0x80) > 0) ? 0x1 : 0x0; // Serial controller read
                controller_state[addr & 0x0001] <<= 1; // Shift controller state
            }
        }
        return data.value & 0xFF; // Return the read value
    }

    /**
     * Load a Cartridge into the console and link it to the PPU
     *
     * @param cart the Cartridge to load
     */
    public void insertCartridge(Cartridge cart) {
        this.cartridge = cart; // Set the cartridge
        ppu.connectCartridge(cartridge); // Bridge Pattern: Link cartridge to PPU
    }

    /**
     * Reset the console by resetting the CPU, the PPU and set the systemTicks to 0
     */
    public void reset() {
        cpu.reset(); // Reset the CPU
        ppu.reset(); // Reset the PPU
        cartridge.reset(); // Reset the cartridge
        system_ticks = 0; // Reset system tick counter
        dma_page = 0x00; // Reset DMA state
        dma_addr = 0x00; // Reset DMA state
        dma_data = 0x00; // Reset DMA state
        dma_dummy = true; // Reset DMA dummy state
        dma_transfer = false; // Reset DMA transfer state
    }

    /**
     * Set the console to its default boot up state
     */
    public void startup() {
        cpu.startup(); // Startup the CPU
        apu.startup(); // Startup the APU
        ppu.reset(); // Reset the PPU
        cartridge.reset(); // Reset the cartridge
        system_ticks = 0; // Reset system tick counter
    }

    /**
     * Compute one console tick
     * the PPU is clocked every times
     * the CPU is clocked one every 3 times
     */
    public boolean clock(boolean update_apu_visual) {
        ppu.clock(); // Clock the PPU every tick
        apu.clock(sound_rendering, time_per_NES_cycle); // Clock the APU every tick
        if (system_ticks % 3 == 0) { // Clock the CPU every 3 ticks
            if (dma_transfer) {
                if (dma_dummy) { // Handle DMA dummy cycle
                    if (system_ticks % 2 == 1)
                        dma_dummy = false;
                } else { // Handle DMA transfer cycle
                    if (system_ticks % 2 == 0) // Read from memory on even cycles
                        dma_data = cpuRead(dma_page << 8 | dma_addr, false);
                    else { // Write to PPU memory on odd cycles
                        switch ((dma_addr) & 0x03) {
                            case 0x0:
                                ppu.getOams()[dma_addr >> 2].setY(dma_data);
                            case 0x1:
                                ppu.getOams()[dma_addr >> 2].setId(dma_data);
                            case 0x2:
                                ppu.getOams()[dma_addr >> 2].setAttribute(dma_data);
                            case 0x3:
                                ppu.getOams()[dma_addr >> 2].setX(dma_data);
                        }
                        dma_addr++;
                        dma_addr &= 0xFF;
                        if (dma_addr == 0x00) { // End of DMA transfer
                            dma_transfer = false;
                            dma_dummy = true;
                        }
                    }
                }
            } else if (dummy_cycle_left == 0)
                cpu.clock(); // Clock the CPU
            else
                dummy_cycle_left--;
        }

        boolean audioSampleReady = false;
        audio_time += time_per_NES_cycle; // Accumulate time for audio sampling
        if (audio_time >= time_per_system_sample) { // Check if it's time for an audio sample
            audio_time -= time_per_system_sample; // Reset audio time
            if (sound_rendering)
                final_audio_sample = apu.getSample(update_apu_visual); // Get audio sample from APU
            else
                final_audio_sample = 0;
            audioSampleReady = true;
        }

        if (ppu.nmi())
            cpu.nmi(); // Handle PPU NMI interrupt
        if (apu.irq())
            cpu.irq(); // Handle APU IRQ interrupt
        if (cartridge.getMapper().irqState()) {
            cartridge.getMapper().irqClear();
            cpu.irq(); // Handle Cartridge IRQ interrupt
        }

        if (System.currentTimeMillis() >= next_save) { // Save the state if necessary
            cartridge.save(); // Save the cartridge state
            next_save = System.currentTimeMillis() + SAVE_INTERVAL; // Update the next save time
        }
        system_ticks++; // Increment system ticks

        return audioSampleReady; // Return whether an audio sample is ready
    }

    /**
     * Reference to the currently inserted "Cartridge"
     *
     * @return the currently inserted Cartridge
     */
    public Cartridge getCartridge() {
        return cartridge; // Accessor for the cartridge
    }

    /**
     * @param enabled should the audio rendering be enabled
     */
    public void enableSoundRendering(boolean enabled) {
        this.sound_rendering = enabled; // Enable or disable sound rendering
    }

    /**
     * @param raw should RAW Audio be triggered or not
     */
    public void toggleRawAudio(boolean raw) {
        apu.enabledRawMode(raw); // Toggle RAW audio mode in APU
    }

/*
    public void toggleRawAudio(boolean raw) {
        apu.enableRawMode(raw); // Toggle RAW audio mode in APU
    }
*/

    /**
     * @param cycles the number of cycles the CPU should be halted
     */
    public void haltCPU(int cycles) {
        dummy_cycle_left = cycles; // Set the number of dummy cycles for the CPU
    }

    /**
     * @return is sound rendering enabled
     */
    public boolean isSoundRenderingEnabled() {
        return sound_rendering; // Accessor for sound rendering state
    }
}
