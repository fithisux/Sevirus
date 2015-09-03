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
package org.ntua.headless;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoCodingFactory;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import java.util.*;
import org.esa.beam.framework.gpf.GPF;
import java.io.*;

public class RunCSVConverter {
	public static void main(String[] args){
		
		try{
			RunCSVConverter.saveToDimap("C:/img2/seviri_FVC_Euro_201108030000_FR.csv", "C:/img2/blahblah.dim");		
		} catch(IOException e){
			e.printStackTrace();
		}		
	}
	
	
	public static void saveToDimap(String infile,String outfile) throws IOException{		
			String filename=infile;
			Product product = ProductIO.readProduct(new File(filename),"CSV");
			Band latband=product.getBand("LAT");
			Band lonband=product.getBand("LON");
			String validmask="";
			int searchRadius = 6;
			
			final GeoCoding pixelGeoCoding =
					GeoCodingFactory.createPixelGeoCoding(latband,
					lonband,
					validmask,
	                searchRadius);
			
			
			product.setGeoCoding(pixelGeoCoding);
			
			
			Map<String, Object> parameterMap=new HashMap<>();
			
			parameterMap.put("preserveResolution ", true);
			parameterMap.put("resamplingName", "Bicubic");
	        parameterMap.put("includeTiePointGrids", true);
	        parameterMap.put("addDeltaBands", false);
	        parameterMap.put("noDataValue", Double.NaN);
	        
	        String wkt = 
	                "  GEOGCS[\"WGS84(DD)\", \n" +
	                "    DATUM[\"WGS84\", \n" +
	                "      SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], \n" +
	                "    PRIMEM[\"Greenwich\", 0.0], \n" +
	                "    UNIT[\"degree\", 0.017453292519943295], \n" +
	                "    AXIS[\"Longitude\", EAST], \n" +
	                "    AXIS[\"Latitude\", NORTH]]";
	        
	        parameterMap.put("crs", wkt);
	        
	        Map<String, Product> productMap = new HashMap<>();
	        productMap.put("source", product);
	        
	        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
	        Product output=GPF.createProduct("Reproject", parameterMap, productMap);
	        
	        ProductIO.writeProduct(output, outfile,  "BEAM-DIMAP");			
	}
}
