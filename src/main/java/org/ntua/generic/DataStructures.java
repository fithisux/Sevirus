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


public class DataStructures {
	public static class Point {

		public double latitude;
		public double longitude;
		
		public Point clone(){
			return new Point(this.latitude,this.longitude);
		}
		
		public Point(double lat,double lon){
			this.latitude=lat;
			this.longitude=lon;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}
	}

	public static class Locus {

		public Point point;
		public int index;
		
		public Locus(Point point ,int index){
			this.point=point;
			this.index=index;
		}
		
		public Locus clone(){
			return new Locus(this.point.clone(),this.index);
		}

		public Point getPoint() {
			return point;
		}

		public int getIndex() {
			return index;
		}				
	}
	
	public static class Reading {

		public Locus locus;
		public double[] values;
		
		public Reading(Locus locus,double[] values){
			this.locus=locus;
			this.values=values;
		}
		
		public Reading clone(){
			return new Reading(this.locus.clone(),Arrays.copyOf(this.values, this.values.length));
		}

		public Locus getLocus() {
			return locus;
		}

		public double[] getValues() {
			return values;
		}
	}

	public static class Place {

		public Point point;
		public String name;
		
		public Place(Point point ,String name){
			this.point=point;
			this.name=name;
		}
		
		public Place clone(){
			return new Place(this.point.clone(),this.name);
		}

		public Point getPoint() {
			return point;
		}

		public String getName() {
			return name;
		}
			
	}
	
}
