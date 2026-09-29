package com.ratoriku.ohmyteleport.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A set of named teleport points belonging to one player.
 *
 * <p>Names are normalized through {@link NameRules} on every access, so lookups are
 * case-insensitive and whitespace tolerant.
 */
public final class PointStore {

	private final Map<String, TeleportPoint> points = new LinkedHashMap<>();

	public boolean isEmpty() {
		return points.isEmpty();
	}

	public int size() {
		return points.size();
	}

	public Optional<TeleportPoint> get(String name) {
		String key = NameRules.normalize(name);
		return key == null ? Optional.empty() : Optional.ofNullable(points.get(key));
	}

	public boolean has(String name) {
		return get(name).isPresent();
	}

	/**
	 * Stores a point under the given name.
	 *
	 * @return {@code true} when an existing point was replaced
	 * @throws IllegalArgumentException when the name is not usable
	 */
	public boolean put(String name, TeleportPoint point) {
		Objects.requireNonNull(point, "point");
		String key = NameRules.normalize(name);
		if (key == null) {
			throw new IllegalArgumentException("invalid point name: " + name);
		}
		return points.put(key, point) != null;
	}

	/** Stores a point without validation, for decoding persisted data. */
	public void putRaw(String name, TeleportPoint point) {
		points.put(name, point);
	}

	public Optional<TeleportPoint> remove(String name) {
		String key = NameRules.normalize(name);
		return key == null ? Optional.empty() : Optional.ofNullable(points.remove(key));
	}

	/** All names, alphabetically sorted so command output is stable. */
	public List<String> names() {
		return points.keySet().stream().sorted().toList();
	}

	/** Live view used when encoding to disk. */
	public Map<String, TeleportPoint> asMap() {
		return Collections.unmodifiableMap(points);
	}

	public void clear() {
		points.clear();
	}
}
