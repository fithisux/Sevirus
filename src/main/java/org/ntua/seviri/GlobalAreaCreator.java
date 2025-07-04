package org.ntua.seviri;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.ntua.generic.DataStructures.Locus;
import org.ntua.generic.ProcessorUtilities;
import org.ntua.seviri.model.GeosPixels;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GlobalAreaCreator {

    public static void main(String[] args) {
        GeosPixels extra_coord = ProcessorUtilities.synthesizeGlobalCoverage();

        try {
            String csvFile = args[0];
            File f = new File(csvFile);
            if (f.exists() || f.isDirectory()) {
                throw new RuntimeException("Existing file or directory");
            }
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(new String[]{"LON", "LAT"})
                    .build();
            try (final CSVPrinter printer = new CSVPrinter(new FileWriter(csvFile), csvFormat)) {
                for (Locus locus : extra_coord.loci) {
                    printer.printRecord(
                            Double.toString(locus.coordinate().x),
                            Double.toString(locus.coordinate().y)
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
