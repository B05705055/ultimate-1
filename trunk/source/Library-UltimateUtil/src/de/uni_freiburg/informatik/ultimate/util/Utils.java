package de.uni_freiburg.informatik.ultimate.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Utils {

	/**
	 * Converts a number of bytes to a human readable String containing the byte
	 * number as the highest compatible unit.
	 * 
	 * @param bytes
	 *            A number of bytes
	 * @param si
	 *            true iff SI units should be used (base 1000, without the "i")
	 * @return
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public static String humanReadableNumber(long number) {
		int unit = 1000 ;
		if (number < unit)
			return number + "";
		int exp = (int) (Math.log(number) / Math.log(unit));
		String pre = ("KMGTPE").charAt(exp - 1)+"";
		return String.format("%.1f %s", number / Math.pow(unit, exp), pre);
	}

	/***
	 * Returns a String representation of a collection by calling toString on
	 * each object in the collection.
	 * 
	 * @param collection
	 * @param delimiter
	 * @return
	 */
	public static String join(Collection<?> collection, String delimiter) {
		StringBuilder builder = new StringBuilder();
		Iterator<?> iter = collection.iterator();
		while (iter.hasNext()) {
			builder.append(iter.next());
			if (!iter.hasNext()) {
				break;
			}
			builder.append(delimiter);
		}
		return builder.toString();
	}

	/**
	 * Returns a String representation of time as a fraction of the largest
	 * whole unit.
	 * 
	 * I.e. 1001ms becomes 1,001s, 25h become 1,041d.
	 * 
	 * @param time
	 *            The amount of time
	 * @param unit
	 *            The unit of the amount.
	 * @param decimal
	 *            The decimal accurracy of the ouptut.
	 * @return A String with unit symbol.
	 */
	public static String humanReadableTime(long time, TimeUnit unit, int decimal) {
		return humanReadableTime((double) time, unit, decimal);
	}

	/**
	 * Returns a String representation of time as a fraction of the largest
	 * whole unit.
	 * 
	 * I.e. 1001ms becomes 1,001s, 25h become 1,041d.
	 * 
	 * @param time
	 *            The amount of time
	 * @param unit
	 *            The unit of the amount.
	 * @param decimal
	 *            The decimal accurracy of the ouptut.
	 * @return A String with unit symbol.
	 */
	public static String humanReadableTime(double time, TimeUnit unit, int decimal) {
		String[] units = { "ns", "µs", "ms", "s", "m", "h", "d" };

		switch (unit) {
		case DAYS:
			return String.format("%." + decimal + "f %s", time, units[6]);
		case HOURS:
			if (time > 24) {
				return humanReadableTime(time / 24.0, TimeUnit.DAYS, decimal);
			} else {
				return String.format("%." + decimal + "f %s", time, units[5]);
			}
		case MINUTES:
			if (time > 60) {
				return humanReadableTime(time / 60.0, TimeUnit.HOURS, decimal);
			} else {
				return String.format("%." + decimal + "f %s", time, units[4]);
			}
		case SECONDS:
			if (time > 60) {
				return humanReadableTime(time / 60.0, TimeUnit.MINUTES, decimal);
			} else {
				return String.format("%." + decimal + "f %s", time, units[3]);
			}
		case MILLISECONDS:
			if (time > 1000) {
				return humanReadableTime(time / 1000.0, TimeUnit.SECONDS, decimal);
			} else {
				return String.format("%." + decimal + "f %s", time, units[2]);
			}
		case MICROSECONDS:
			if (time > 1000) {
				return humanReadableTime(time / 1000.0, TimeUnit.MILLISECONDS, decimal);
			} else {
				return String.format("%." + decimal + "f %s", time, units[1]);
			}
		case NANOSECONDS:
			if (time > 1000) {
				return humanReadableTime(time / 1000.0, TimeUnit.MICROSECONDS, decimal);
			} else {
				return String.format("%." + decimal + "f %s", time, units[0]);
			}
		default:
			throw new UnsupportedOperationException(unit + " TimeUnit not yet implemented");
		}
	}
	
	
	/**
	 * Filter Collection to all elements that are subclasses of clazz.
	 */
	@SuppressWarnings("unchecked")
	public static <E> Collection<E> filter(Collection<?> iterable, Class<E> clazz) {
		ArrayList<E> filteredList = new ArrayList<E>();
		for (Object e: iterable) {
			if (clazz.isAssignableFrom(e.getClass())) {
				filteredList.add((E) e);
			}
		}
		return filteredList;
	}
	
	/**
	 * Construct a new HashSet that contains the elements of a given Iterable.
	 */
	public static <E> HashSet<E> constructHashSet(Iterable<E> iterable) {
		HashSet<E> result = new HashSet<E>();
		for (E element : iterable) {
			result.add(element);
		}
		return result;
	}
	
	/**
	 * @return a new HashMap that contains all key-value pairs of map whose
	 * key is contained in filter.
	 */
	public static <K,V> HashMap<K,V> constructFilteredMap(Map<K,V> map, Collection<K> filter) {
		HashMap<K,V> result = new HashMap<>();
		for (K key : filter) {
			V value = map.get(key);
			if (value != null) {
				result.put(key, value);
			}
		}
		return result;
	}
}
