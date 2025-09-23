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
import org.apache.commons.csv.CSVPrinter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class WriterUtilities {
	
	private static final double MISSING_VALUE = -9999.0;
	
	
	public String netCDFFile;
	public double[][] readings;
	public String timing;
	public String[] bandNames;
	
	public void saveToNetCDF() throws IOException{
		Map<Double, Map<Double, Double[]>> pts = createSampledData();

		var latSet = pts.keySet();
		var lonSet = new HashSet<Double>();

		for (var temp : pts.values()) {
			lonSet.addAll(temp.keySet());
		}

		var latList = new ArrayList<Double>(latSet);
		latList.sort(Double::compare);

		var lonList = new ArrayList<Double>(lonSet);
		lonList.sort(Double::compare);

		int nx = latList.size();
		int ny = lonList.size();

		System.out.println(nx + "   " + ny);

		double[][] data = new double[nx * ny][bandNames.length];
		Arrays.fill(data, Double.NaN);

		int index = 0;
		for (var latIndex : latList) {
			for (var lonIndex : lonList) {
				var someValues = pts.get(latIndex).get(lonIndex);
				if (someValues == null) {
					for(int i=0;i<bandNames.length;i++) {
						data[index][i] = MISSING_VALUE;
					}
				} else {
					for(int i=0;i<bandNames.length;i++) {
						var someValue = someValues[i];
						data[index][i] = Double.isNaN(someValue) ? MISSING_VALUE : someValue;
					}
				}
				
				index++;
			}
		}

		double[] latArr = new double[nx];
		for (int i = 0; i < latList.size(); i++) {
			latArr[i] = latList.get(i);
		}

		double[] lonArr = new double[ny];
		for (int i = 0; i < lonList.size(); i++) {
			lonArr[i] = lonList.get(i);
		}

		// Build NetCDF in-memory
		Dimension latDim = Dimension.builder().setName("lat").setLength(nx).build();
		Dimension lonDim = Dimension.builder().setName("lon").setLength(ny).build();


		Array latCachedData = Array.factory(DataType.DOUBLE, new int[] { nx }, latArr);
		Array lonCachedData = Array.factory(DataType.DOUBLE, new int[] { ny }, lonArr);
		
		Array[] valueCachedData = new Array[bandNames.length];
		for(int i=0;i<bandNames.length;i++) {
			valueCachedData[i] = Array.factory(DataType.DOUBLE, new int[] { nx, ny }, data[i]);
		}

		NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(netCDFFile);
		builder.addDimension(lonDim);
		builder.addDimension(latDim);
		var lonVar = builder.addVariable("lon", DataType.DOUBLE, "lon");
		Attribute Longitude = new Attribute("long_name", "Longitude");
		lonVar.addAttribute(Longitude);
		Attribute lonUnits = new Attribute("units", "degrees_east");
		lonVar.addAttribute(lonUnits);
		Attribute lonCoordinateAxis = new Attribute("_CoordinateAxisType", "Lon");
		lonVar.addAttribute(lonCoordinateAxis);

		var latVar = builder.addVariable("lat", DataType.DOUBLE, "lat");
		Attribute latLongName = new Attribute("long_name", "Latitude");
		latVar.addAttribute(latLongName);
		Attribute latUnits = new Attribute("units", "degrees_north");
		latVar.addAttribute(latUnits);
		Attribute latCoordinateAxis = new Attribute("_CoordinateAxisType", "Lat");
		latVar.addAttribute(latCoordinateAxis);

		var valueVars = new Variable.Builder[bandNames.length];
		for(int i=0;i<bandNames.length;i++) {
			valueVars[i] = builder.addVariable(bandNames[i], DataType.DOUBLE, "lat lon");
			Attribute missingValue = new Attribute("missing_value", MISSING_VALUE);
			valueVars[i].addAttribute(missingValue);
		}

		try (NetcdfFormatWriter writer = builder.build()) {
			Variable someVar = writer.findVariable("lat");
			writer.write(someVar, latCachedData);
			someVar = writer.findVariable("lon");
			writer.write(someVar, lonCachedData);
			for(int i=0;i<bandNames.length;i++) {
				someVar = writer.findVariable(bandNames[i]);
				writer.write(someVar, valueCachedData[i]);
			}
		}catch(InvalidRangeException e) {
			throw new IOException("Invalid Range");
		}
	}
	
	
	private Map<Double, Map<Double, Double[]>> createSampledData() {
		Map<Double, Map<Double, Double[]>> sampledData = new HashMap<Double, Map<Double, Double[]>>();
		
		for(double[] reading : readings) {
            
            var lat = Double.valueOf(reading[bandNames.length]);
            var lon = Double.valueOf(reading[bandNames.length+1]);
            var values = new Double[bandNames.length];
            for(int i=0;i<bandNames.length;i++) {
            	values[i] = Double.valueOf(reading[i]);
            }
            
            var temp = sampledData.get(lat);
			if (temp != null) {
				temp.put(lon, values);
			} else {
				sampledData.put(lat, new HashMap<Double, Double[]>());
				sampledData.get(lat).put(lon, values);
			}
        }
		return sampledData;
	}
}
