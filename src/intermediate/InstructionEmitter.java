package intermediate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InstructionEmitter {

    // cuvamo listu instrukcija koje cemo da stampamo u fajl
    private final List<String> instructions = new ArrayList<>();

    // dodaj instrukciju
    public void emit(String instruction) {
        instructions.add(instruction);
    }

    public List<String> getInstructions() {
        return instructions;
    }


    // upisuje medjukod u prosledjenu putanju fajla
    public void writeToFile(String filePath) {
        try {
            Files.write(Path.of(filePath), instructions);
            System.out.println();
            System.out.println("Intermediate code successfully generated in file: " + filePath);
        } catch (IOException e) {
            throw new RuntimeException("Greška pri upisu međukoda u fajl: " + filePath, e);
        }
    }
}
