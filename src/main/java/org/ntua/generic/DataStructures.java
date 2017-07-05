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

import java.util.Arrays;

import com.vividsolutions.jts.geom.Coordinate;


public class DataStructures {


	public static class Locus {

		public Coordinate coordinate;
		public int index;
		
		public Locus(Coordinate coordinate ,int index){
			this.coordinate=coordinate;
			this.index=index;
		}
		
		public Locus clone(){
			return new Locus((Coordinate) this.coordinate.clone(),this.index);
		}

					
	}
	
	public static class Place {

		public Coordinate coordinate;
		public String name;
		
		public Place(Coordinate coordinate ,String name){
			this.coordinate=coordinate;
			this.name=name;
		}
		
		public Place clone(){
			return new Place((Coordinate) this.coordinate.clone(),this.name);
		}

		
			
	}
	
}
