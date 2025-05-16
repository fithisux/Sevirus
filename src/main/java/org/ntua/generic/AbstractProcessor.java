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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javafx.scene.control.Alert;
import org.ntua.generic.DataStructures.*;
import org.ntua.seviri.model.GeosPixels;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalPosition;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;



public abstract class AbstractProcessor {
	
	

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
	
	public static Map<Place,double[]> scanFile(AuxiliaryInfo info, List<Place> places)
			throws IOException {
		System.out.println("Product : " + info.fileName);		
		Map<Place,double[]> assoc=new HashMap<>();		
		System.out.println("Start scanning for sites");
		for (Place place : places) {
			double distance = 0;
			double[] selected = null;
			for (double[] reading : info.readings) {
				Coordinate readingPoint = new Coordinate(reading[reading.length-1],reading[reading.length-2]);
				double temp = AbstractProcessor.vincenty4(place.coordinate, readingPoint);
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

	public void filter(AuxiliaryInfo info,Map<Place,double[]> pin,String csvFile)
			throws IOException {
		if(pin.isEmpty()) return;		
		File f=new File(csvFile);

		if(!f.isFile() || f.isDirectory()) return;

		StringWriter sw = new StringWriter();
		CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
				.setHeader(new String[]{"LAT", "LON", "site_id", "sample_time", "filename"})
				.build();

		try {
			try (final CSVPrinter printer = new CSVPrinter(sw, csvFormat)) {
				for (Map.Entry<Place, double[]> entry : pin.entrySet()) {
					printer.printRecord(
							Double.toString(entry.getValue()[0]),
							Double.toString(entry.getValue()[1]),
							entry.getKey().name,
							info.timing,
							info.fileName
					);
				}
			}
		} catch (IOException ex) {
		ex.printStackTrace();;
//		Alert dlg = new Alert(Alert.AlertType.ERROR, "");
//		dlg.setTitle("File problem.");
//		dlg.getDialogPane().setContentText("Could not save data to file:\n" + f.getPath());
//		dlg.initOwner(this.primaryStage);
//		dlg.showAndWait();
	}
	}

	
	public static List<Place> readPlaces(String fileName) throws IOException {
		var f = new File(fileName);
		List<Place> places = new ArrayList<>();
		System.out.println("start reading places");
		int index = 0;
		// read the places

		CSVFormat csvFormat = CSVFormat.RFC4180.withSkipHeaderRecord(true).builder()
				.setHeader(new String[]{"lat", "lon"})
				.build();

		try (
				Reader reader = Files.newBufferedReader(Paths.get(f.toURI())); CSVParser csvParser = new CSVParser(reader, csvFormat);) {
			for (CSVRecord csvRecord : csvParser) {

				double lat = Double.parseDouble(csvRecord.get("lat"));
				double lon = Double.parseDouble(csvRecord.get("lon"));
				Coordinate coordinate = new Coordinate(lon, lat);
				String name = "P" + Integer.toString(index++);
				Place place = new Place(coordinate, name);
				places.add(place);
			}
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
}
