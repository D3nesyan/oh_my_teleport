package com.ratoriku.ohmyteleport.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.ratoriku.ohmyteleport.core.PlayerDataStore;
import com.ratoriku.ohmyteleport.core.TeleportPoint;

import org.junit.jupiter.api.Test;

/**
 * Round-trips the persistence codec through {@link JsonOps}, which needs no game classes - so this
 * runs as a plain unit test even though the codec is what actually writes the world save file.
 */
class PlayerDataCodecTest {

	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Test
	void emptyStoreRoundTrips() {
		PlayerDataStore store = new PlayerDataStore();

		PlayerDataStore decoded = encodeThenDecode(store);

		assertTrue(decoded.isEmpty());
	}

	@Test
	void homesWarpsAndOriginRoundTrip() {
		PlayerDataStore store = new PlayerDataStore();
		store.forPlayer(ALICE).homes().put("base", new TeleportPoint("minecraft:overworld", 1.5, 64.0, -2.5, 90.0f, 10.0f));
		store.forPlayer(ALICE).homes().put("mine", new TeleportPoint("minecraft:the_nether", 7.0, 30.0, 8.0, 0.0f, 0.0f));
		store.forPlayer(ALICE).warps().put("farm", TeleportPoint.of("minecraft:overworld", 100.0, 70.0, 200.0));
		store.forPlayer(ALICE).setLastTeleportOrigin(TeleportPoint.of("minecraft:the_end", 0.0, 64.0, 0.0));

		PlayerDataStore decoded = encodeThenDecode(store);

		assertEquals(1, decoded.size());
		assertEquals(TeleportPoint.of("minecraft:the_end", 0.0, 64.0, 0.0),
				decoded.forPlayer(ALICE).lastTeleportOrigin().orElseThrow());
		assertEquals(2, decoded.forPlayer(ALICE).homes().size());
		assertEquals(new TeleportPoint("minecraft:overworld", 1.5, 64.0, -2.5, 90.0f, 10.0f),
				decoded.forPlayer(ALICE).homes().get("base").orElseThrow());
		assertEquals(TeleportPoint.of("minecraft:overworld", 100.0, 70.0, 200.0),
				decoded.forPlayer(ALICE).warps().get("farm").orElseThrow());
	}

	@Test
	void multiplePlayersRoundTripIndependently() {
		UUID bob = UUID.fromString("00000000-0000-0000-0000-000000000002");
		PlayerDataStore store = new PlayerDataStore();
		store.forPlayer(ALICE).warps().put("a", TeleportPoint.of("minecraft:overworld", 1.0, 2.0, 3.0));
		store.forPlayer(bob).warps().put("b", TeleportPoint.of("minecraft:overworld", 4.0, 5.0, 6.0));

		PlayerDataStore decoded = encodeThenDecode(store);

		assertEquals(2, decoded.size());
		assertEquals(TeleportPoint.of("minecraft:overworld", 1.0, 2.0, 3.0), decoded.forPlayer(ALICE).warps().get("a").orElseThrow());
		assertEquals(TeleportPoint.of("minecraft:overworld", 4.0, 5.0, 6.0), decoded.forPlayer(bob).warps().get("b").orElseThrow());
		assertEquals(Optional.empty(), decoded.forPlayer(bob).lastTeleportOrigin());
	}

	@Test
	void storedDataSurvivesAnAbsentOptionalField() {
		PlayerDataStore store = new PlayerDataStore();
		store.forPlayer(ALICE).homes().put("base", TeleportPoint.of("minecraft:overworld", 1.0, 2.0, 3.0));

		JsonElement encoded = PlayerDataCodec.CODEC.encodeStart(JsonOps.INSTANCE, store).getOrThrow();
		PlayerDataStore decoded = PlayerDataCodec.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

		assertEquals(Optional.empty(), decoded.forPlayer(ALICE).lastTeleportOrigin());
	}

	private static PlayerDataStore encodeThenDecode(PlayerDataStore store) {
		JsonElement encoded = PlayerDataCodec.CODEC.encodeStart(JsonOps.INSTANCE, store).getOrThrow();
		return PlayerDataCodec.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
	}
}
