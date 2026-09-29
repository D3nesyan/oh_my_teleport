package com.ratoriku.ohmyteleport.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class PointStoreTest {

	private static TeleportPoint point(double x) {
		return new TeleportPoint("minecraft:overworld", x, 64.0, 0.0, 0.0f, 0.0f);
	}

	@Test
	void startsEmpty() {
		PointStore store = new PointStore();
		assertTrue(store.isEmpty());
		assertEquals(0, store.size());
		assertEquals(List.of(), store.names());
	}

	@Test
	void putThenGetReturnsThePoint() {
		PointStore store = new PointStore();
		assertFalse(store.put("base", point(1.0)));

		assertEquals(Optional.of(point(1.0)), store.get("base"));
		assertTrue(store.has("base"));
	}

	@Test
	void putReturnsTrueWhenItReplacedAnExistingPoint() {
		PointStore store = new PointStore();
		store.put("base", point(1.0));

		assertTrue(store.put("base", point(2.0)));
		assertEquals(Optional.of(point(2.0)), store.get("base"));
		assertEquals(1, store.size());
	}

	@Test
	void lookupIsCaseInsensitiveAndTrims() {
		PointStore store = new PointStore();
		store.put("Base", point(1.0));

		assertEquals(Optional.of(point(1.0)), store.get("base"));
		assertEquals(Optional.of(point(1.0)), store.get("  BASE  "));
		assertEquals(List.of("base"), store.names());
	}

	@Test
	void getMissingNameIsEmpty() {
		PointStore store = new PointStore();
		assertEquals(Optional.empty(), store.get("nope"));
		assertFalse(store.has("nope"));
	}

	@Test
	void removeReturnsTheRemovedPointAndIsIdempotent() {
		PointStore store = new PointStore();
		store.put("base", point(1.0));

		assertEquals(Optional.of(point(1.0)), store.remove("base"));
		assertTrue(store.isEmpty());
		assertEquals(Optional.empty(), store.remove("base"));
	}

	@Test
	void namesAreSortedForStableOutput() {
		PointStore store = new PointStore();
		store.put("zeta", point(1.0));
		store.put("alpha", point(2.0));
		store.put("mid", point(3.0));

		assertEquals(List.of("alpha", "mid", "zeta"), store.names());
	}

	@Test
	void rejectsInvalidNamesOnPut() {
		PointStore store = new PointStore();
		assertThrows(IllegalArgumentException.class, () -> store.put("", point(1.0)));
		assertThrows(IllegalArgumentException.class, () -> store.put(null, point(1.0)));
		assertThrows(IllegalArgumentException.class, () -> store.put("a b", point(1.0)));
	}

	@Test
	void clearRemovesEverything() {
		PointStore store = new PointStore();
		store.put("a", point(1.0));
		store.put("b", point(2.0));

		store.clear();
		assertTrue(store.isEmpty());
	}

	@Test
	void asMapExposesNormalizedNamesAndMapIsLive() {
		PointStore store = new PointStore();
		store.put("Base", point(1.0));

		assertEquals(java.util.Map.of("base", point(1.0)), store.asMap());
	}
}
