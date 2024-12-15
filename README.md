# Java-NES-Emulator
A NES Emulator made in Java. Final project for Principles in Software Engineering

To play the emulator, run the java project nes_emul on your IDE and click File -> "Path To Your Rom" *Note, it only supports specific ROMs. Super Mario and Zelda will be able to work.*

# Nintendo Emulation: Overall Framework and Functionality

The **Nintendo Entertainment System (NES)** was, and still is, an iconic console that set the foundation for future gaming systems. This project aims to emulate the NES using Java, showcasing how pattern designs can be employed for developing software for emulation purposes. The project operates at a low level, focusing on replicating the hardware of the system itself. By utilizing principles of emulation and the resources available to enthusiasts, this project allows the recreation and enjoyment of classic childhood games.

## NES Design

What made the NES groundbreaking was its simplicity and efficiency, which stemmed from its key components:

1. **Central Processing Unit (CPU)**  
   The NES features a custom 8-bit microprocessor derived from the Ricoh 2A03. While limited, it provides an effective set of instructions optimized for game logic execution.

2. **Picture Processing Unit (PPU)**  
   Responsible for rendering the video output, the PPU manages sprites, background tiling, and the color palette. It can display up to 64 sprites per frame and leverages a palette of 54 indexed colors out of 64 possible options.

3. **Audio Processing Unit (APU)**  
   The APU generates the NES's audio, including sounds and music. It includes five sound channels: two pulse wave generators, a triangle wave generator, a noise generator, and a delta modulation channel (DMC) for simple playback.

4. **Memory**  
   The NES architecture includes 2KB of onboard RAM and 2KB of video RAM (VRAM). Cartridge-based games often expanded this with additional ROM and RAM, which was accessed through memory mapping techniques. The system also supports memory mirroring to optimize available space.


   ## NES Architecture, Mapping, and Bus Functionality

### 1. Mapping
Memory mapping in the NES refers to the management of memory resources within the hardware. The NES utilizes a 16-bit address bus, allowing it to address up to 64KB of memory. However, the actual memory used in the system consists of RAM, ROM, and memory-mapped I/O registers.

### 2. Bus Functionality
The bus system serves as the connective infrastructure, or the "wiring," that enables communication between the CPU, PPU, memory, and APU. Here’s an overview of the bus components:

- **Address Bus**  
  The address bus is a 16-bit wide bus used by the CPU to specify the memory address it needs to read from or write to, allowing the CPU to directly access up to 64KB of memory. When executing instructions involving memory access, the CPU places the relevant address on the address bus. Devices like the PPU and APU also use the address bus to determine which registers or memory locations they can access.

- **Data Bus**  
  The data bus is an 8-bit wide bus used to transfer data between the CPU, memory, and other input/output devices. When the CPU reads from or writes to a memory location (or I/O register), the data is transferred via the data bus. It carries the actual value being read from or written to the address specified on the address bus.

  An illustration of the NES architecture can be found below.



![image](https://github.com/user-attachments/assets/4556b037-e06e-4de4-91fd-f3109a1a3ecd)

