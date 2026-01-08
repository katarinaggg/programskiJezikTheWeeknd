package intermediate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstructionEmitter {

    // cuvamo listu instrukcija koje cemo da stampamo u fajl
    private final List<String> instructions = new ArrayList<>();

    // Mapa: ime labele -> redni broj instrukcije (IP)
    private final Map<String, Integer> labelToIp = new HashMap<>();

    // Trenutni IP (redni broj sledece instrukcije)
    // Koristi se samo za mapiranje labela
    private int ip = 0;

    //Dodavanje nove instrukcije u listu, IP se povecava jer je dodata nova linija
    public void emit(String instr) {
        instructions.add(instr);
        ip++;
    }

    // Emitovanje labele - labela se mapira na trenutni IP, a u listu instrukcija se dodaje NOP
    // NOP sluzi kao sidro na koje moze da se skoci pomocu instrukcije jmp
    public void emitLabel(String label) {
        labelToIp.put(label, ip);
        instructions.add("nop");   // instrukcija bez efekta
        ip++;
    }


    //prolazimo kroz sve instrukcije i zamenjujemo labele konkretnim IP vrednostima
    public void writeToFile(String filePath) {
        try {
            // Lista instrukcija nakon sto se labele razrese u IP vrednosti
            List<String> resolved = new ArrayList<>();

            // DRUGI PROLAZ kroz instrukcije
            // U ovom prolazu se simbolicke labele zamenjuju konkretnim IP brojevima
            for (String instr : instructions) {

                // USLOVNI SKOK: jmp_zero
                // Skace na cilj ako je vrednost na vrhu steka jednaka 0
                // Ako je cilj labela, menjamo je njenim IP brojem

                if (instr.startsWith("jmp_zero ")) {
                    String target = instr.substring("jmp_zero ".length());

                    if (labelToIp.containsKey(target)) {
                        // Zamena labele odgovarajucim IP brojem
                        resolved.add("jmp_zero " + labelToIp.get(target));
                    } else {
                        // Ako cilj nije labela, instrukcija ostaje ista
                        resolved.add(instr);
                    }

                // BEZUSLOVNI SKOK: jmp
                // Moze da skace:
                // - na labelu
                // - na IP (jmp IP)
                // - direktno na broj instrukcije

                } else if (instr.startsWith("jmp ")) {
                    String target = instr.substring(4);

                    if (labelToIp.containsKey(target)) {
                        // Ako je cilj labela, zamenjujemo je IP brojem
                        resolved.add("jmp " + labelToIp.get(target));
                    } else {
                        // jmp IP ili jmp na vec poznat broj
                        resolved.add(instr);
                    }


                // PUSH instrukcija
                // Koristi se i za push povratne adrese pri pozivu funkcije
                // Ako se pushuje labela, ona se zamenjuje njenim IP brojem

                } else if (instr.startsWith("push ")) {
                    String value = instr.substring(5);

                    if (labelToIp.containsKey(value)) {
                        // Push konkretne IP vrednosti umesto labele
                        resolved.add("push " + labelToIp.get(value));
                    } else {
                        // Push literala ili promenljive ostaje nepromenjen
                        resolved.add(instr);
                    }


                // SVE OSTALE INSTRUKCIJE
                // (add, sub, pop, echo, nop, itd.)
                // Ne zahtevaju nikakvu obradu
                } else {
                    resolved.add(instr);
                }
            }

            // Upis konacnog medjukoda u fajl
            // U ovom trenutku vise nema labela, samo numericki IP
            Files.write(Path.of(filePath), resolved);

            System.out.println(
                    "Intermediate code successfully generated in file: " + filePath
            );

        } catch (IOException e) {
            // Greska pri upisu fajla
            throw new RuntimeException(e);
        }
    }
}
