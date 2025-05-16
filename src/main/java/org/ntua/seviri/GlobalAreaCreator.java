package org.ntua.seviri;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.ntua.generic.DataStructures.Locus;
import org.ntua.seviri.model.GeosPixels;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.locationtech.jts.geom.Coordinate;

public class GlobalAreaCreator {

	public static void main(String[] args) {
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
				double s2 = sn * sinx * cosy;
				double s3 = -sn * siny;
				double sxy = Math.sqrt(s1 * s1 + s2 * s2);
				double glon = Math.toDegrees(Math.atan(s2 / s1));
				double glat = Math.toDegrees(Math.atan(p2 * (s3 / sxy)));
				// System.out.println(glat+";"+glon);
				Coordinate point = new Coordinate(glon, glat);
				Locus locus = new Locus(point, index++);
				// if ( (locus.coordinate.y > 40) && (locus.coordinate.y < 45) &&
				// (locus.coordinate.x > 11) && (locus.coordinate.x < 13) ) {
				// System.out.println(locus.coordinate.x + "<---->" + locus.coordinate.y);
				// }
				extra_coord.loci.add(locus);
			}
		}

		try {
			String csvFile = args[0];
			File f = new File(csvFile);
			if (!f.exists() || f.isDirectory()) {
				throw new RuntimeException("Bad file");
			}
			StringWriter sw = new StringWriter();
			CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
					.setHeader(new String[]{"LON", "LAT"})
					.build();
				try (final CSVPrinter printer = new CSVPrinter(sw, csvFormat)) {
					for (Locus locus : extra_coord.loci) {
						printer.printRecord(
								Double.toString(locus.coordinate.x),
								Double.toString(locus.coordinate.y)
						);
					}
				}
				Files.writeString(Paths.get(f.toURI()), sw.toString().trim());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
