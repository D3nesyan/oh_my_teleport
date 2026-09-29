package com.ratoriku.ohmyteleport.server;

import java.util.Optional;

import com.ratoriku.ohmyteleport.core.TeleportPoint;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Translation between Minecraft's world types and the mod's own {@link TeleportPoint}. */
public final class TeleportPoints {

	private TeleportPoints() {
	}

	/**
	 * The server a player is on.
	 *
	 * <p>26.3 has no {@code Entity.getServer()}, so this goes through the level they are in.
	 */
	public static MinecraftServer serverOf(ServerPlayer player) {
		return player.level().getServer();
	}

	/** Where the player is standing right now. */
	public static TeleportPoint of(ServerPlayer player) {
		ServerLevel level = player.level();
		return new TeleportPoint(
				level.dimension().identifier().toString(),
				player.getX(), player.getY(), player.getZ(),
				player.getYRot(), player.getXRot());
	}

	/** The player's vanilla death location, if they have died since last using it. */
	public static Optional<TeleportPoint> deathPoint(ServerPlayer player) {
		return player.getLastDeathLocation().map(globalPos -> {
			Vec3 center = Vec3.atBottomCenterOf(globalPos.pos());
			return new TeleportPoint(
					globalPos.dimension().identifier().toString(),
					center.x, center.y, center.z,
					player.getYRot(), player.getXRot());
		});
	}

	/** @return the level, or {@code null} when that dimension is not loaded or the id is invalid */
	public static ServerLevel levelOf(MinecraftServer server, TeleportPoint point) {
		Identifier identifier = Identifier.tryParse(point.dimensionId());
		if (identifier == null) {
			return null;
		}
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, identifier);
		return server.getLevel(key);
	}

	/** The only place in this mod that moves a player. */
	public static void move(ServerPlayer player, ServerLevel level, TeleportPoint point) {
		TeleportTransition transition = new TeleportTransition(
				level,
				new Vec3(point.x(), point.y(), point.z()),
				player.getDeltaMovement(),
				point.yaw(),
				point.pitch(),
				TeleportTransition.DO_NOTHING);
		player.teleport(transition);
	}
}
