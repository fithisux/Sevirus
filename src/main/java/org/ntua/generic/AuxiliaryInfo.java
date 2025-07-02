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

import java.io.*;

public class AuxiliaryInfo {
	public String csvFile;
	public double[][] readings;
	public String timing;
	public double threshold;
	public String[] bandnames;

	public void saveToCSV() throws IOException {
        String [] fieldValues = new String[bandnames.length+2];
        System.arraycopy(bandnames, 0, fieldValues, 0, bandnames.length);
        fieldValues[bandnames.length] = "Lat";
        fieldValues[bandnames.length+1] = "Lon";

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(fieldValues.clone())
                .build();

        try (final CSVPrinter printer = new CSVPrinter(new FileWriter(csvFile), csvFormat)) {
            for(double[] reading : readings) {
                for(int i=0; i< fieldValues.length ; i++) {
                    fieldValues[i] = (Double.isNaN(reading[i])) ? "0" : Double.toString(reading[i]);
                }
                printer.printRecord(fieldValues);
            }
        }
	}
}
