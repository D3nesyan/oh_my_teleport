package com.ratoriku.ohmyteleport.data;

import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.ratoriku.ohmyteleport.OhMyTeleport;
import com.ratoriku.ohmyteleport.core.PlayerData;
import com.ratoriku.ohmyteleport.core.PlayerDataStore;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The mod's save file: {@code <world>/data/oh_my_teleport/players.dat} or similar, resolved by the
 * overworld's data storage.
 *
 * <p>Minecraft 26.x {@code SavedData} is codec-only and every mutation has to flag itself dirty,
 * otherwise nothing is written. To make that hard to forget, mutate through {@link #mutate}.
 */
public final class PlayerDataSavedData extends SavedData {

	private static final Codec<PlayerDataSavedData> CODEC = PlayerDataCodec.CODEC
			.xmap(PlayerDataSavedData::of, PlayerDataSavedData::store);

	public static final SavedDataType<PlayerDataSavedData> TYPE = new SavedDataType<>(
			OhMyTeleport.id("players"),
			PlayerDataSavedData::new,
			CODEC,
			null);

	private final PlayerDataStore store = new PlayerDataStore();

	private PlayerDataSavedData() {
	}

	/** The overworld holds the single copy of this data. */
	public static PlayerDataSavedData get(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		return overworld.getDataStorage().computeIfAbsent(TYPE);
	}

	public PlayerData dataFor(UUID playerId) {
		return store.forPlayer(playerId);
	}

	/** Applies a change and marks the data dirty so it reaches disk. */
	public void mutate(UUID playerId, Consumer<PlayerData> action) {
		action.accept(store.forPlayer(playerId));
		setDirty();
	}

	private static PlayerDataSavedData of(PlayerDataStore decoded) {
		PlayerDataSavedData data = new PlayerDataSavedData();
		decoded.asMap().forEach(data.store::putRaw);
		return data;
	}

	private PlayerDataStore store() {
		return store;
	}
}
