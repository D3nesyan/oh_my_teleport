package com.ratoriku.ohmyteleport.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Per-player data for every player the mod has seen, keyed by UUID. */
public final class PlayerDataStore {

	private final Map<UUID, PlayerData> byPlayer = new HashMap<>();

	/** Returns this player's data, creating an empty entry when there is none yet. */
	public PlayerData forPlayer(UUID playerId) {
		return byPlayer.computeIfAbsent(playerId, id -> new PlayerData());
	}

	public Optional<PlayerData> peek(UUID playerId) {
		return Optional.ofNullable(byPlayer.get(playerId));
	}

	/** Stores data without creating anything, for decoding persisted data. */
	public void putRaw(UUID playerId, PlayerData data) {
		byPlayer.put(playerId, data);
	}

	public Map<UUID, PlayerData> asMap() {
		return java.util.Collections.unmodifiableMap(byPlayer);
	}

	public boolean isEmpty() {
		return byPlayer.isEmpty();
	}

	public int size() {
		return byPlayer.size();
	}
}
