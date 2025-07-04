/*
 	Copyright (c) ${date} Dr. Vasileios I. Anagnostopoulos.
	This file is part of Sevirus.

    Sevirus is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Sevirus is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

	Contributors:
   Vasileios I. Anagnostopoulos - initial API and implementation under subcontract for Aberystwyth University
 */
package org.ntua.seviri.model;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.locationtech.jts.geom.MultiPolygon;
import org.ntua.generic.ProcessorUtilities;
import org.ntua.generic.AuxiliaryInfo;
import org.ntua.generic.DataStructures;
import org.ntua.generic.DataStructures.Locus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GeosProcessor {

    public static final int THRESHOLD = 5;
    public static final Map<String, String[]> BANDS = ImmutableMap.of(
            "ET", new String[]{"ET", "ET_Q_Flag"},
            "FVC", new String[]{"FVC", "FVC_QF", "FVC_err"},
            "LST", new String[]{"LST", "Q_FLAGS", "errorbar_LST"}
    );

    public Map<String, GeosPixels> area_mapper;
    String static_geofolder;

    public GeosProcessor(String static_geofolder) throws IOException {
        this.static_geofolder = static_geofolder;
        this.area_mapper = GeosPixels.loadCoordinates(static_geofolder);
        var temp = ProcessorUtilities.synthesizeGlobalCoverage();
        this.area_mapper.put("MSG-Disk", temp);
    }

    public void savePins(AuxiliaryInfo info, Map<DataStructures.Place, double[]> pins, String csvFile)
            throws IOException {
        System.out.println("Pins is empty: "+pins.isEmpty());
        if (pins.isEmpty()) return;

        final String [] readingValues = new String[info.bandNames.length+2];
        System.arraycopy(info.bandNames, 0, readingValues, 0, info.bandNames.length);
        readingValues[info.bandNames.length] = "Lat";
        readingValues[info.bandNames.length+1] = "Lon";

        final String [] placeValues = {"x", "y", "site_id"};

        final String[] headerValues = new String[readingValues.length + placeValues.length];

        System.arraycopy(placeValues, 0, headerValues, 0, placeValues.length);
        System.arraycopy(readingValues, 0, headerValues,  placeValues.length, readingValues.length);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headerValues)
                .build();

        String[] records = new String[headerValues.length];
        try (final CSVPrinter printer = new CSVPrinter(new FileWriter(csvFile), csvFormat)) {
            for (Map.Entry<DataStructures.Place, double[]> entry : pins.entrySet()) {
                records[0] = Double.toString(entry.getKey().coordinate().x);
                records[1] = Double.toString(entry.getKey().coordinate().y);
                records[2] = entry.getKey().name();
                for(int i= 0; i < readingValues.length ;i++) {
                    records[3+i] = Double.toString(entry.getValue()[i]);
                }
                printer.printRecord(records);
            }
        }
        System.out.println("finish writing "+pins.size());
    }

    public void doConversion(String fileName, String outputFolder, String country_name, MultiPolygon country_polygon) throws GeosException {
        try {
            System.out.println("Converting " + fileName);
            String name = (new File(fileName)).getName();
            String geographical_area = name.split("_")[4];

            GeosPixels geos_pixels = area_mapper.get(geographical_area);
            if (country_polygon != null) {
                geos_pixels = geos_pixels.maskIt(country_polygon);
            }
            AuxiliaryInfo info = this.readme(fileName, outputFolder, country_name, geos_pixels);
            System.out.println("Converted product " + fileName);
            if (info == null) {
                throw new GeosException("cannot find area");
            }
            System.out.println("Start csv " + fileName);
            info.saveToCSV();
            System.out.println("End csv " + fileName);
        } catch (IOException e) {
            throw new GeosProcessor.GeosException(e.getMessage());
        }
    }

    public void process(String fileName, List<DataStructures.Place> places, String csvFile, double threshold) throws IOException {
        System.out.println("Converting " + fileName);
        String name = (new File(fileName)).getName();
        String geographical_area = name.split("_")[4];

        GeosPixels geos_pixels = area_mapper.get(geographical_area);
        AuxiliaryInfo info = this.readme(fileName, "", "", geos_pixels);
        Map<DataStructures.Place, double[]> assoc = ProcessorUtilities.scanFile(info, places, threshold);
        this.savePins(info, assoc, csvFile);
    }

    public AuxiliaryInfo readme(String fileName, String outputFolder,
                                String country_name, GeosPixels geos_pixels) throws IOException {
        String name = (new File(fileName)).getName();
        String product_type = name.split("_")[3];
        String geographical_area = name.split("_")[4];
        String temptiming = name.split("_")[5];
        String timing = temptiming.substring(0, 4);
        timing += "-" + temptiming.substring(4, 6);
        timing += "-" + temptiming.substring(6, 8);
        timing += "T" + temptiming.substring(8, 10);
        timing += ":" + temptiming.substring(10, 12);
        timing += "Z";

        String[] bandNames = BANDS.get(product_type);
        for (String bandName : bandNames) {
            System.out.println("Reading band " + bandName);
        }
        double[][] bands = new double[bandNames.length][];
        for (int k = 0; k < bandNames.length; k++) {
            bands[k] = GeosPixels.readBand(fileName, bandNames[k]);
        }
        System.out.println("Bands read " + fileName);

        double[][] readings = new double[geos_pixels.loci.size()][bands.length + 2];
        System.out.println("Readings for " + fileName + " are " + geos_pixels.loci.size());
        int index = 0;
        for (Locus locus : geos_pixels.loci) {
            for (int i = 0; i < bands.length; i++) {
                readings[index][i] = bands[i][locus.index()];
            }
            readings[index][bands.length] = locus.coordinate().y;
            readings[index][bands.length + 1] = locus.coordinate().x;
            index++;
        }

        System.out.println("Start to auxiliary info " + fileName);
        AuxiliaryInfo info = new AuxiliaryInfo();
        info.readings = readings;
        info.timing = timing;
        info.csvFile = outputFolder + "/seviri_" + product_type + "_"
                + geographical_area + "_" + temptiming + "_" + country_name + ".csv";
        info.bandNames = bandNames;
        System.out.println("End to auxiliary info " + fileName);
        return info;
    }

    public static class GeosException extends Exception {
        public GeosException(String what) {
            super(what);
        }
    }
}
