package com.ratoriku.ohmyteleport.core;

/**
 * Readable names for dimensions, used as the English fallback of the dimension message.
 *
 * <p>Vanilla ships no translation key for dimension names, so the mod supplies its own for the
 * three built-in dimensions. Anything from another namespace keeps its full id, because dropping
 * the namespace could make two different dimensions look identical.
 *
 * <p>Pure string work, on purpose: the name rule is logic worth testing, and this way it can be
 * tested without loading a single Minecraft class.
 */
public final class DimensionNames {

	public static final String VANILLA_NAMESPACE = "minecraft";

	private DimensionNames() {
	}

	/**
	 * {@code minecraft:the_nether} becomes {@code The Nether}; {@code twilightforest:twilight_forest}
	 * stays as it is.
	 */
	public static String fallback(String dimensionId) {
		if (dimensionId == null || dimensionId.isEmpty()) {
			return "unknown";
		}

		int separator = dimensionId.indexOf(':');
		String namespace = separator < 0 ? VANILLA_NAMESPACE : dimensionId.substring(0, separator);
		String path = separator < 0 ? dimensionId : dimensionId.substring(separator + 1);

		if (!VANILLA_NAMESPACE.equals(namespace)) {
			return dimensionId;
		}
		String titleCased = titleCase(path);
		// An id with an empty path has no readable form; showing the raw id beats showing nothing.
		return titleCased.isEmpty() ? dimensionId : titleCased;
	}

	private static String titleCase(String path) {
		StringBuilder result = new StringBuilder(path.length());
		for (String word : path.split("_")) {
			if (word.isEmpty()) {
				continue;
			}
			if (!result.isEmpty()) {
				result.append(' ');
			}
			result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return result.toString();
	}
}
