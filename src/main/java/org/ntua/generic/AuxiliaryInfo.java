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
import java.util.*;
import org.ntua.generic.DataStructures.*;
import org.ntua.ascat.model.*;
import com.csvreader.CsvWriter;

public class AuxiliaryInfo {
	public String fileName;	
	public List<Reading> readings;
	public String timing;
	public double threshold;
	public String[] bandnames;
	
	public void saveToCSV() throws IOException{
		CsvWriter csvOutput = new CsvWriter(new FileWriter(this.fileName,false), '\t');		
		for(String bandname : bandnames){
			csvOutput.write(bandname);
		}		
		csvOutput.write("LAT");
		csvOutput.write("LON");		
		csvOutput.endRecord();			
		for(Reading reading : readings){
			for(int i=0;i<reading.values.length;i++){
				csvOutput.write(Double.toString(reading.values[i]));
			}
			csvOutput.write(Double.toString(reading.getLocus().getPoint().getLatitude()));
			csvOutput.write(Double.toString(reading.getLocus().getPoint().getLongitude()));
			csvOutput.endRecord();
		}
		csvOutput.close();

	}
}
