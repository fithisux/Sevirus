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

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.nio.file.*;
import com.vividsolutions.jts.geom.*;
import org.ntua.ascat.model.*;
import org.ntua.generic.AbstractProcessor;
import org.ntua.generic.AuxiliaryInfo;
import org.ntua.generic.DataStructures.*;
import org.opengis.feature.simple.SimpleFeature;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.MetadataElement;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.esa.beam.util.ProductUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

public class GeosProcessor extends AbstractProcessor{

	public static class GenericExtFilter implements FilenameFilter {
		
		private String producttype;

		public GenericExtFilter(String producttype) {
			this.producttype = producttype;
		}

		public boolean accept(File dir, String name) {
			return (name.split("_")[3].equals(this.producttype));
		}
	}
	
	
	/*
	public static final String[][] bands = { { "ET", "ET_Q_Flag" },
			{ "FVC", "FVC_QF", "FVC_err" },
			{ "LST", "Q_FLAGS", "errorbar_LST" } };

*/
	
	
	public static class GeosException extends Exception {
		public GeosException(String what) {
			super(what);
		}
	}

	String static_geofolder;
	public Map<String,GeosPixels> area_mapper;
	
	public GeosProcessor(String static_geofolder) throws IOException{
		this.static_geofolder=static_geofolder;
		this.area_mapper=GeosPixels.loadCoordinates(static_geofolder);
	}
	

	public void doConversion(String fileName, String outputFolder,String country_name, GeosPixels country_pixels) throws GeosException {
		try {		
			AuxiliaryInfo info=this.readme(fileName,outputFolder,country_name,country_pixels);
			if(info == null){
				throw new GeosException("cannot find area");
			}	
			info.saveToCSV();			
			org.ntua.headless.RunCSVConverter.saveToDimap(info.fileName, info.fileName.substring(0, info.fileName.length()-3)+"dim");
		} catch (IOException e) {
			throw new GeosProcessor.GeosException(e.getMessage());
		}
	}

	
	
	public static AuxiliaryInfo toAuxiliaryInfo(double[][] bands,GeosPixels geos_pixels){
		List<Reading> readings=new ArrayList<>();
		for (Locus locus : geos_pixels.loci) {
			double[] values=new double[bands.length];
			for(int i=0;i<values.length;i++){
				values[i]=bands[i][locus.getIndex()];
			}
			
			readings.add(new Reading(locus,values));
		}
		
		
		AuxiliaryInfo info=new AuxiliaryInfo();
		info.readings=readings;
		info.fileName="";
		info.timing="";
		info.threshold=5;
		return info;
	}
	
	
	
	
	public AuxiliaryInfo readme(String fileName,String outputFolder,
			String country_name, GeosPixels country_pixels) throws IOException {
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

		GeosPixels geos_pixels=area_mapper.get(geographical_area);
		
		Product product = ProductIO.readProduct(fileName);
		String[] bandnames = product.getBandNames();
		product.closeProductReader();

		double[][] bands = new double[bandnames.length][];
		for (int k = 0; k < bandnames.length; k++) {
			bands[k] = GeosPixels.readBand(fileName, bandnames[k]);			
		}
		
		
				
		List<Reading> readings=new ArrayList<>();
		for (Locus locus : geos_pixels.loci) {
			double[] values=new double[bands.length];
			for(int i=0;i<values.length;i++){
				values[i]=bands[i][locus.getIndex()];
			}
			
			readings.add(new Reading(locus,values));
		}
		
		AuxiliaryInfo temp=GeosProcessor.toAuxiliaryInfo(bands, country_pixels);
		String countryTargetFilename = outputFolder + "/seviri_" + product_type + "_"
				+ geographical_area + "_" + temptiming + "_"+country_name+".csv";
		temp.fileName=countryTargetFilename;
		temp.bandnames=bandnames;
		return temp;
	}
	
	
}
