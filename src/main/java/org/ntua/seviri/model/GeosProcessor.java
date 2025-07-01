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

import org.locationtech.jts.geom.MultiPolygon;
import org.ntua.generic.AbstractProcessor;
import org.ntua.generic.AuxiliaryInfo;
import org.ntua.generic.DataStructures.Locus;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GeosProcessor extends AbstractProcessor {

    public static final Map<String, String[]> bands = new HashMap<String, String[]>();

    static {
        bands.put("ET", new String[]{"ET", "ET_Q_Flag"});
        bands.put("FVC", new String[]{"FVC", "FVC_QF", "FVC_err"});
        bands.put("LST", new String[]{"LST", "Q_FLAGS", "errorbar_LST"});
    }

    public Map<String, GeosPixels> area_mapper;
    String static_geofolder;

    public GeosProcessor(String static_geofolder) throws IOException {
        this.static_geofolder = static_geofolder;
        this.area_mapper = GeosPixels.loadCoordinates(static_geofolder);
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
//			System.out.println("Start Dimap "+fileName);
//			org.ntua.headless.RunCSVConverter.saveToDimap(info.fileName, info.fileName.substring(0, info.fileName.length()-3)+"dim");
//			System.out.println("End Dimap "+fileName);
        } catch (IOException e) {
            throw new GeosProcessor.GeosException(e.getMessage());
        }
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
        // timing += ":" + temptiming.substring(12, 14);
        timing += "Z";

        String[] bandNames = bands.get(product_type);

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
                readings[index][i] = bands[i][locus.index];
            }
            readings[index][bands.length] = locus.coordinate.y;
            readings[index][bands.length + 1] = locus.coordinate.x;
            index++;
        }

        System.out.println("Start to auxiliary info " + fileName);
        AuxiliaryInfo info = new AuxiliaryInfo();
        info.readings = readings;
        info.timing = timing;
        info.threshold = 5;
        info.fileName = outputFolder + "/seviri_" + product_type + "_"
                + geographical_area + "_" + temptiming + "_" + country_name + ".csv";
        info.bandnames = bandNames;
        System.out.println("End to auxiliary info " + fileName);
        return info;
    }

    public static class GenericExtFilter implements FilenameFilter {

        private String producttype;

        public GenericExtFilter(String producttype) {
            this.producttype = producttype;
        }

        public boolean accept(File dir, String name) {
            return (name.split("_")[3].equals(this.producttype));
        }
    }

    public static class GeosException extends Exception {
        public GeosException(String what) {
            super(what);
        }
    }

}
