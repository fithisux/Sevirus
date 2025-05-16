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

public class AuxiliaryInfo {
	public String fileName;	
	public double[][] readings;
	public String timing;
	public double threshold;
	public String[] bandnames;
	
	public void saveToCSV() throws IOException{
		boolean firstTime=true;
		File file = new File(fileName);
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}
		String [] fieldValues = new String[bandnames.length+2];
		System.arraycopy(bandnames, 0, fieldValues, 0, bandnames.length);		
		fieldValues[bandnames.length] = "Lat";
		fieldValues[bandnames.length+1] = "Lon";		
		writeFields(fileOutputStream, fieldValues);		
		for(double[] reading : readings){			
			for(int i=0; i< fieldValues.length ; i++) {
				if(Double.isNaN(reading[i])) {					
					fieldValues[i]="0";
				} else {
					fieldValues[i] =Float.toString((float) reading[i]);
				}
				//fieldValues[i] =Double.toString(reading[i]);
			}
			
			if (firstTime) {
				for(String fieldValue : fieldValues){
					System.out.println(fieldValue);
				}
				firstTime = false;
			}
			writeFields(fileOutputStream, fieldValues);
		}
		fileOutputStream.close();

	}
	
	private void writeFields(FileOutputStream fileOutputStream, String [] fieldValues) throws IOException {
		boolean firstField = true;
		for(String fieldValue : fieldValues){
			if (firstField) {
				firstField = false;
			} else {
				fileOutputStream.write("\t".getBytes());
			}
			fileOutputStream.write(fieldValue.getBytes());			
		}
		fileOutputStream.write("\n".getBytes());
	}
}
