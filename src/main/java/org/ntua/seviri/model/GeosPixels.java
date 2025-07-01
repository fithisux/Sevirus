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

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.ntua.generic.DataStructures.Locus;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class GeosPixels {

    public List<Locus> loci;
    public MultiPolygon multipolygon = null;

    public static Map<String, GeosPixels> loadCoordinates(String geofolder) throws IOException {

        String[] geoareas = {"Euro", "SAme", "NAfr", "SAfr"};
        String geofref_lat = null;
        String geofref_lon = null;

        Map<String, GeosPixels> mapper = new HashMap<>();

        for (String geoarea : geoareas) {
            geofref_lat = "HDF5_LSASAF_MSG_LAT_" + geoarea + "_4bytesPrecision";
            geofref_lon = "HDF5_LSASAF_MSG_LON_" + geoarea + "_4bytesPrecision";

            GeosPixels geofref_coord = new GeosPixels();
            geofref_coord.multipolygon = null;
            geofref_lat = geofolder + "/" + geofref_lat;
            geofref_lon = geofolder + "/" + geofref_lon;
            double[] lat = GeosPixels.readBand(geofref_lat, "LAT");
            double[] lon = GeosPixels.readBand(geofref_lon, "LON");
            geofref_coord.loci = new ArrayList<>();
            for (int l = 0; l < lat.length; l++) {
                if (!Double.isNaN(lat[l]) && !Double.isNaN(lon[l])) {
                    Coordinate point = new Coordinate(lon[l], lat[l]);
                    Locus locus = new Locus(point, l);
                    geofref_coord.loci.add(locus);
                }
            }
            mapper.put(geoarea, geofref_coord);
        }

//		GeosPixels extra_coord = new GeosPixels();
//		extra_coord.multipolygon = null;
//		extra_coord.loci = new ArrayList<>();
//		int index = 0;
//		CsvReader csv_global = new CsvReader(
//				(new File(geofolder + "/" + "global_locus.csv")).getAbsolutePath(), '\t');
//		csv_global.readHeaders();
//		while (csv_global.readRecord()) {
//			double lon = Double.parseDouble(csv_global.get("LON"));
//			double lat = Double.parseDouble(csv_global.get("LAT"));
//			Coordinate coordinate=new Coordinate(lon, lat);
//			Locus locus = new Locus(coordinate, index++);
//			extra_coord.loci.add(locus);
//		}
//		csv_global.close();
//		mapper.put("MSG-Disk", extra_coord);
        return mapper;
    }

    public static double[] readBand(String filename, String bandName) throws IOException {

        NetcdfFile ncfile = NetcdfFile.open(filename);
        Variable v = ncfile.findVariable(bandName);

        int missing_value = v.findAttribute("MISSING_VALUE").getNumericValue().intValue();
        int scaling_factor = v.findAttribute("SCALING_FACTOR").getNumericValue().intValue();
        int[] varShape = v.getShape();

        System.out.println(bandName + " has missing_value = " + missing_value + " scaling_factor=" + scaling_factor);
        System.out.println("shape is " + varShape[0] + "   " + varShape[1]);

        double[] vals = null;
        try {
            Array data2D = v.read(null, varShape);
            vals = new double[varShape[0] * varShape[1]];

            for (int i = 0; i < vals.length; i++) {
                int originalValue = data2D.getInt(i);
                if (originalValue == missing_value) {
                    vals[i] = Double.NaN;
                } else {

                    vals[i] = ((double) originalValue) / ((double) scaling_factor);
                }
            }

        } catch (InvalidRangeException e) {
            e.printStackTrace();
        } finally {
            ncfile.close();
            return vals;
        }
    }

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
                System.out.println(feature.getAttributeCount());
                MultiPolygon countryShape = (MultiPolygon) feature.getAttribute(0);
                String countryName = (String) feature.getAttribute(2);
                System.out.println(countryName);
                countries.put(countryName, countryShape);
            }
        }

        Map<String, MultiPolygon> sortedCountries = new HashMap<String, MultiPolygon>();
        List<String> sortedCountryNames = new ArrayList<>(countries.keySet());
        Collections.sort(sortedCountryNames);
        sortedCountryNames.forEach((countryName) -> {
            sortedCountries.put(countryName, countries.get(countryName));
        });
        return sortedCountries;
    }

    public GeosPixels maskIt(MultiPolygon multipolygon) {

        GeosPixels geofref_coord = new GeosPixels();
        if (multipolygon == null) {
            System.out.println("!");
        }
        geofref_coord.multipolygon = multipolygon;
        geofref_coord.loci = new ArrayList<>();
        System.out.println("Size pre mask = " + this.loci.size());

        for (Locus locus : this.loci) {

            Point pt = JTSFactoryFinder.getGeometryFactory().createPoint(locus.coordinate);

            if (multipolygon.contains(pt)) {
                geofref_coord.loci.add(locus);
            }
        }
        System.out.println("Size post mask = " + geofref_coord.loci.size());

        return geofref_coord;
    }
}
