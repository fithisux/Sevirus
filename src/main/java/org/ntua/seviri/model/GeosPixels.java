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

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.ntua.generic.DataStructures.*;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class GeosPixels {
	public List<Locus> loci;
	public MultiPolygon multipolygon=null;
	public static GeometryFactory gf=new GeometryFactory();
	
	public static Map<String,GeosPixels> loadCoordinates(String geofolder) throws IOException{
		
		String[] geoareas={"Euro","SAme","NAfr","SAfr"};
		String geofref_lat = null;
		String geofref_lon = null;
		
		Map<String,GeosPixels> mapper=new HashMap<>();
		
		for(String geoarea : geoareas){
			geofref_lat = "HDF5_LSASAF_MSG_LAT_"+geoarea+"_4bytesPrecision";
			geofref_lon = "HDF5_LSASAF_MSG_LON_"+geoarea+"_4bytesPrecision";
			
			GeosPixels geofref_coord=new GeosPixels();				
			geofref_coord.multipolygon=null;
			geofref_lat = geofolder + "/" + geofref_lat;
			geofref_lon = geofolder + "/" + geofref_lon;
			double[] lat = GeosPixels.readBand(geofref_lat, "LAT");
			double[] lon = GeosPixels.readBand(geofref_lon, "LON");
			geofref_coord.loci=new ArrayList<>();	
			for (int l = 0; l < lat.length; l++) {
				boolean include = !Double.isNaN(lat[l])	&& !Double.isNaN(lon[l]);
				if(include){
					Point point=new Point(lat[l],lon[l]);
					Locus locus=new Locus(point,l);
					geofref_coord.loci.add(locus);
				}
			}
			mapper.put(geoarea,geofref_coord);
		}			
		
		int globalsize = 3712;
		double COFF = 1856 ;
		double LOFF = 1856 ;
		double CFAC = 13642337;
		double LFAC = 13642337;
		int scale = 1 << 16;
		double p1 = 42164;
		double p2 = 1.006803;
		double p3 = 1737121856;
		GeosPixels extra_coord=new GeosPixels();				
		extra_coord.multipolygon=null;
		double[] lat = new double[globalsize*globalsize];
		double[] lon = new double[globalsize*globalsize];
		extra_coord.loci=new ArrayList<>();	
		int index=0;
		for (int i = 0; i < globalsize; i++) {
			double x = (i+1 - COFF) * scale / CFAC ;
			double cosx = Math.cos(Math.toRadians(x));
			double sinx = Math.sin(Math.toRadians(x));
			for (int j = 0; j < globalsize; j++) {
				double y = (j+1 - LOFF) * scale / LFAC ;
						double cosy = Math.cos(Math.toRadians(y));
						double siny = Math.sin(Math.toRadians(y));
						double sd = Math.sqrt(Math.pow(p1 * cosx * cosy, 2) - (Math.pow(cosy,2) + p2 * Math.pow(siny,2)) * p3);
						double sn = (p1 * cosx * cosy -sd) / (Math.pow(cosy,2)  + p2 * Math.pow(siny,2));
						double s1 = p1 - sn * cosx * cosy;
						double s2 = sn * sinx * cosx;
						double s3 = -sn * sinx;
						double sxy = Math.sqrt(s1*s1 + s2*s2);
						double glon = Math.toDegrees(Math.atan( s2 / s1));
						double glat = Math.toDegrees(Math.atan( p2 * (s3 / sxy)));
						Point point=new Point(glat,glon);
						Locus locus=new Locus(point,index++);
						extra_coord.loci.add(locus);				
			}
		}
		mapper.put("Global",extra_coord);
		return mapper;
	}
	
	
	public static class Insider {
		MultiPolygon multipolygon;
		Polygon[] polygons;
		Geometry[] envelopes;
		public Insider(MultiPolygon multipolygon){
			this.multipolygon=multipolygon;
			this.polygons=new Polygon[this.multipolygon.getNumGeometries()];
			this.envelopes=new Geometry[this.multipolygon.getNumGeometries()];
			for(int n=0;n<this.polygons.length;n++){
				this.polygons[n]=(Polygon) this.multipolygon.getGeometryN(n);
				this.envelopes[n]=this.polygons[n].getEnvelope();
			}
		}
		
		public boolean contains(com.vividsolutions.jts.geom.Point pt){
			for(int n=0;n<this.polygons.length;n++){
				if(this.envelopes[n].contains(pt) && this.polygons[n].contains(pt)){
					return true;
				}
			}
			return false;
		}
	}
	
	public GeosPixels maskIt(MultiPolygon multipolygon){	
		
		GeosPixels geofref_coord=new GeosPixels();
		if(multipolygon == null){
			System.out.println("!");
		}
		geofref_coord.multipolygon=multipolygon;
		geofref_coord.loci=new ArrayList<>();
		System.out.println("1");
		//int index=0;
			
		Insider insider=new Insider(multipolygon);
		for(Iterator<Locus> it=this.loci.iterator();it.hasNext();){
			Locus locus=it.next();
			Point point=locus.getPoint();
			Coordinate coordinate=new Coordinate(point.getLongitude(),point.getLatitude());
			//boolean include=multipolygon.intersects(gf.createPoint(coordinate));
			//System.out.println(index+" offf "+this.loci.size());
			//index++;
			com.vividsolutions.jts.geom.Point pt=GeosPixels.gf.createPoint(coordinate);
			//if(multipolygon.contains(pt)){
			if(insider.contains(pt)){
				geofref_coord.loci.add(locus);
				it.remove();
			}
		}			
		System.out.println("2");
		
		return geofref_coord;
	}
	
	public  static double[] readBand(String filename, String bandName)
			throws IOException {

		Product product = ProductIO.readProduct(filename);
		int W = product.getSceneRasterWidth();
		int H = product.getSceneRasterHeight();
		int[] band_values = product.getBand(bandName).readPixels(0, 0, W, H,
				(int[]) null);

		MetadataElement el = product.getMetadataRoot()
				.getElement("Variable_Attributes").getElement(bandName);
		int missing_value = el.getAttributeInt("MISSING_VALUE");
		int scaling_factor = el.getAttributeInt("SCALING_FACTOR");

		System.out.println(bandName + " has missing_value = " + missing_value
				+ " scaling_factor=" + scaling_factor);
		double[] vals = new double[band_values.length];

		for (int i = 0; i < band_values.length; i++) {
			if (band_values[i] == missing_value) {
				vals[i] = Double.NaN;
			} else {
				vals[i] = ((double) band_values[i]) / ((double) scaling_factor);
			}
		}

		product.closeProductReader();
		return vals;
	}
	
	public static Map<String,MultiPolygon> loadCountries(String shapefile) throws IOException 
	{
	    File file = new File(shapefile);
	    Map<String, Serializable> map = new HashMap<>();
	    map.put( "url", file.toURI().toURL() );

	    DataStore dataStore = DataStoreFinder.getDataStore( map );
	    String typeName = dataStore.getTypeNames()[0];

	    FeatureSource source = dataStore.getFeatureSource( typeName );

	    FeatureCollection collection =  source.getFeatures();
	    FeatureIterator<SimpleFeature> results = collection.features();
	    Map<String,com.vividsolutions.jts.geom.MultiPolygon> countries=new HashMap<>();
	    try {
	        while (results.hasNext()) {
	        	MultiPolygon multipolygon=null;
	        	String name=null;
	            SimpleFeature feature = (SimpleFeature) results.next();
	            
	            //System.out.println("ZZ "+feature.getID());
	            for(Object ff :feature.getAttributes()){
	            	
	            	if(ff instanceof com.vividsolutions.jts.geom.MultiPolygon){
	            		multipolygon=(MultiPolygon) ff;
	            	} else if(ff instanceof java.lang.String ){
	            		name=(String) ff;
	            	}            	
	            }
	            
	            if(name.length()==2){	           
	        		countries.put(name, multipolygon);
	            }
	            //String code = feature.getAttribute("Codigo_SSC").toString();
	            //System.out.println( code );
	           //System.exit(0);
	        }
	    } finally {
	        results.close();
	    }
	    //dataStore.dispose();
	    for(Entry<String,MultiPolygon> entryyy : countries.entrySet()){
			System.out.println("Country : "+entryyy.getKey());
	    }
	    return countries;
	}
	
	
	public static Map<String,GeosPixels>  maskCountries(String shapefile,GeosPixels geofref_coords) throws IOException {
		Map<String,MultiPolygon> countries=GeosPixels.loadCountries(shapefile);
		
		Map<String,GeosPixels> mapper=new HashMap<>();
		for(Map.Entry<String,MultiPolygon> countryinfo : countries.entrySet()){
			GeosPixels temp = geofref_coords.maskIt(countryinfo.getValue());
			mapper.put(countryinfo.getKey(),temp);
		}
		return mapper;
	}
	
}