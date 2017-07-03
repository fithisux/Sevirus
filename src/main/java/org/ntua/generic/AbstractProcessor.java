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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ntua.generic.DataStructures.*;
import org.ntua.seviri.model.GeosPixels;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalPosition;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;



public abstract class AbstractProcessor {
	
	

	public static double vincenty4(Point point1, Point point2) {
		GeodeticCalculator geoCalc = new GeodeticCalculator();
		Ellipsoid reference = Ellipsoid.WGS84;
		GlobalPosition pointA = new GlobalPosition(point1.latitude,
				point1.longitude, 0.0);
		GlobalPosition userPos = new GlobalPosition(point2.latitude,
				point2.longitude, 0.0);
		double distance = geoCalc.calculateGeodeticCurve(reference, userPos,
				pointA).getEllipsoidalDistance();
		return distance / 1000;
	}
	
	public static Map<Place,double[]> scanFile(AuxiliaryInfo info, List<Place> places)
			throws IOException {
		System.out.println("Product : " + info.fileName);		
		Map<Place,double[]> assoc=new HashMap<>();		
		System.out.println("Start scanning for sites");
		for (Place place : places) {
			double distance = 0;
			double[] selected = null;
			for (double[] reading : info.readings) {
				Point readingPoint = new Point(reading[reading.length-2],reading[reading.length-1]);
				double temp = AbstractProcessor.vincenty4(place.point, readingPoint);
				if ((selected == null) || (temp < distance)) {
					distance = temp;
					selected = reading;
				}
			}

			if (distance <= info.threshold) {
				assoc.put(place, selected);
			}
		}
		System.out.println("Finished scanning for sites");
		return assoc;
	}
	
	String[] heads = null;
	
	Map<String,CsvWriter> outputs=new HashMap<>();
	
	public void filter(AuxiliaryInfo info,Map<Place,double[]> pin,String csvFile)
			throws IOException {
		if(pin.isEmpty()) return;		
		File f=new File(csvFile);
		CsvWriter csvOutput=null;
		if(f.exists() && !f.isDirectory()) {
			csvOutput = new CsvWriter(new FileWriter(csvFile, true),'\t');
			for(String bandname : info.bandnames){
				csvOutput.write(bandname);
			}		
			csvOutput.write("LAT");
			csvOutput.write("LON");		
			csvOutput.write("site_id");
			csvOutput.write("sample_time");
			csvOutput.write("filename");
			csvOutput.endRecord();
		} else {
			csvOutput=outputs.get(csvFile);
		}
		for(Map.Entry<Place, double[]> entry : pin.entrySet()) {
			for(double value : entry.getValue()) {
				csvOutput.write(Double.toString(value));
			}
			csvOutput.write(entry.getKey().name);
			csvOutput.write(info.timing);
			csvOutput.write(info.fileName);
			csvOutput.endRecord();				
		}			
	}

	
	public static List<Place> readPlaces(String fileName) throws IOException {
		List<Place> places = new ArrayList<>();
		System.out.println("start reading places");
		int index = 0;
		// read the places
		CsvReader csv_places = new CsvReader(
				(new File(fileName)).getAbsolutePath(), ',');
		csv_places.readHeaders();
		while (csv_places.readRecord()) {
			double lat = Double.parseDouble(csv_places.get("lat"));
			double lon = Double.parseDouble(csv_places.get("lon"));
			Point point=new Point(lat,lon);
			String name = "P" + Integer.toString(index++);
			Place place = new Place(point,name);
			places.add(place);
		}
		System.out.println("finished reading places");
		return places;
	}
	
	public abstract AuxiliaryInfo readme(String fileName,String outputFolder,
			String country_name, GeosPixels country_pixels) throws IOException;
	
	
	public void process(String fileName,List<Place> places,String csvFile) throws IOException{
		AuxiliaryInfo info=this.readme(fileName,"","",null);
		Map<Place,double[]> assoc=AbstractProcessor.scanFile(info, places);
		this.filter(info,assoc,csvFile);
		
	}	
	
	public void closeCSVs(){
		for(CsvWriter cc : this.outputs.values()){
			cc.close();
		}
	}
	
}
