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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opengis.filter.Filter;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.ntua.generic.DataStructures.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.vividsolutions.jts.geom.Point;
import org.opengis.filter.IncludeFilter;
import org.opengis.geometry.coordinate.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.Geometry;
//import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.geom.MultiPolygon;
//import com.vividsolutions.jts.geom.Polygon;

public class GeosPixels {
	public List<Locus> loci;
	public MultiPolygon multipolygon = null;

	public static Map<String, GeosPixels> loadCoordinates(String geofolder) throws IOException {

		String[] geoareas = { "Euro", "SAme", "NAfr", "SAfr" };
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
		int index = 0;
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
				double s2 = sn * sinx * cosx;
				double s3 = -sn * sinx;
				double sxy = Math.sqrt(s1 * s1 + s2 * s2);
				double glon = Math.toDegrees(Math.atan(s2 / s1));
				double glat = Math.toDegrees(Math.atan(p2 * (s3 / sxy)));
				// System.out.println(glat+";"+glon);
				Coordinate point = new Coordinate(glon, glat);
				Locus locus = new Locus(point, index++);
				extra_coord.loci.add(locus);
			}
		}
		mapper.put("MSG-Disk", extra_coord);
		return mapper;
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
		IncludeFilter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM,
												// 10,20,30,40)")

		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);

		Map<String, MultiPolygon> countries = new HashMap<String, MultiPolygon>();
		try (FeatureIterator<SimpleFeature> features = collection.features()) {
			while (features.hasNext()) {
				MultiPolygon multipolygon = null;
				String name = null;
				SimpleFeature feature = (SimpleFeature) features.next();

				for (Object ff : feature.getAttributes()) {

					if (ff instanceof com.vividsolutions.jts.geom.MultiPolygon) {
						multipolygon = (MultiPolygon) ff;
					} else if (ff instanceof java.lang.String) {
						name = (String) ff;
					}
				}

				countries.put(name, multipolygon);
			}
		} finally {

		}

		// dataStore.dispose();
		for (Entry<String, MultiPolygon> entryyy : countries.entrySet()) {
			System.out.println("Country : " + entryyy.getKey());
		}
		return countries;
	}
}