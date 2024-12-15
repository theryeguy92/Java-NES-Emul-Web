package utils;

import exceptions.InvalidFileException;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReader {

    private int currentIndex;
    private final byte[] file;

    public FileReader(String filename) throws InvalidFileException {
        try {
            file = Files.readAllBytes(Paths.get(filename));
        } catch (IOException e) {
            throw new InvalidFileException("Unable to open file \"" + filename + "\"");
        }
        currentIndex = 0;
    }

    /**
     * @return the next byte of the file
     * @throws EOFException If we try to read outside the file
     */
    public byte nextByte() throws EOFException {
        if (currentIndex < file.length)
            return file[currentIndex++];
        throw new EOFException("No bytes left to read");
    }

    /**
     * @param size the number of bytes to read
     * @return an array of int containing the bytes
     * @throws EOFException If we try to read outside the file
     */
    public byte[] readBytes(int size) throws EOFException {
        byte[] buf = new byte[size];
        for (int i = 0; i < size; i++)
            buf[i] = nextByte();
        return buf;
    }
}
