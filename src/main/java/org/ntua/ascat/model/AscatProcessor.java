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
package org.ntua.ascat.model;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.*;
import java.util.*;
import java.nio.file.*;

import org.ntua.generic.DataStructures.*;
import org.gavaghan.geodesy.*;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.esa.beam.util.ProductUtils;
import org.ntua.ascat.model.*;
import org.ntua.generic.AbstractProcessor;
import org.ntua.generic.AuxiliaryInfo;
import org.ntua.seviri.model.GeosPixels;

public class AscatProcessor extends AbstractProcessor{


	public static class GenericExtFilter implements FilenameFilter {

		private String ext;

		public GenericExtFilter(String ext) {
			this.ext = ext;
		}

		public boolean accept(File dir, String name) {
			return (name.endsWith(ext));
		}
	}

	

	public AuxiliaryInfo readme(String fileName,String outputFolder,
			String country_name, GeosPixels country_pixels) throws IOException {
		List<Locus> loci = new ArrayList<>();
		Product product = ProductIO.readProduct(fileName);
		int W = product.getSceneRasterWidth();
		int H = product.getSceneRasterHeight();
		double[] latValues = product.getBand("latitude").readPixels(0, 0, W, H,
				(double[]) null);
		double[] lonValues = product.getBand("longitude").readPixels(0, 0, W,
				H, (double[]) null);
		System.out.println("Reading a " + W + "X" + H + " raster");

		
		
		String[] bandNames=product.getBandNames();
		double[][] bands = new double[bandNames.length][];
		
		for(int k=0;k<bands.length;k++){
			Band temp_band = product.getBand(bandNames[k]);
			int index=0;			
			for(int i=0;i<W;i++){
				for(int j=0;j<H;j++){
					if(k==0){
						Point point = new Point(latValues[index],lonValues[index]);
						Locus locus = new Locus(point,index);
						loci.add(locus);
					}
					bands[k][index++]=Double.parseDouble(temp_band.getPixelString(i,j));
				}
			}

		}
		
		
		List<Reading> readings=new ArrayList<>();
		for (Locus locus : loci) {
			double[] values=new double[bands.length];
			for(int i=0;i<values.length;i++){
				values[i]=bands[i][locus.getIndex()];
			}
			
			readings.add(new Reading(locus,values));
		}
		
		
		product.closeProductReader();
		System.out.println("Raster done");
		
		String fileNameOnly = (new File(fileName)).getName();
		String temptiming = fileNameOnly.split("_")[4];
		System.out.println(temptiming);
		String timing = temptiming.substring(0, 4);
		timing += "-" + temptiming.substring(4, 6);
		timing += "-" + temptiming.substring(6, 8);
		timing += "T" + temptiming.substring(8, 10);
		timing += ":" + temptiming.substring(10, 12);
		timing += ":" + temptiming.substring(12, 14);
		timing += "Z";
		
		String tempspacing = fileNameOnly.split("_")[8];
		double spacing = Double.parseDouble(tempspacing);
		spacing /= 10 * Math.sqrt(2);
		
		
		AuxiliaryInfo info=new AuxiliaryInfo();
		info.readings=readings;
		info.fileName=fileName;
		info.bandnames=bandNames;
		info.timing=timing;
		info.threshold=spacing;
		return info;
	}
	
	
	
	

	
}
