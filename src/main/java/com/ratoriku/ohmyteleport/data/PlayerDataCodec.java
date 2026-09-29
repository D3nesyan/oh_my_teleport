package com.ratoriku.ohmyteleport.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ratoriku.ohmyteleport.core.PlayerData;
import com.ratoriku.ohmyteleport.core.PlayerDataStore;
import com.ratoriku.ohmyteleport.core.TeleportPoint;

/**
 * Serialization for the whole per-player data store.
 *
 * <p>Only DataFixerUpper primitives are used here, so this codec can be round-tripped in a plain
 * unit test through {@code JsonOps} without a running game. Minecraft 26.x dropped the old
 * {@code SavedData} NBT read/write pair, so a codec is the only way to persist anything.
 */
public final class PlayerDataCodec {

	private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(
			text -> {
				try {
					return DataResult.success(UUID.fromString(text));
				} catch (IllegalArgumentException exception) {
					return DataResult.error(() -> "not a UUID: " + text);
				}
			},
			UUID::toString);

	private static final Codec<TeleportPoint> POINT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("dimension").forGetter(TeleportPoint::dimensionId),
			Codec.DOUBLE.fieldOf("x").forGetter(TeleportPoint::x),
			Codec.DOUBLE.fieldOf("y").forGetter(TeleportPoint::y),
			Codec.DOUBLE.fieldOf("z").forGetter(TeleportPoint::z),
			Codec.FLOAT.fieldOf("yaw").forGetter(TeleportPoint::yaw),
			Codec.FLOAT.fieldOf("pitch").forGetter(TeleportPoint::pitch))
			.apply(instance, TeleportPoint::new));

	private static final Codec<PlayerData> PLAYER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, POINT_CODEC).fieldOf("homes").forGetter(data -> data.homes().asMap()),
			Codec.unboundedMap(Codec.STRING, POINT_CODEC).fieldOf("warps").forGetter(data -> data.warps().asMap()),
			POINT_CODEC.optionalFieldOf("last_teleport_origin").forGetter(PlayerData::lastTeleportOrigin))
			.apply(instance, PlayerDataCodec::buildPlayerData));

	private static final Codec<PlayerEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUID_CODEC.fieldOf("uuid").forGetter(PlayerEntry::id),
			PLAYER_CODEC.fieldOf("data").forGetter(PlayerEntry::data))
			.apply(instance, PlayerEntry::new));

	public static final Codec<PlayerDataStore> CODEC = ENTRY_CODEC.listOf()
			.xmap(PlayerDataCodec::toStore, PlayerDataCodec::fromStore);

	private PlayerDataCodec() {
	}

	private record PlayerEntry(UUID id, PlayerData data) {
	}

	private static PlayerData buildPlayerData(Map<String, TeleportPoint> homes, Map<String, TeleportPoint> warps,
			java.util.Optional<TeleportPoint> lastTeleportOrigin) {
		PlayerData data = new PlayerData();
		homes.forEach(data.homes()::putRaw);
		warps.forEach(data.warps()::putRaw);
		lastTeleportOrigin.ifPresent(data::setLastTeleportOrigin);
		return data;
	}

	private static PlayerDataStore toStore(List<PlayerEntry> entries) {
		PlayerDataStore store = new PlayerDataStore();
		for (PlayerEntry entry : entries) {
			store.putRaw(entry.id(), entry.data());
		}
		return store;
	}

	private static List<PlayerEntry> fromStore(PlayerDataStore store) {
		List<PlayerEntry> entries = new ArrayList<>();
		store.asMap().forEach((id, data) -> entries.add(new PlayerEntry(id, data)));
		return entries;
	}
}
