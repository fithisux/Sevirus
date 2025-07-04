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
package org.ntua.generic;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalPosition;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.ntua.generic.DataStructures.Place;
import org.ntua.seviri.model.GeosPixels;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class ProcessorUtilities {

    public static Map<String, MultiPolygon> loadCountries(String shapefile) throws IOException {
        File file = new File(shapefile);
        Map<String, Serializable> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource source = dataStore.getFeatureSource(typeName);
        IncludeFilter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        Map<String, MultiPolygon> countries = new HashMap<String, MultiPolygon>();
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                String countryName = null;
                MultiPolygon countryShape = null;
                for(var attr: feature.getAttributes()) {
                    if(attr.getClass().getName() == "java.lang.String") {
                        countryName = (String) attr;
                    } else if (attr.getClass().getName() == "org.locationtech.jts.geom.MultiPolygon") {
                        countryShape = (MultiPolygon) attr;
                    }
                }

                if (Objects.isNull(countryName)) {
                    System.out.println("Null country name. Exiting ....");
                    System.exit(-1);
                }
                if (Objects.isNull(countryShape)) {
                    System.out.println("Null country shape. Exiting ....");
                    System.exit(-1);
                }

                countries.put(countryName, countryShape);
            }
        }

        Map<String, MultiPolygon> sortedCountries = new HashMap<String, MultiPolygon>();
        var sortedCountryNames = new ArrayList<>(countries.keySet());
        Collections.sort(sortedCountryNames);
        sortedCountryNames.forEach(countryName -> {
            sortedCountries.put(countryName, countries.get(countryName));
        });
        return sortedCountries;
    }

    public static double vincenty4(Coordinate coordinate1, Coordinate coordinate2) {
        GeodeticCalculator geoCalc = new GeodeticCalculator();
        Ellipsoid reference = Ellipsoid.WGS84;
        GlobalPosition pointA = new GlobalPosition(coordinate1.y,
                coordinate1.x, 0.0);
        GlobalPosition userPos = new GlobalPosition(coordinate2.y,
                coordinate2.x, 0.0);
        double distance = geoCalc.calculateGeodeticCurve(reference, userPos,
                pointA).getEllipsoidalDistance();
        return distance / 1000;
    }

    public static Map<Place, double[]> scanFile(AuxiliaryInfo info, List<Place> places, double threshold) {
        System.out.println("Product : " + info.csvFile);
        Map<Place, double[]> assoc = new HashMap<>();
        System.out.println("Start scanning for places");
        for (Place place : places) {
            System.out.println("Start scanning for place: " + place.name());
            double distance = 0;
            double[] selected = null;
            for(var bandName : info.bandNames) {
                System.out.println(bandName);
            }

            for (double[] reading : info.readings) {
                if (Double.isNaN(reading[0]) || Double.isNaN(reading[reading.length - 1]) || Double.isNaN(reading[reading.length - 2])) {
                    continue;
                }
                Coordinate readingPoint = new Coordinate(reading[reading.length - 1], reading[reading.length - 2]);
                double temp = ProcessorUtilities.vincenty4(place.coordinate(), readingPoint);
                if ((selected == null) || (temp < distance)) {
                    distance = temp;
                    selected = reading;
                }
            }

            System.out.println("Distance: "+distance);

            if (distance <= threshold) {
                assoc.put(place, selected);
            }
        }
        System.out.println("Finished scanning for places");
        return assoc;
    }

    public static GeosPixels synthesizeGlobalCoverage() {
        int globalsize = 3712;
        double COFF = 1856;
        double LOFF = 1856;
        double CFAC = 13642337;
        double LFAC = 13642337;
        int scale = 1 << 16;
        double p1 = 42164;
        double p2 = 1.006803;
        double p3 = 1737121856;
        GeosPixels extra_coord = new GeosPixels();
        extra_coord.multipolygon = null;
        extra_coord.loci = new ArrayList<>();
        for (int i = 0; i < globalsize; i++) {
            double x = (i + 1 - COFF) * scale / CFAC;
            double cosx = Math.cos(Math.toRadians(x));
            double sinx = Math.sin(Math.toRadians(x));
            for (int j = 0; j < globalsize; j++) {
                double y = (j + 1 - LOFF) * scale / LFAC;
                double cosy = Math.cos(Math.toRadians(y));
                double siny = Math.sin(Math.toRadians(y));

                double sdsquared = Math.pow(p1 * cosx * cosy, 2) - (Math.pow(cosy, 2) + p2 * Math.pow(siny, 2)) * p3;
                if (sdsquared < 0) {
                    sdsquared = 0;
                }
                double sd = Math.sqrt(sdsquared);
                double sn = (p1 * cosx * cosy - sd) / (Math.pow(cosy, 2) + p2 * Math.pow(siny, 2));
                double s1 = p1 - sn * cosx * cosy;
                double s2 = sn * sinx * cosy;
                double s3 = -sn * siny;
                double sxy = Math.sqrt(s1 * s1 + s2 * s2);
                double glon = Math.toDegrees(Math.atan(s2 / s1));
                double glat = Math.toDegrees(Math.atan(p2 * (s3 / sxy)));
                Coordinate point = new Coordinate(glon, glat);
                DataStructures.Locus locus = new DataStructures.Locus(point, extra_coord.loci.size());
                extra_coord.loci.add(locus);
            }
        }
        return extra_coord;
    }

    public static List<Place> readPlaces(String fileName) throws IOException {
        var f = new File(fileName);
        List<Place> places = new ArrayList<>();
        System.out.println("start reading places");
        CSVFormat csvFormat = CSVFormat.RFC4180.withSkipHeaderRecord(true).builder()
                .setHeader(new String[]{"lat", "lon"})
                .build();

        try (
                Reader reader = Files.newBufferedReader(Paths.get(f.toURI())); CSVParser csvParser = new CSVParser(reader, csvFormat)) {
            for (CSVRecord csvRecord : csvParser) {
                double lat = Double.parseDouble(csvRecord.get("lat"));
                double lon = Double.parseDouble(csvRecord.get("lon"));
                Coordinate coordinate = new Coordinate(lon, lat);
                String name = "P" + places.size();
                places.add(new Place(coordinate, name));
            }
        }
        System.out.println("finished reading places");
        return places;
    }
}
