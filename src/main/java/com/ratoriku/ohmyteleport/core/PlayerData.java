package com.ratoriku.ohmyteleport.core;

import java.util.Optional;

/** Everything the mod persists for a single player. */
public final class PlayerData {

	private final PointStore homes = new PointStore();
	private final PointStore warps = new PointStore();
	private TeleportPoint lastTeleportOrigin;

	public PointStore homes() {
		return homes;
	}

	public PointStore warps() {
		return warps;
	}

	/** Where the player stood just before this mod last moved them. */
	public Optional<TeleportPoint> lastTeleportOrigin() {
		return Optional.ofNullable(lastTeleportOrigin);
	}

	public void setLastTeleportOrigin(TeleportPoint point) {
		this.lastTeleportOrigin = point;
	}

	public void clearLastTeleportOrigin() {
		this.lastTeleportOrigin = null;
	}
}
