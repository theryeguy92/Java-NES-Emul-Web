package core.ppu;

import core.cartridge.Cartridge;
import core.ppu.registers.*;
import gui.lwjgui.windows.Tile;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.lwjgl.BufferUtils;
import utils.IntegerWrapper;
import utils.NumberUtils;

import java.nio.ByteBuffer;

/**
 * This class represent the PPU of the NES
 * Here we are handling all the graphics
 */
public class PPU_2C02 {

    public static final int SCREEN_WIDTH = 256;
    public static final int SCREEN_HEIGHT = 240;

    private final Color[] system_palette;
    private final ByteBuffer screen_buffer;
    private final ByteBuffer screen_buffer_tmp;

    private final byte[][] nametable_memory;
    private final byte[] palette_memory;
    private final byte[][] patterntable_memory;

    private final MaskRegister mask_register;
    private final ControlRegister control_register;
    private final StatusRegister status_register;
    private final LoopyRegister vram_addr;
    private final LoopyRegister tram_addr;

    private final ObjectAttribute[] oams;
    private final ObjectAttribute[] visible_oams;

    private final int[] sprite_shift_pattern_low;
    private final int[] sprite_shift_pattern_high;

    public boolean frame_complete;
    private Cartridge cartridge;

    private int sprite_count;
    private int address_latch = 0x00;
    private int ppu_data_buffer = 0x00;
    private int oam_addr = 0x00;
    private int fine_x = 0x00;

    private int bg_next_tile_id = 0x00;
    private int bg_next_tile_attrib = 0x00;
    private int bg_next_tile_lsb = 0x00;
    private int bg_next_tile_msb = 0x00;

    private int bg_shift_pattern_low = 0x0000;
    private int bg_shift_pattern_high = 0x0000;
    private int bg_shift_attrib_low = 0x0000;
    private int bg_shift_attrib_high = 0x0000;

    private boolean spriteZeroHitPossible = false;
    private boolean spriteZeroBeingRendered = false;

    private int scanline;
    private int cycle;
    private boolean odd_frame = false;
    private boolean nmi;

