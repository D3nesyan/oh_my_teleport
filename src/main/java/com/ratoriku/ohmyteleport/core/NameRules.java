package com.ratoriku.ohmyteleport.core;

import java.util.Locale;

/** Rules for naming homes and warps. Shared by the stores and by command argument parsing. */
public final class NameRules {

	/** Longest accepted name, after trimming. */
	public static final int MAX_LENGTH = 32;

	private NameRules() {
	}

	/**
	 * Trims, lowercases and validates a user supplied name.
	 *
	 * <p>Names are lowercased so that {@code /home Base} finds {@code /sethome base}.
	 *
	 * @return the usable name, or {@code null} when the input cannot be used as a name
	 */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
			return null;
		}
		for (int i = 0; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i);
			if (Character.isWhitespace(c) || Character.isISOControl(c)) {
				return null;
			}
		}
		return trimmed.toLowerCase(Locale.ROOT);
	}

	public static boolean isValid(String raw) {
		return normalize(raw) != null;
	}
}
