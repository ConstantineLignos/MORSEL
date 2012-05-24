/*******************************************************************************
 * Copyright (C) 2012 Constantine Lignos
 * 
 * This file is a part of MORSEL.
 * 
 * MORSEL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * MORSEL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MORSEL.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.upenn.ircs.lignos.morsel;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Util {
	public static <T> T[] concatArrays(T[] first, T[] second) {
		  T[] result = Arrays.copyOf(first, first.length + second.length);
		  System.arraycopy(second, 0, result, first.length, second.length);
		  return result;
	}
	
	public static <T> List<T> concatCollections(Collection<T> first, Collection<T> second) {
		List<T> out = new LinkedList<T>(first);
		out.addAll(second);
		return out;
	}
	
	public static <T> List<T> truncateCollection(Collection<T> items, int max) {
		int curr = 0;
		List<T> out = new LinkedList<T>();
		for(T item: items) {
			if (++curr > max) {
				break;
			}
			out.add(item);
		}	
		return out;
	}
	
	static public String join(Collection<String> list, String delim) {
	   StringBuilder sb = new StringBuilder();
	   boolean first = true;
	   for (String item : list) {
	      if (first)
	         first = false;
	      else
	         sb.append(delim);
	      sb.append(item);
	   }
	   return sb.toString();
	}
}