    /**
     * Create a new PPU, instantiate its components and fill up the palettes
     */
    public PPU_2C02() {
        nametable_memory = new byte[2][1024];
        patterntable_memory = new byte[2][4096];
        palette_memory = new byte[32];
        system_palette = new Color[0x40];
        screen_buffer = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        screen_buffer_tmp = BufferUtils.createByteBuffer(SCREEN_HEIGHT * SCREEN_WIDTH * 4);
        frame_complete = false;
        scanline = 0;
        cycle = 0;
        mask_register = new MaskRegister();
        control_register = new ControlRegister();
        status_register = new StatusRegister();
        vram_addr = new LoopyRegister();
        tram_addr = new LoopyRegister();
        oams = new ObjectAttribute[64];
        for (int i = 0; i < oams.length; i++)
            oams[i] = new ObjectAttribute();
        visible_oams = new ObjectAttribute[8];
        for (int i = 0; i < visible_oams.length; i++)
            visible_oams[i] = new ObjectAttribute();
        sprite_shift_pattern_low = new int[8];
        sprite_shift_pattern_high = new int[8];

        // Here is the palette table

        system_palette[0x00] = new Color(84 / 255.0, 84 / 255.0, 84 / 255.0, 1);
        system_palette[0x01] = new Color(0 / 255.0, 30 / 255.0, 116 / 255.0, 1);
        system_palette[0x02] = new Color(8 / 255.0, 16 / 255.0, 144 / 255.0, 1);
        system_palette[0x03] = new Color(48 / 255.0, 0 / 255.0, 136 / 255.0, 1);
        system_palette[0x04] = new Color(68 / 255.0, 0 / 255.0, 100 / 255.0, 1);
        system_palette[0x05] = new Color(92 / 255.0, 0 / 255.0, 48 / 255.0, 1);
        system_palette[0x06] = new Color(84 / 255.0, 4 / 255.0, 0 / 255.0, 1);
        system_palette[0x07] = new Color(60 / 255.0, 24 / 255.0, 0 / 255.0, 1);
        system_palette[0x08] = new Color(32 / 255.0, 42 / 255.0, 0 / 255.0, 1);
        system_palette[0x09] = new Color(8 / 255.0, 58 / 255.0, 0 / 255.0, 1);
        system_palette[0x0A] = new Color(0 / 255.0, 64 / 255.0, 0 / 255.0, 1);
        system_palette[0x0B] = new Color(0 / 255.0, 60 / 255.0, 0 / 255.0, 1);
        system_palette[0x0C] = new Color(0 / 255.0, 50 / 255.0, 60 / 255.0, 1);
        system_palette[0x0D] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x0E] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x0F] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x10] = new Color(152 / 255.0, 150 / 255.0, 152 / 255.0, 1);
        system_palette[0x11] = new Color(8 / 255.0, 76 / 255.0, 196 / 255.0, 1);
        system_palette[0x12] = new Color(48 / 255.0, 50 / 255.0, 236 / 255.0, 1);
        system_palette[0x13] = new Color(92 / 255.0, 30 / 255.0, 228 / 255.0, 1);
        system_palette[0x14] = new Color(136 / 255.0, 20 / 255.0, 176 / 255.0, 1);
        system_palette[0x15] = new Color(160 / 255.0, 20 / 255.0, 100 / 255.0, 1);
        system_palette[0x16] = new Color(152 / 255.0, 34 / 255.0, 32 / 255.0, 1);
        system_palette[0x17] = new Color(120 / 255.0, 60 / 255.0, 0 / 255.0, 1);
        system_palette[0x18] = new Color(84 / 255.0, 90 / 255.0, 0 / 255.0, 1);
        system_palette[0x19] = new Color(40 / 255.0, 114 / 255.0, 0 / 255.0, 1);
        system_palette[0x1A] = new Color(8 / 255.0, 124 / 255.0, 0 / 255.0, 1);
        system_palette[0x1B] = new Color(0 / 255.0, 118 / 255.0, 40 / 255.0, 1);
        system_palette[0x1C] = new Color(0 / 255.0, 102 / 255.0, 120 / 255.0, 1);
        system_palette[0x1D] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x1E] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x1F] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x20] = new Color(236 / 255.0, 238 / 255.0, 236 / 255.0, 1);
        system_palette[0x21] = new Color(76 / 255.0, 154 / 255.0, 236 / 255.0, 1);
        system_palette[0x22] = new Color(120 / 255.0, 124 / 255.0, 236 / 255.0, 1);
        system_palette[0x23] = new Color(176 / 255.0, 98 / 255.0, 236 / 255.0, 1);
        system_palette[0x24] = new Color(228 / 255.0, 84 / 255.0, 236 / 255.0, 1);
        system_palette[0x25] = new Color(236 / 255.0, 88 / 255.0, 180 / 255.0, 1);
        system_palette[0x26] = new Color(236 / 255.0, 106 / 255.0, 100 / 255.0, 1);
        system_palette[0x27] = new Color(212 / 255.0, 136 / 255.0, 32 / 255.0, 1);
        system_palette[0x28] = new Color(160 / 255.0, 170 / 255.0, 0 / 255.0, 1);
        system_palette[0x29] = new Color(116 / 255.0, 196 / 255.0, 0 / 255.0, 1);
        system_palette[0x2A] = new Color(76 / 255.0, 208 / 255.0, 32 / 255.0, 1);
        system_palette[0x2B] = new Color(56 / 255.0, 204 / 255.0, 108 / 255.0, 1);
        system_palette[0x2C] = new Color(56 / 255.0, 180 / 255.0, 204 / 255.0, 1);
        system_palette[0x2D] = new Color(60 / 255.0, 60 / 255.0, 60 / 255.0, 1);
        system_palette[0x2E] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x2F] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x30] = new Color(236 / 255.0, 238 / 255.0, 236 / 255.0, 1);
        system_palette[0x31] = new Color(168 / 255.0, 204 / 255.0, 236 / 255.0, 1);
        system_palette[0x32] = new Color(188 / 255.0, 188 / 255.0, 236 / 255.0, 1);
        system_palette[0x33] = new Color(212 / 255.0, 178 / 255.0, 236 / 255.0, 1);
        system_palette[0x34] = new Color(236 / 255.0, 174 / 255.0, 236 / 255.0, 1);
        system_palette[0x35] = new Color(236 / 255.0, 174 / 255.0, 212 / 255.0, 1);
        system_palette[0x36] = new Color(236 / 255.0, 180 / 255.0, 176 / 255.0, 1);
        system_palette[0x37] = new Color(228 / 255.0, 196 / 255.0, 144 / 255.0, 1);
        system_palette[0x38] = new Color(204 / 255.0, 210 / 255.0, 120 / 255.0, 1);
        system_palette[0x39] = new Color(180 / 255.0, 222 / 255.0, 120 / 255.0, 1);
        system_palette[0x3A] = new Color(168 / 255.0, 226 / 255.0, 144 / 255.0, 1);
        system_palette[0x3B] = new Color(152 / 255.0, 226 / 255.0, 180 / 255.0, 1);
        system_palette[0x3C] = new Color(160 / 255.0, 214 / 255.0, 228 / 255.0, 1);
        system_palette[0x3D] = new Color(160 / 255.0, 162 / 255.0, 160 / 255.0, 1);
        system_palette[0x3E] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
        system_palette[0x3F] = new Color(0 / 255.0, 0 / 255.0, 0 / 255.0, 1);
    }

    /**
     * @return a ByteBuffer that can be loaded into a texture to be displayed on the screen
     */
    public ByteBuffer getScreenBuffer() {
        return screen_buffer;
    }

    /**
     * Connect a Cartridge to the CPU
     *
     * @param cartridge the Cartridge to connect
     */
    public void connectCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    /**
     * Called when the CPU wants to read from the PPU Memory
     *
     * @param addr     the address to read from (8 locations mirrored through the addressable range)
     * @param readOnly is the access allowed to alter the PPU state
     * @return the read data as an 8bit unsigned value
     */
    public int cpuRead(int addr, boolean readOnly) {
        int data = 0x00;
        if (readOnly) {
            //If in read only, don't alter the PPU state
            switch (addr) {
                case 0x0000: // Control
                    data = control_register.get();
                    break;
                case 0x0001: // Mask
                    data = mask_register.get();
                    break;
                case 0x0002: // Status
                    data = status_register.get();
                    break;
                case 0x0003: // OAM Address
                    break;
                case 0x0004: // OAM Data
                    data = getOamData();
                    break;
                case 0x0005: // Scroll
                    break;
                case 0x0006: // PPU Address
                    break;
                case 0x0007: // PPU Data
                    break;
            }
            return data & 0xFF;
        }
        switch (addr) {
            case 0x0000: // Control
                break;
            case 0x0001: // Mask
                data = oam_addr;
                break;
            case 0x0002: // Status
                //When reading the Status Register, the unused bits are filled with le last data that was read
                data = (status_register.get() & 0xF0) | (ppu_data_buffer & 0x1F);
                //The Vertical Blank Flag is reset
                status_register.setVerticalBlank(false);
                //The address_latch is also reset to ensure proper write for the next time
                address_latch = 0;
                break;
            case 0x0003: // OAM Address
                break;
            case 0x0004: // OAM Data
                data = getOamData();
                break;
            case 0x0005: // Scroll
                break;
            case 0x0006: // PPU Address
                break;
            case 0x0007: // PPU Data
                //Nametable reads are delayed by one cycle
                //When reading the last fetched data is returned and the next is fetched
                int last_addr = vram_addr.get();
                data = ppu_data_buffer;
                ppu_data_buffer = ppuRead(vram_addr.get(), false);
                //Except palette, here their is no delay
                if (vram_addr.get() >= 0x3F00) data = ppu_data_buffer;
                //The vram address is incremented (horizontally or vertically depending on the Control Register)
                vram_addr.set(vram_addr.get() + (control_register.isIncrementModeSet() ? 32 : 1));
                if ((vram_addr.get() & 0x1000) == 0x1000 && (last_addr & 0x1000) == 0)
                    cartridge.getMapper().notifyScanline();
                break;
        }
        return data & 0xFF;
    }

    /**
     * Called when the CPU wants to write to the PPU Memory (Registers)
     *
     * @param addr the address to write to (8 locations mirrored through the addressable range)
     * @param data the data to write
     */
    public void cpuWrite(int addr, int data) {
        switch (addr) {
            case 0x0000: // Control
                control_register.set(data);
                //When writing to the Control Register, one of the Loopy Register need to be updated (in case the nametable has changed)
                tram_addr.setNametableX(control_register.isNametableXSet());
                tram_addr.setNametableY(control_register.isNametableYSet());
                break;
            case 0x0001: // Mask
                mask_register.set(data);
                break;
            case 0x0002: // Status
                break;
            case 0x0003: // OAM Address
                oam_addr = data;
                break;
            case 0x0004: // OAM Data
                switch (oam_addr & 0x03) {
                    case 0x0:
                        oams[oam_addr >> 2].setY(data);
                    case 0x1:
                        oams[oam_addr >> 2].setId(data);
                    case 0x2:
                        oams[oam_addr >> 2].setAttribute(data);
                    case 0x3:
                        oams[oam_addr >> 2].setX(data);
                }
                oam_addr++;
                oam_addr &= 0xFF;
                break;
            case 0x0005: // Scroll
                //When writing to the Scroll Register, we first write the X offset
                if (address_latch == 0) {
                    //The offset is spliced into coarseX and fineX
                    fine_x = data & 0x07;
                    tram_addr.setCoarseX(data >> 3);
                    address_latch = 1;
                    //The second write is the Y offset
                } else {
                    //The offset is spliced into coarseY and fineY
                    tram_addr.setFineY(data & 0x07);
                    tram_addr.setCoarseY(data >> 3);
                    address_latch = 0;
                }
                break;
            case 0x0006: // PPU Address
                //An address is 16bit, therefor we need 2 write cycle to load a full address
                //The first write is the 8 MSB of the address
                if (address_latch == 0) {
                    tram_addr.set((tram_addr.get() & 0x00FF) | ((data & 0x3F) << 8));
                    address_latch = 1;
                    //The second write is the 8 LSB of the address
                } else {
                    tram_addr.set((tram_addr.get() & 0xFF00) | data);
                    //When the address has been fully fetched, it is store into the main Loopy Register
                    vram_addr.set(tram_addr.get());
                    address_latch = 0;
                }
                break;
            case 0x0007: // PPU Data
                //The data is written to the VRAM address
                ppuWrite(vram_addr.get(), data);
                //The vram address is incremented (horizontally or vertically depending on the Control Register)
                vram_addr.set(vram_addr.get() + (control_register.isIncrementModeSet() ? 32 : 1));
                break;
        }
    }

    /**
     * @param addr the address to read from
     * @return the read data
     */
    private int ppuRead(int addr, boolean readOnly) {
        addr &= 0x3FFF;
        //A Wrapper used to store the data gathered by the Cartridge
        IntegerWrapper data = new IntegerWrapper();
        //If the address is mapped by the cartridge, let it handle and return read value
        if (!cartridge.ppuRead(addr, data)) {
            if (addr <= 0x1FFF) { //Read from pattern table
                data.value = patterntable_memory[(addr & 0x1000) >> 12][addr & 0x0FFF];
            } else if (addr <= 0x3EFF) { //Read from nametable
                addr &= 0x0FFF;
                if (cartridge.getMirror() == Mirror.VERTICAL) {
                    if (addr <= 0x03FF)
                        data.value = nametable_memory[0][addr & 0x03FF];
                    if (addr >= 0x0400 && addr <= 0x07FF)
                        data.value = nametable_memory[1][addr & 0x03FF];
                    if (addr >= 0x0800 && addr <= 0x0BFF)
                        data.value = nametable_memory[0][addr & 0x03FF];
                    if (addr >= 0x0C00)
                        data.value = nametable_memory[1][addr & 0x03FF];
                } else if (cartridge.getMirror() == Mirror.HORIZONTAL) {
                    if (addr <= 0x03FF)
                        data.value = nametable_memory[0][addr & 0x03FF];
                    if (addr >= 0x0400 && addr <= 0x07FF)
                        data.value = nametable_memory[0][addr & 0x03FF];
                    if (addr >= 0x0800 && addr <= 0x0BFF)
                        data.value = nametable_memory[1][addr & 0x03FF];
                    if (addr >= 0x0C00)
                        data.value = nametable_memory[1][addr & 0x03FF];
                }
            } else { //Read from palette memory
                addr &= 0x1F;
                if (addr == 0x0010) addr = 0x0000;
                if (addr == 0x0014) addr = 0x0004;
                if (addr == 0x0018) addr = 0x0008;
                if (addr == 0x001C) addr = 0x000C;
                data.value = palette_memory[addr] & (mask_register.isGrayscaleSet() ? 0x30 : 0x3F);
            }
        }
        if (!readOnly)
            cartridge.getMapper().updateLatch(addr);
        return data.value & 0xFF;
    }

    /**
     * Called when the PPU wants to write to Memory
     *
     * @param addr the address to write to
     * @param data the data to write
     */
    private void ppuWrite(int addr, int data) {
        addr &= 0x3FFF;
        data &= 0xFF;
        //If the address is mapped by the cartridge, let it handle and return
        if (!cartridge.ppuWrite(addr, data)) {
            if (addr <= 0x1FFF) { //Write to pattern table
                patterntable_memory[(addr & 0x1000) >> 12][addr & 0x0FFF] = (byte) data;

            } else if (addr <= 0x3EFF) { //Write to nametable
                addr &= 0x0FFF;
                if (cartridge.getMirror() == Mirror.VERTICAL) {
                    if (addr <= 0x03FF)
                        nametable_memory[0][addr & 0x03FF] = (byte) data;
                    if (addr >= 0x0400 && addr <= 0x07FF)
                        nametable_memory[1][addr & 0x03FF] = (byte) data;
                    if (addr >= 0x0800 && addr <= 0x0BFF)
                        nametable_memory[0][addr & 0x03FF] = (byte) data;
                    if (addr >= 0x0C00)
                        nametable_memory[1][addr & 0x03FF] = (byte) data;
                } else if (cartridge.getMirror() == Mirror.HORIZONTAL) {
                    if (addr <= 0x03FF)
                        nametable_memory[0][addr & 0x03FF] = (byte) data;
                    if (addr >= 0x0400 && addr <= 0x07FF)
                        nametable_memory[0][addr & 0x03FF] = (byte) data;
                    if (addr >= 0x0800 && addr <= 0x0BFF)
                        nametable_memory[1][addr & 0x03FF] = (byte) data;
                    if (addr >= 0x0C00)
                        nametable_memory[1][addr & 0x03FF] = (byte) data;
                }
            } else { //Writting to palette memory
                addr &= 0x001F;
                if (addr == 0x0010) addr = 0x0000;
                if (addr == 0x0014) addr = 0x0004;
                if (addr == 0x0018) addr = 0x0008;
                if (addr == 0x001C) addr = 0x000C;
                palette_memory[addr] = (byte) data;
            }
        }
    }

    /**
     * @return an 8bit unsigned value pointed by the current OAM address
     */
    private int getOamData() {
        return switch (oam_addr & 0x03) {
            case 0x0 -> oams[oam_addr >> 2].getY() & 0xFF;
            case 0x1 -> oams[oam_addr >> 2].getId() & 0xFF;
            case 0x2 -> oams[oam_addr >> 2].getAttribute() & 0xFF;
            case 0x3 -> oams[oam_addr >> 2].getX() & 0xFF;
            default -> 0x00;
        };
    }

    /**
     * @param paletteId the palette ID
     * @param pixel     the pixel ID
     * @return the corresponding Color
     */
    public Color getColorFromPalette(int paletteId, int pixel) {
        return system_palette[ppuRead(0x3F00 + ((paletteId << 2) & 0x00FF) + (pixel & 0x00FF), false)];
    }

    /**
     * @return do we have to fire a NMI
     */
    public boolean nmi() {
        //If we fire a NMI, the flag is reset to avoid multiple NMI in chain
        if (nmi) {
            nmi = false;
            return true;
        }
        return false;
    }

    /**
     * Reset the PPU to its default state
     */
    public void reset() {
        fine_x = 0x00;
        address_latch = 0x00;
        ppu_data_buffer = 0x00;
        scanline = 0;
        cycle = 0;
        bg_next_tile_id = 0x00;
        bg_next_tile_attrib = 0x00;
        bg_next_tile_lsb = 0x00;
        bg_next_tile_msb = 0x00;
        bg_shift_pattern_low = 0x0000;
        bg_shift_pattern_high = 0x0000;
        bg_shift_attrib_low = 0x0000;
        bg_shift_attrib_high = 0x0000;
        status_register.set(0xA0);
        mask_register.set(0x00);
        control_register.set(0x00);
        vram_addr.set(0x0000);
        tram_addr.set(0x0000);
        screen_buffer_tmp.clear();
    }


    /**
     * We need to impliment a clock. Here we will impliment a state pattern to indicate one 'tick' of the PPU clock.
     */
    public void clock() {
        //If we are in the visible screen (regarding scanlines)
        if (scanline >= -1 && scanline < 240) {
            if (cycle >= 257 && cycle <= 320)
                oam_addr = 0;
            if (scanline == -1 && cycle == 0)
                screen_buffer_tmp.clear();
            //If we are on the top left, we will count the cycle and clear the screen for buffering
            if (scanline == 0 && cycle == 0 && odd_frame && (mask_register.isRenderBackgroundSet() || mask_register.isRenderSpritesSet())) {
                cycle = 1;
            }
            //If we are before the first scanline, we reset the Status Register and Shift Registers
            if (scanline == -1 && cycle == 1) {
                status_register.setVerticalBlank(false);
                status_register.setSpriteOverflow(false);
                status_register.setSpriteZeroHit(false);
                for (int i = 0; i < 8; i++) {
                    sprite_shift_pattern_low[i] = 0x00;
                    sprite_shift_pattern_high[i] = 0x00;
                }
            }
            //In the event we need to compute the color
            if ((cycle >= 2 && cycle < 258) || (cycle >= 321 && cycle < 338)) {

                if (mask_register.isRenderBackgroundSet()) {
                    bg_shift_pattern_low = (bg_shift_pattern_low << 1) & 0xFFFF;
                    bg_shift_pattern_high = (bg_shift_pattern_high << 1) & 0xFFFF;
                    bg_shift_attrib_low = (bg_shift_attrib_low << 1) & 0xFFFF;
                    bg_shift_attrib_high = (bg_shift_attrib_high << 1) & 0xFFFF;
                }
                if (mask_register.isRenderSpritesSet() && cycle >= 1 && cycle < 258) {
                    for (int i = 0; i < sprite_count; i++) {
                        //For all visible sprites, we decrement the position by one until we need to render it.
                        if (visible_oams[i].getX() > 0)
                            visible_oams[i].setX(visible_oams[i].getX() - 1);
                        else {
                            sprite_shift_pattern_low[i] = (sprite_shift_pattern_low[i] << 1) & 0xFF;
                            sprite_shift_pattern_high[i] = (sprite_shift_pattern_high[i] << 1) & 0xFF;
                        }
                    }
                }
                //All of the following action will be executed once and in order for each tile
                //At the beginning of a tile we load the Background Shifters with the previously fetched tile ID and tile attribute
                //We fetch the next tile ID
                //We then fetch the next tile attribute
                //We only keep the 2 lsb of the attribute
                switch ((cycle - 1) % 8) {
                    case 0 -> {
                        bg_shift_pattern_low = ((bg_shift_pattern_low & 0xFF00) | bg_next_tile_lsb) & 0xFFFF;
                        bg_shift_pattern_high = ((bg_shift_pattern_high & 0xFF00) | bg_next_tile_msb) & 0xFFFF;
                        bg_shift_attrib_low = ((bg_shift_attrib_low & 0xFF00) | (((bg_next_tile_attrib & 0b01) == 0b01) ? 0xFF : 0x00)) & 0xFFFF;
                        bg_shift_attrib_high = ((bg_shift_attrib_high & 0xFF00) | (((bg_next_tile_attrib & 0b10) == 0b10) ? 0xFF : 0x00)) & 0xFFFF;
                        bg_next_tile_id = ppuRead(0x2000 | (vram_addr.get() & 0x0FFF), false);
                    }
                    case 2 -> {
                        bg_next_tile_attrib = ppuRead(0x23C0 | (vram_addr.isNametableYSet() ? 0x1 << 11 : 0x0) | (vram_addr.isNametableXSet() ? 0x1 << 10 : 0x0) | ((vram_addr.getCoarseY() >> 2) << 3) | (vram_addr.getCoarseX() >> 2), false);
                        if ((vram_addr.getCoarseY() & 0x02) == 0x02)
                            bg_next_tile_attrib = (bg_next_tile_attrib >> 4) & 0xFF;
                        if ((vram_addr.getCoarseX() & 0x02) == 0x02)
                            bg_next_tile_attrib = (bg_next_tile_attrib >> 2) & 0xFF;
                        bg_next_tile_attrib &= 0x03;
                    }
                    case 4 -> bg_next_tile_lsb = ppuRead((control_register.isPatternBackgroundSet() ? 0x1 << 12 : 0) + (bg_next_tile_id << 4) + vram_addr.getFineY(), false);
                    case 6 -> bg_next_tile_msb = ppuRead((control_register.isPatternBackgroundSet() ? 0x1 << 12 : 0) + (bg_next_tile_id << 4) + vram_addr.getFineY() + 8, false);
                    case 7 -> { // Increment Scroll X
                        //If we are rendering sprites or background
                        if (mask_register.isRenderBackgroundSet() || mask_register.isRenderSpritesSet()) {
                            //If we cross a nametable boundary we invert the nametableX bit to fetch from the other nametable
                            if (vram_addr.getCoarseX() == 31) {
                                vram_addr.setCoarseX(0);
                                vram_addr.setNametableX(!vram_addr.isNametableXSet());
                                //Or we just continue in the same one
                            } else {
                                vram_addr.setCoarseX(vram_addr.getCoarseX() + 1);
                            }
                        }
                    }
                }
            }
            //If we are at the end of a visible scanline we pass to the next one
            if (cycle == 256) { // Increment Scroll Y axis
                //Are we are rendering sprites or background?
                if (mask_register.isRenderBackgroundSet() || mask_register.isRenderSpritesSet()) {
                    //Are we are still in the same tile row?
                    if (vram_addr.getFineY() < 7) {
                        vram_addr.setFineY(vram_addr.getFineY() + 1);
                        //If we have passed to the next tile row
                    } else {
                        //reset the offset inside the row to 0
                        vram_addr.setFineY(0);
                        //If we are at le last tile row, we skip the OAM and switch to the next nametable
                        if (vram_addr.getCoarseY() == 29) {
                            vram_addr.setCoarseY(0);
                            vram_addr.setNametableY(!vram_addr.isNametableYSet());
                            //Just in case we've gone behond the nametable
                        } else if (vram_addr.getCoarseY() == 31) {
                            vram_addr.setCoarseY(0);
                            //Or we simply switch to the next tile row
                        } else {
                            vram_addr.setCoarseY(vram_addr.getCoarseY() + 1);
                        }
                    }
                }
            }
            //If we are at the first pixel of the horizontal blank we reset the X coordinates to the start of a line
            if (cycle == 257) {
                bg_shift_pattern_low = ((bg_shift_pattern_low & 0xFF00) | bg_next_tile_lsb) & 0xFFFF;
                bg_shift_pattern_high = ((bg_shift_pattern_high & 0xFF00) | bg_next_tile_msb) & 0xFFFF;
                bg_shift_attrib_low = ((bg_shift_attrib_low & 0xFF00) | (((bg_next_tile_attrib & 0b01) == 0b01) ? 0xFF : 0x00)) & 0xFFFF;
                bg_shift_attrib_high = ((bg_shift_attrib_high & 0xFF00) | (((bg_next_tile_attrib & 0b10) == 0b10) ? 0xFF : 0x00)) & 0xFFFF;
                if (mask_register.isRenderBackgroundSet() || mask_register.isRenderSpritesSet()) {
                    vram_addr.setNametableX(tram_addr.isNametableXSet());
                    vram_addr.setCoarseX(tram_addr.getCoarseX());
                }
            }

            if (cycle == 338 || cycle == 340) {
                bg_next_tile_id = ppuRead(0x2000 | (vram_addr.get() & 0x0FFF), false);
            }
            //At the start of a new frame we reset the Y coordinates to the top of the screen
            if (scanline == -1 && cycle >= 280 && cycle < 305) {
                if (mask_register.isRenderBackgroundSet() || mask_register.isRenderSpritesSet()) {
                    vram_addr.setNametableY(tram_addr.isNametableYSet());
                    vram_addr.setCoarseY(tram_addr.getCoarseY());
                    vram_addr.setFineY(tram_addr.getFineY());
                }
            }

            //At the end of a scanline, we fetch the sprite that will be visible on the next scanline
            if (cycle == 320 && scanline >= 0) {
                //We clear all visible Object Attribute
                for (ObjectAttribute visible_oam : visible_oams) visible_oam.clear(0xFF);
                //And reset the sprite count
                sprite_count = 0;

                for (int i = 0; i < 8; i++) {
                    sprite_shift_pattern_low[i] = 0x00;
                    sprite_shift_pattern_high[i] = 0x00;
                }

                //Reset the oam entry index and sprite zero hit possible flag
                int oam_entry = 0;
                spriteZeroHitPossible = false;

                //We read all OAM and break if we hit the max number of sprite for one scanline
                while (oam_entry < 64 && sprite_count <= 8) {
                    //We compute if the sprite is in the current scanline
                    int diff = scanline - oams[oam_entry].getY();
                    if (diff >= 0 && diff < (control_register.isSpriteSizeSet() ? 16 : 8)) {
                        //If their is room left for another sprite, we add it to the rendered sprite
                        if (sprite_count < 8) {
                            //If this is the first sprite, a sprite zero hit is possible, we update the flag
                            if (oam_entry == 0) {
                                spriteZeroHitPossible = true;
                            }
                            //Instead of instantiating new OAM, we fill it with the data of the other one
                            visible_oams[sprite_count].set(oams[oam_entry]);
                        }
                        sprite_count++;
                    }
                    oam_entry++;
                }
                //If we hit a 9th sprite on the scanline, we set the sprite overflow flag to 1
                status_register.setSpriteOverflow(sprite_count >= 8);
                if (sprite_count > 8) sprite_count = 8;
            }
            //At the end of the horizontal blank, we fetch all the relevant sprite data for the next scanline
            //This is really done one multiple cycles, but it's easier to do it all. It doesn't change the overall behaviour of the rendering process
            if (cycle == 340) {
                //For each sprite
                for (int i = 0; i < sprite_count; i++) {
                    int sprite_pattern_low, sprite_pattern_high;
                    int sprite_pattern_addr_low, sprite_pattern_addr_high;
                    if (!control_register.isSpriteSizeSet()) { //If the sprites are 8x8
                        if (!((visible_oams[i].getAttribute() & 0x80) == 0x80)) //If the sprite normally oriented
                            sprite_pattern_addr_low = (control_register.isPatternSpriteSet() ? 0x1 << 12 : 0x0) | (visible_oams[i].getId() << 4) | (scanline - visible_oams[i].getY());
                        else //If the sprite is flipped vertically
                            sprite_pattern_addr_low = (control_register.isPatternSpriteSet() ? 0x1 << 12 : 0x0) | (visible_oams[i].getId() << 4) | (7 - (scanline - visible_oams[i].getY()));
                    } else { //If the sprites are 8x16
                        if (!((visible_oams[i].getAttribute() & 0x80) == 0x80)) { //If the sprite normally oriented
                            if (scanline - visible_oams[i].getY() < 8) //Reading top half
                                sprite_pattern_addr_low = ((visible_oams[i].getId() & 0x01) << 12) | ((visible_oams[i].getId() & 0xFE) << 4) | ((scanline - visible_oams[i].getY()) & 0x07);
                            else //Reading bottom half
                                sprite_pattern_addr_low = ((visible_oams[i].getId() & 0x01) << 12) | (((visible_oams[i].getId() & 0xFE) + 1) << 4) | ((scanline - visible_oams[i].getY()) & 0x07);
                        } else {  //If the sprite is flipped vertically
                            if (scanline - visible_oams[i].getY() < 8) //Reading top half
                                sprite_pattern_addr_low = ((visible_oams[i].getId() & 0x01) << 12) | (((visible_oams[i].getId() & 0xFE) + 1) << 4) | (7 - (scanline - visible_oams[i].getY()) & 0x07);
                            else //Reading bottom half
                                sprite_pattern_addr_low = ((visible_oams[i].getId() & 0x01) << 12) | ((visible_oams[i].getId() & 0xFE) << 4) | (7 - (scanline - visible_oams[i].getY()) & 0x07);
                        }
                    }
                    //We compute the complete address and fetch the the sprite's bitplane
                    sprite_pattern_addr_high = (sprite_pattern_addr_low + 8) & 0xFFFF;
                    sprite_pattern_low = ppuRead(sprite_pattern_addr_low, false);
                    sprite_pattern_high = ppuRead(sprite_pattern_addr_high, false);

                    //If the sprite is flipped horizontally, the sprite bitplane are flipped
                    if ((visible_oams[i].getAttribute() & 0x40) == 0x40) {
                        sprite_pattern_low = NumberUtils.byteFlip(sprite_pattern_low);
                        sprite_pattern_high = NumberUtils.byteFlip(sprite_pattern_high);
                    }

                    //We load the sprites to the Shift Registers
                    sprite_shift_pattern_low[i] = sprite_pattern_low;
                    sprite_shift_pattern_high[i] = sprite_pattern_high;
                }
            }
        }

        //If we exit the visible screen, we set the vertical blank flag and eventually fire a Non Maskable Interrupt
        if (scanline >= 241 && scanline < 261) {
            if (scanline == 241 && cycle == 1) {
                status_register.setVerticalBlank(true);
                if (control_register.isEnableNmiSet())
                    nmi = true;
            }
        }

        int bg_pixel = 0x00;
        int bg_palette = 0x00;

        //If background rendering is enabled
        if (mask_register.isRenderBackgroundSet()) {
            //We select the current pixels offset using the scroll information
            if (mask_register.isRenderBackgroundLeftSet() || cycle >= 9) {
                int bit_mux = (0x8000 >> fine_x) & 0xFFFF;
                //We compute the pixel ID by getting the right bit from the 2 shift registers
                int p0_pixel = (bg_shift_pattern_low & bit_mux) > 0 ? 0x1 : 0x0;
                int p1_pixel = (bg_shift_pattern_high & bit_mux) > 0 ? 0x1 : 0x0;
                bg_pixel = ((p1_pixel << 1) | p0_pixel) & 0x0F;
                //Same for the palette ID
                int bg_pal0 = (bg_shift_attrib_low & bit_mux) > 0 ? 0x1 : 0x0;
                int bg_pal1 = (bg_shift_attrib_high & bit_mux) > 0 ? 0x1 : 0x0;
                bg_palette = ((bg_pal1 << 1) | bg_pal0) & 0x0F;
            }
        }

        int fg_pixel = 0x00;
        int fg_palette = 0x00;
        boolean fg_priority = false;

        //If sprite rendering is enabled
        if (mask_register.isRenderSpritesSet()) {
            //The 0th sprite being rendered flag is reset
            if (mask_register.isRenderSpriteLeftSet() || cycle >= 9) {
                spriteZeroBeingRendered = false;
                //For each sprite in order of priority
                for (int i = 0; i < sprite_count; i++) {
                    //If we are at the sprite X location
                    if (visible_oams[i].getX() == 0) {
                        //We get the foreground pixel lsb and msb
                        int fg_pixel_low = (sprite_shift_pattern_low[i] & 0x80) == 0x80 ? 0x1 : 0x0;
                        int fg_pixel_high = (sprite_shift_pattern_high[i] & 0x80) == 0x80 ? 0x1 : 0x0;
                        //We combine them into a 2bit ID
                        fg_pixel = ((fg_pixel_high << 1) | fg_pixel_low) & 0x03;
                        //We get the sprite palette and if it has priority over the background
                        fg_palette = (visible_oams[i].getAttribute() & 0x03) + 0x04;
                        fg_priority = (visible_oams[i].getAttribute() & 0x20) == 0;

                        //If the pixel isn't transparent and we are rendering sprite 0, we set the 0th sprite being rendered to true
                        if (fg_pixel != 0) {
                            if (i == 0)
                                spriteZeroBeingRendered = true;
                            break;
                        }
                    }
                }
            }
        }

        int pixel = 0x00;
        int palette = 0x00;
        //If the background pixel is transparent the final color is the foreground one
        if (bg_pixel == 0 && fg_pixel > 0) {
            pixel = fg_pixel;
            palette = fg_palette;
        }
        //If the foreground color is transparent the final color is the background one
        if (bg_pixel > 0 && fg_pixel == 0) {
            pixel = bg_pixel;
            palette = bg_palette;
        }
        //If neither of the pixels are transparent
        if (bg_pixel > 0 && fg_pixel > 0) {
            //If the foreground has priority over the background, the final color is the foreground one
            if (fg_priority) {
                pixel = fg_pixel;
                palette = fg_palette;
                //Otherwise the final color is the background one
            } else {
                pixel = bg_pixel;
                palette = bg_palette;
            }
            //If we are rendering the 0th sprite and a sprite zero hit is possible then a sprite zero hit may have occur
            if (spriteZeroBeingRendered && spriteZeroHitPossible) {
                //If we are rendering background and sprites
                if (mask_register.isRenderBackgroundSet() && mask_register.isRenderSpritesSet()) {
                    //If we are in the valid test.state space (if we don't render the first columns we don't test.state for hit in it)
                    if (!(mask_register.isRenderBackgroundLeftSet() || mask_register.isRenderSpriteLeftSet())) {
                        if (cycle >= 9 && cycle < 258)
                            status_register.setSpriteZeroHit(true);
                    } else if (cycle >= 1 && cycle < 258)
                        status_register.setSpriteZeroHit(true);
                }
            }
        }

        //If we are in the visible area we push a pixel into the screen buffer
        if (cycle - 1 >= 0 && cycle - 1 < SCREEN_WIDTH && scanline >= 0 && scanline < SCREEN_HEIGHT) {
            screen_buffer_tmp.put((byte) ((int) (getColorFromPalette(palette, pixel).getRed() * 255) & 0xFF));
            screen_buffer_tmp.put((byte) ((int) (getColorFromPalette(palette, pixel).getGreen() * 255) & 0xFF));
            screen_buffer_tmp.put((byte) ((int) (getColorFromPalette(palette, pixel).getBlue() * 255) & 0xFF));
            screen_buffer_tmp.put((byte) ((int) (getColorFromPalette(palette, pixel).getOpacity() * 255) & 0xFF));
        }

        if (mask_register.isRenderBackgroundSet() || mask_register.isRenderSpritesSet()) {
            if (cycle == 260 && scanline < 240) {
                cartridge.getMapper().notifyScanline();
            }
        }

        cycle++;
        //If we are at the end of a scanline
        if (cycle >= 341) {
            cycle = 0;
            scanline++;
            //If we are a the bottom of the screen
            if (scanline >= 261) {
                //We reset the scanline to the top, set the frameComplete flag and flip the screen buffer to prepare rendering
                scanline = -1;
                frame_complete = true;
                odd_frame = !odd_frame;
                //We put the content if the tmp buffer to the screen buffer that will be fetched by the UI
                screen_buffer_tmp.flip();
                screen_buffer.clear();
                screen_buffer.put(screen_buffer_tmp);
                screen_buffer.flip();
            }
        }
    }

    // Debug Methods

    /**
     * @return an array of ObjectAttribute containing all the OAM
     */
    public ObjectAttribute[] getOams() {
        return oams;
    }

    /**
     * @param i         the pattern table index
     * @param paletteId the paletteId to be used
     * @param dest      the image where to store the patternTable
     */
    public void getPatternTable(int i, int paletteId, WritableImage dest) {
        //For each row of tiles starting at the top
        for (int tileX = 0; tileX < 16; tileX++) {
            //For each tile starting at the left
            for (int tileY = 0; tileY < 16; tileY++) {
                //We compute the tile offset inside the Pattern Memory
                int offset = tileX * 256 + tileY * 16;
                //For each row of the tile
                for (byte row = 0; row < 8; row++) {
                    //We get the lsb of the pixels of the row
                    int tile_lsb = ppuRead(i * 0x1000 + offset + row, true);
                    //We get the msb of the pixels of the row
                    int tile_msb = ppuRead(i * 0x1000 + offset + row + 8, true);
                    //for each pixel of the row
                    for (int col = 0; col < 8; col++) {
                        //We compute the pixel id
                        int pixel = ((tile_lsb & 0x01) << 1) | (tile_msb & 0x01);
                        //We shift the tile registers to get the next pixel id
                        tile_lsb >>= 1;
                        tile_msb >>= 1;
                        //We populate the image by getting the right color from the palette using the palette and pixel IDs
                        dest.getPixelWriter().setColor(((tileY << 3) | (7 - col)), ((tileX << 3) | row), getColorFromPalette(paletteId, pixel));
                    }
                }
            }
        }
    }

    /**
     * @param i    the pattern table index
     * @param dest the image where to store the nametable
     */
    public void getNametable(int i, WritableImage dest) {
        //For each row of tiles starting at the top
        for (int y = 0; y < 30; y++) {
            //For each tile starting at the left
            for (int x = 0; x < 32; x++) {
                //For each row of the tile
                //We read the tile ID by selecting the correct nametable using the mirroring mode
                int offset = 0x0400 * (i & 0x3);
                int tile_id = ppuRead(0x2000 | offset | (y << 5) | x, true);
                //We read the tile attribute starting at offset 0x03C0 of the selected nametable, the attribute offset is calculated using the tile pos divided by 4
                int tile_attrib = ppuRead(0x23C0 | offset | ((y >> 2) << 3) | (x >> 2), true);
                //We select the right attribute depending on the tile pos inside the current 4x4 tile grid
                if ((y & 0x02) == 0x02)
                    tile_attrib = (tile_attrib >> 4) & 0x00FF;
                if ((x & 0x02) == 0x02)
                    tile_attrib = (tile_attrib >> 2) & 0x00FF;
                //We only keep the 2 lsb of the attribute
                tile_attrib &= 0x03;
                //We use the attribute to determinate the tile palette
                int palette = tile_attrib & 0b11;
                int pid;
                for (int row = 0; row < 8; row++) {
                    //We use the tile id and the current row index to get the lsb of the 8 pixel IDs of the row (low bitplane)
                    int tile_lsb = ppuRead((control_register.isPatternBackgroundSet() ? 0x1 << 12 : 0) + (tile_id << 4) + row, true);
                    //We use the tile id and the current row index to get the msb of the 8 pixels of the row (high bitplane)
                    int tile_msb = ppuRead((control_register.isPatternBackgroundSet() ? 0x1 << 12 : 0) + (tile_id << 4) + row + 8, true);
                    //For each pixel of the row
                    for (int col = 0; col < 8; col++) {
                        //We get the correct pixel ID by reading the 2 bitplanes
                        int p0_pixel = (tile_lsb & 0x80) > 0 ? 0x1 : 0x0;
                        int p1_pixel = (tile_msb & 0x80) > 0 ? 0x1 : 0x0;
                        int pixel = ((p1_pixel << 1) | p0_pixel) & 0x000F;
                        pid = palette;
                        //If the pixel ID is 0, then it's transparent so we use pixel 0 of palette 0
                        if (pixel == 0x00) pid = 0x00;
                        //We shift the tile registers to get the next pixel id
                        tile_lsb = (tile_lsb << 1) & 0xFFFF;
                        tile_msb = (tile_msb << 1) & 0xFFFF;
                        //We populate the image by getting the right color from the palette using the palette and pixel IDs
                        dest.getPixelWriter().setColor(((x << 3) | (col)), ((y << 3) | row), getColorFromPalette(pid, pixel));
                    }
                }
            }
        }
    }

    public Tile getNametableTile(int x, int y, int nametable) {
        Tile dest = new Tile(false);
        dest.x = x;
        dest.y = y;
        int offset = 0x0400 * (nametable & 0x3);
        dest.addr = 0x2000 | offset | (y << 5) | x;
        //We read the tile ID by selecting the correct nametable using the mirroring mode
        dest.tile = ppuRead(dest.addr, true);
        //We read the tile attribute starting at offset 0x03C0 of the selected nametable, the attribute offset is calculated using the tile pos divided by 4
        dest.attribute = ppuRead(0x23C0 | offset | ((y >> 2) << 3) | (x >> 2), true);
        //We select the right attribute depending on the tile pos inside the current 4x4 tile grid
        if ((y & 0x02) == 0x02)
            dest.attribute = (dest.attribute >> 4) & 0x00FF;
        if ((x & 0x02) == 0x02)
            dest.attribute = (dest.attribute >> 2) & 0x00FF;
        //We only keep the 2 lsb of the attribute
        dest.attribute &= 0x03;
        dest.palette = dest.attribute & 0b11;
        int pid;
        //For each row of the tile
        for (int row = 0; row < 8; row++) {
            //We use the tile id and the current row index to get the lsb of the 8 pixel IDs of the row (low bitplane)
            int tile_lsb = ppuRead((control_register.isPatternBackgroundSet() ? 0x1 << 12 : 0) + (dest.tile << 4) + row, true);
            //We use the tile id and the current row index to get the msb of the 8 pixels of the row (high bitplane)
            int tile_msb = ppuRead((control_register.isPatternBackgroundSet() ? 0x1 << 12 : 0) + (dest.tile << 4) + row + 8, true);
            //We use the attribute to determinate the tile palette
            //For each pixel of the row
            for (int col = 0; col < 8; col++) {
                //We get the correct pixel ID by reading the 2 bitplanes
                int p0_pixel = (tile_lsb & 0x80) > 0 ? 0x1 : 0x0;
                int p1_pixel = (tile_msb & 0x80) > 0 ? 0x1 : 0x0;
                int pixel = ((p1_pixel << 1) | p0_pixel) & 0x000F;
                pid = dest.palette;
                //If the pixel ID is 0, then it's transparent so we use pixel 0 of palette 0
                if (pixel == 0x00) pid = 0x00;
                //We shift the tile registers to get the next pixel id
                tile_lsb = (tile_lsb << 1) & 0xFFFF;
                tile_msb = (tile_msb << 1) & 0xFFFF;
                //We populate the image by getting the right color from the palette using the palette and pixel IDs
                dest.colors[col | (row << 3)] = getColorFromPalette(pid, pixel);
            }
        }
        return dest;
    }

    public Tile getPatterntableTile(int x, int y, int paletteId, int patterntableId) {
        Tile dest = new Tile(false);
        int offset = y * 256 + x * 16;
        dest.tile = y | (x << 4);
        dest.addr = patterntableId * 0x1000 + offset;
        //For each row of the tile
        for (byte row = 0; row < 8; row++) {
            //We get the lsb of the pixels of the row
            int tile_lsb = ppuRead(dest.addr + row, true);
            //We get the msb of the pixels of the row
            int tile_msb = ppuRead(dest.addr + row + 8, true);
            //for each pixel of the row
            for (int col = 0; col < 8; col++) {
                //We compute the pixel id
                int pixel = ((tile_lsb & 0x01) << 1) | (tile_msb & 0x01);
                //We shift the tile registers to get the next pixel id
                tile_lsb >>= 1;
                tile_msb >>= 1;
                //We populate the image by getting the right color from the palette using the palette and pixel IDs
                dest.colors[(7 - col) | (row << 3)] = getColorFromPalette(paletteId, pixel);
            }
        }
        return dest;
    }

    public Tile getOamTile8x8(int oamId) {
        if (oamId < 64) {
            Tile tile = new Tile(false);
            ObjectAttribute entry = oams[oamId];
            for (int row = 0; row < 8; row++) {
                tile.addr = (control_register.isPatternSpriteSet() ? 1 << 12 : 0) | (entry.getId() << 4);
                tile.x = entry.getX();
                tile.y = entry.getY();
                tile.tile = entry.getId() >> 1;
                tile.palette = (entry.getAttribute() & 0x3) + 4;
                tile.attribute = entry.getAttribute() & 0xE0;
                int sprite_pattern_low, sprite_pattern_high;
                int sprite_pattern_addr_low, sprite_pattern_addr_high;
                //We retrieve the low bit plane address of the current sprite row
                if ((entry.getAttribute() & 0x80) != 0x80) // Sprite normally oriented
                    sprite_pattern_addr_low = tile.addr | row;
                else //Sprite flipped vertically
                    sprite_pattern_addr_low = tile.addr | (7 - row);

                //The high bit plane one is offset by 8 from the low bit plane
                sprite_pattern_addr_high = (sprite_pattern_addr_low + 8) & 0xFFFF;
                //We read the 2 bit planes
                sprite_pattern_low = ppuRead(sprite_pattern_addr_low, true);
                sprite_pattern_high = ppuRead(sprite_pattern_addr_high, true);

                //If the sprite is flipped horizontally
                if ((entry.getAttribute() & 0x40) == 0x40) {
                    sprite_pattern_low = NumberUtils.byteFlip(sprite_pattern_low);
                    sprite_pattern_high = NumberUtils.byteFlip(sprite_pattern_high);
                }
                //For each pixel of the row
                for (int col = 0; col < 8; col++) {
                    //We compute the pixel and palette id
                    int px = ((sprite_pattern_low & 0x80) == 0x80 ? 0x1 : 0x0) | ((((sprite_pattern_high & 0x80) == 0x80 ? 0x1 : 0x0)) << 1);
                    //We draw the pixel
                    tile.colors[col | (row << 3)] = getColorFromPalette(px == 0 ? 0 : tile.palette, px);
                    //We shift the bit planes for the next pixel
                    sprite_pattern_high <<= 1;
                    sprite_pattern_low <<= 1;
                }
            }
            return tile;
        }
        return null;
    }

    public Tile getOamTile8x16(int oamId) {
        Tile tile = new Tile(true);
        ObjectAttribute entry = oams[oamId];
        for (int row = 0; row < 16; row++) {
            int sprite_pattern_low, sprite_pattern_high;
            int sprite_pattern_addr_low, sprite_pattern_addr_high;
            if ((entry.getAttribute() & 0x80) != 0x80) {
                if (row < 8)
                    sprite_pattern_addr_low = ((entry.getId() & 0x1) << 12) | ((entry.getId() & 0xFE) << 4) | row;
                else
                    sprite_pattern_addr_low = ((entry.getId() & 0x1) << 12) | (((entry.getId() & 0xFE) + 1) << 4) | (row - 8);
            } else {
                if (row < 8)
                    sprite_pattern_addr_low = ((entry.getId() & 0x1) << 12) | (((entry.getId() & 0xFE) + 1) << 4) | (7 - row);
                else
                    sprite_pattern_addr_low = ((entry.getId() & 0x1) << 12) | ((entry.getId() & 0xFE) << 4) | (7 - row + 8);
            }
            sprite_pattern_addr_high = (sprite_pattern_addr_low + 8) & 0xFFFF;
            sprite_pattern_low = ppuRead(sprite_pattern_addr_low, true);
            sprite_pattern_high = ppuRead(sprite_pattern_addr_high, true);

            if ((entry.getAttribute() & 0x40) == 0x40) {
                sprite_pattern_low = NumberUtils.byteFlip(sprite_pattern_low);
                sprite_pattern_high = NumberUtils.byteFlip(sprite_pattern_high);
            }
            tile.addr = entry.getId();
            tile.x = entry.getX();
            tile.y = entry.getY();
            tile.tile = entry.getId() >> 1;
            tile.palette = (entry.getAttribute() & 0x3) + 4;
            for (int col = 0; col < 8; col++) {
                int px = ((sprite_pattern_low & 0x80) == 0x80 ? 0x1 : 0x0) | ((((sprite_pattern_high & 0x80) == 0x80 ? 0x1 : 0x0)) << 1);

                tile.colors[col | (row << 3)] = getColorFromPalette(px == 0 ? 0 : tile.palette, px);

                sprite_pattern_high <<= 1;
                sprite_pattern_low <<= 1;
            }
        }
        return tile;
    }
    public ByteBuffer getFrameBuffer() {
        return screen_buffer; // Returns the ByteBuffer, giving the current frame
    }
    
}
