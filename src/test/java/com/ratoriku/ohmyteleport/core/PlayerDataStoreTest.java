package com.ratoriku.ohmyteleport.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerDataStoreTest {

	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void startsEmpty() {
		PlayerDataStore store = new PlayerDataStore();
		assertTrue(store.isEmpty());
		assertEquals(0, store.size());
	}

	@Test
	void forPlayerCreatesOnceAndThenReturnsTheSameInstance() {
		PlayerDataStore store = new PlayerDataStore();
		PlayerData first = store.forPlayer(ALICE);
		PlayerData second = store.forPlayer(ALICE);

		assertSame(first, second);
		assertEquals(1, store.size());
	}

	@Test
	void peekDoesNotCreate() {
		PlayerDataStore store = new PlayerDataStore();
		assertEquals(Optional.empty(), store.peek(BOB));
		assertTrue(store.isEmpty());
	}

	@Test
	void playersAreIndependent() {
		PlayerDataStore store = new PlayerDataStore();
		store.forPlayer(ALICE).homes().put("base", TeleportPoint.of("minecraft:overworld", 1, 2, 3));
		store.forPlayer(BOB).homes().put("base", TeleportPoint.of("minecraft:overworld", 9, 9, 9));

		assertEquals(2, store.size());
		assertEquals(TeleportPoint.of("minecraft:overworld", 1, 2, 3), store.forPlayer(ALICE).homes().get("base").orElseThrow());
		assertEquals(TeleportPoint.of("minecraft:overworld", 9, 9, 9), store.forPlayer(BOB).homes().get("base").orElseThrow());
	}

	@Test
	void putRawStoresWithoutCreatingDuplicates() {
		PlayerDataStore store = new PlayerDataStore();
		PlayerData data = new PlayerData();
		data.warps().put("mine", TeleportPoint.of("minecraft:overworld", 1, 2, 3));

		store.putRaw(ALICE, data);

		assertFalse(store.isEmpty());
		assertSame(data, store.forPlayer(ALICE));
	}

	@Test
	void warpsAndHomesAreSeparateNamespaces() {
		PlayerData data = new PlayerData();
		data.homes().put("base", TeleportPoint.of("minecraft:overworld", 1, 2, 3));

		assertFalse(data.warps().has("base"));
		assertTrue(data.homes().has("base"));
	}

	@Test
	void lastTeleportOriginStartsEmptyAndCanBeCleared() {
		PlayerData data = new PlayerData();
		assertEquals(Optional.empty(), data.lastTeleportOrigin());

		data.setLastTeleportOrigin(TeleportPoint.of("minecraft:the_nether", 4, 5, 6));
		assertEquals(Optional.of(TeleportPoint.of("minecraft:the_nether", 4, 5, 6)), data.lastTeleportOrigin());

		data.clearLastTeleportOrigin();
		assertEquals(Optional.empty(), data.lastTeleportOrigin());
	}
}
