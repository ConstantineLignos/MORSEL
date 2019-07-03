/*
 * Copyright 2009-2019 Constantine Lignos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lignos.morsel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** General static utility functions. */
public class Util {
  /**
   * Concatenate two arrays of the same type.
   *
   * @param first the first array
   * @param second the second array
   * @return a new array containing the elements of the first and second arrays in order
   */
  public static <T> T[] concatArrays(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  /**
   * Concatenate the elements of two collections into a ArrayList.
   *
   * @param first the first collection
   * @param second the second collection
   * @return a new ArrayList containing the elements of the first and second collections in order
   */
  public static <T> List<T> concatCollections(Collection<T> first, Collection<T> second) {
    List<T> out = new ArrayList<>(first);
    out.addAll(second);
    return out;
  }

  /**
   * Create a new collection with the first items from a collection
   *
   * @param items source collection
   * @param max the number of items to keep
   * @return a new ArrayList containing the first items from the source collection
   */
  public static <T> List<T> truncateCollection(Collection<T> items, int max) {
    int curr = 0;
    List<T> out = new ArrayList<>();
    for (T item : items) {
      if (++curr > max) {
        break;
      }
      out.add(item);
    }
    return out;
  }

  /**
   * Join a collection of strings using a delimiter, like Python's join
   *
   * @param list Collection of strings to join
   * @param delim delimiter
   * @return a string containing the items joined by a delimiter
   */
  public static String join(Collection<String> list, String delim) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String item : list) {
      if (first) first = false;
      else sb.append(delim);
      sb.append(item);
    }
    return sb.toString();
  }
}
