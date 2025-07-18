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

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.ntua.generic.DataStructures;
import org.ntua.generic.DataStructures.Locus;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import java.io.IOException;
import java.util.*;

public class GeosPixels {

    public List<Locus> loci;
    public MultiPolygon multipolygon = null;

    public final static String[] GEOAREAS =  {"Euro", "SAme", "NAfr", "SAfr"};

    public static Map<String, GeosPixels> loadCoordinates(String geofolder) throws IOException {
        String geofref_lat = null;
        String geofref_lon = null;

        Map<String, GeosPixels> mapper = new HashMap<>();

        for (String geoarea : GEOAREAS) {
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
                    DataStructures.Locus locus = new DataStructures.Locus(point, l);
                    geofref_coord.loci.add(locus);
                }
            }
            mapper.put(geoarea, geofref_coord);
        }
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

    public GeosPixels maskIt(MultiPolygon multipolygon) {

        GeosPixels geofref_coord = new GeosPixels();
        if (multipolygon == null) {
            System.out.println("!");
        }
        geofref_coord.multipolygon = multipolygon;
        geofref_coord.loci = new ArrayList<>();
        System.out.println("Size pre mask = " + this.loci.size());

        for (Locus locus : this.loci) {

            Point pt = JTSFactoryFinder.getGeometryFactory().createPoint(locus.coordinate());

            if (multipolygon.contains(pt)) {
                geofref_coord.loci.add(locus);
            }
        }
        System.out.println("Size post mask = " + geofref_coord.loci.size());

        return geofref_coord;
    }
}
