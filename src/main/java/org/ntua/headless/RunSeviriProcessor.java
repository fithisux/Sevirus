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
package org.ntua.headless;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.ntua.generic.AbstractProcessor;
import org.ntua.generic.DataStructures.Place;
import org.ntua.seviri.model.GeosProcessor;
import org.ntua.seviri.model.GeosProcessor.GenericExtFilter;

public class RunSeviriProcessor {
	
	public static void main(String[] args) {
		// TODO code application logic here
		// args[0] the data folder
		// args[1] the csv of places
		// args[2] the output folder
		// args[4] static files

		/*
		String[] args=new String[5];
		
		args[0]="C:/OpenSourceSEVIRI/dataFolder";
		args[1]="C:/workspace/places.csv";
		args[2]="C:/workspace/";
		args[3]="C:/OpenSourceSEVIRI/Static_HDF5_Files";
		*/
		
		File dir = new File(args[0]);

		if (!dir.isDirectory()) {
			System.out.println("Directory does not exists : " + args[0]);
			return;
		}

		String[] fileList = dir.list();

		if (fileList.length == 0) {
			System.out.println("no files in dir");
			return;
		}
		
		
		

		try {
			GeosProcessor processor=new GeosProcessor(args[3]);
			List<Place> places=AbstractProcessor.readPlaces(args[1]);
			for(int i=0;i<fileList.length;i++){
				String fileName=args[0]+"/"+fileList[i];
        		String productType=fileName.split("_")[3];
        		String csvFile=args[2]+"/readings"+productType+".csv";
        		processor.process(fileName, places, csvFile);
				fileList[i]=args[0]+"/"+fileList[i];
			}			
			processor.closeCSVs();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
