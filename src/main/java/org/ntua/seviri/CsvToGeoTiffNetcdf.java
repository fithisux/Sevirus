package org.ntua.seviri;

import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.*;
import java.util.*;
import java.awt.geom.*;

public class CsvToGeoTiffNetcdf {
	
	private static final double MISSING_VALUE = -9999.0;

	record Point(double lat, double lon) {
	};

	public static void main(String[] args) throws Exception {
		String input_csv = "C:\\work\\gpetr\\out_msg\\seviri_ET_MSG-Disk_202308080030.unknown_ES.csv";
		String output_nc = "C:\\work\\gpetr\\out_msg\\seviri_ET_MSG-Disk_202308080030.unknown_ES.nc";
		Map<Double, Map<Double, Double>> pts = readCSV(input_csv);

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

		double[] data = new double[nx * ny];
		Arrays.fill(data, Double.NaN);

		int index = 0;
		for (var latIndex : latList) {
			for (var lonIndex : lonList) {
				var someValue = pts.get(latIndex).get(lonIndex);
				if (someValue == null) {
					data[index++] = MISSING_VALUE;
				} else {
					//System.out.println(index + "  " + latIndex + "  " + lonIndex);
					data[index++] = Double.isNaN(someValue) ? MISSING_VALUE : someValue;
				}
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
		Array valueCachedData = Array.factory(DataType.DOUBLE, new int[] { nx, ny }, data);

		NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(output_nc);
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

		var valueVar = builder.addVariable("val", DataType.DOUBLE, "lat lon");
		Attribute missingValue = new Attribute("missing_value", MISSING_VALUE);
		valueVar.addAttribute(missingValue);

		try (NetcdfFormatWriter writer = builder.build()) {
			Variable someVar = writer.findVariable("lat");
			writer.write(someVar, latCachedData);
			someVar = writer.findVariable("lon");
			writer.write(someVar, lonCachedData);
			someVar = writer.findVariable("val");
			writer.write(someVar, valueCachedData);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not write");
		}
	}

	static Map<Double, Map<Double, Double>> readCSV(String f) throws IOException {
		Map<Double, Map<Double, Double>> sampledData = new HashMap<Double, Map<Double, Double>>();
		BufferedReader r = new BufferedReader(new FileReader(f));
		r.readLine();
		String l;
		while ((l = r.readLine()) != null) {
			String[] p = l.split(",");
			var lat = Double.parseDouble(p[2]);
			var lon = Double.parseDouble(p[3]);
			var val = Double.parseDouble(p[0]);

			var temp = sampledData.get(lat);
			if (temp != null) {
				temp.put(lon, val);
			} else {
				sampledData.put(lat, new HashMap<Double, Double>());
				sampledData.get(lat).put(lon, val);
			}
		}
		r.close();
		return sampledData;
	}
}
