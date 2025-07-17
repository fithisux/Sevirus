package org.ntua.generic;

import ucar.ma2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.grid.*;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.Dimension;

import java.io.*;
import java.util.*;
import java.awt.geom.*;

public class CsvToGeoTiffNetcdf {

    static class Point { double lat, lon, val; }

    public static void main(String[] args) throws Exception {
        String csv = "data.csv", tif = "out.tif";
        List<Point> pts = readCSV(csv);

        // Define grid: fixed resolution
        double res = 0.01;
        double minLat = pts.stream().mapToDouble(p -> p.lat).min().getAsDouble();
        double maxLat = pts.stream().mapToDouble(p -> p.lat).max().getAsDouble();
        double minLon = pts.stream().mapToDouble(p -> p.lon).min().getAsDouble();
        double maxLon = pts.stream().mapToDouble(p -> p.lon).max().getAsDouble();

        int ny = (int)Math.ceil((maxLat - minLat)/res);
        int nx = (int)Math.ceil((maxLon - minLon)/res);

        float[] data = new float[nx * ny];
        int[] cnt = new int[nx * ny];
        Arrays.fill(data, Float.NaN);

        for (Point p : pts) {
            int i = (int)((p.lon - minLon)/res);
            int j = (int)((p.lat - minLat)/res);
            if (i>=0 && i<nx && j>=0 && j<ny) {
                int idx = j*nx + i;
                if (Float.isNaN(data[idx])) data[idx]=0;
                data[idx] += p.val;
                cnt[idx]++;
            }
        }
        for (int idx=0; idx<data.length; idx++) {
            if (cnt[idx]>0) data[idx] /= cnt[idx];
        }

        // Build NetCDF in-memory
        NetcdfDataset ds = NetcdfDataset.create(null, null, null, false, null);
        Dimension xDim = ds.addDimension(null, "lon", nx);
        Dimension yDim = ds.addDimension(null, "lat", ny);

        VariableDS lon = new VariableDS(ds, null, null, "lon", DataType.DOUBLE, "lon", "degree_east", null);
        lon.setDimensions("lon");
        lon.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nx}, makeAxis(minLon, res, nx)), false);
        ds.addVariable(null, lon);

        VariableDS lat = new VariableDS(ds, null, null, "lat", DataType.DOUBLE, "lat", "degree_north", null);
        lat.setDimensions("lat");
        lat.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ny}, makeAxis(minLat, res, ny)), false);
        ds.addVariable(null, lat);

        VariableDS var = new VariableDS(ds, null, null, "val", DataType.FLOAT, "lat lon", null, null);
        var.setDimensions("lat lon");
        var.setCachedData(Array.factory(DataType.FLOAT, new int[]{ny, nx}, data), false);
        ds.addVariable(null, var);

        ds.finish();

        GridDataset gds = GridDataset.open(ds);
        GridDatatype grid = gds.findGridDatatype("val");
        Array arr = grid.readDataSlice(0,0,0,0);

        // Write GeoTIFF
        GeotiffWriter writer = new GeotiffWriter(tif);
        writer.writeGrid(gds, grid, arr, false);
        writer.close();

        System.out.println("GeoTIFF saved to: " + tif);
    }

    static double[] makeAxis(double start, double inc, int len) {
        double[] a = new double[len];
        for (int i=0; i<len; i++) a[i] = start + inc*(i+0.5);
        return a;
    }

    static List<Point> readCSV(String f) throws IOException {
        List<Point> L = new ArrayList<>();
        BufferedReader r = new BufferedReader(new FileReader(f));
        r.readLine();
        String l;
        while ((l = r.readLine())!=null) {
            String[] p = l.split(",");
            L.add(new Point(){{ lat=Double.parseDouble(p[0]); lon=Double.parseDouble(p[1]); val=Double.parseDouble(p[2]); }});
        }
        r.close();
        return L;
    }
}
