package com.ratoriku.ohmyteleport.gametest;

import java.util.Optional;

import com.ratoriku.ohmyteleport.core.PlayerData;
import com.ratoriku.ohmyteleport.data.PlayerDataSavedData;
import com.ratoriku.ohmyteleport.server.TpaService;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * In-world tests. These drive the real commands through a real server level, so they cover the
 * parts a plain unit test cannot: command dispatch, the {@code SavedData} write-through and the
 * actual teleport call.
 *
 * <p>Run with {@code ./gradlew runGameTest}. The classes are inert unless the game is launched with
 * {@code -Dfabric-api.gametest}.
 */
public class TeleportGameTest {

	private static final String EMPTY_STRUCTURE = "fabric-gametest-api-v1:empty";

	private static final double TOLERANCE = 2.0;

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void sethomeThenHomeReturnsToTheSavedSpot(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 home = player.position();

		command(player, "sethome");

		PlayerData data = PlayerDataSavedData.get(level(player).getServer()).dataFor(player.getUUID());
		if (data.homes().get("home").isEmpty()) {
			helper.fail("sethome did not store a home");
			return;
		}

		player.teleportTo(home.x + 30.0, home.y, home.z + 30.0);
		command(player, "home");

		if (player.position().distanceTo(home) > TOLERANCE) {
			helper.fail("home did not move the player back: " + player.position() + " expected " + home);
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void namedHomesStaySeparate(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 first = player.position();

		command(player, "sethome one");
		player.teleportTo(first.x + 20.0, first.y, first.z);
		command(player, "sethome two");

		PlayerData data = PlayerDataSavedData.get(level(player).getServer()).dataFor(player.getUUID());
		if (data.homes().size() != 2) {
			helper.fail("expected two homes, found " + data.homes().size());
			return;
		}

		command(player, "home one");
		if (player.position().distanceTo(first) > TOLERANCE) {
			helper.fail("home one did not go to the first spot: " + player.position());
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void warpSetThenWarp(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 spot = player.position();

		command(player, "warp set farm");

		PlayerData data = PlayerDataSavedData.get(level(player).getServer()).dataFor(player.getUUID());
		if (data.warps().get("farm").isEmpty()) {
			helper.fail("warp set did not store a warp");
			return;
		}

		player.teleportTo(spot.x - 40.0, spot.y, spot.z);
		command(player, "warp farm");

		if (player.position().distanceTo(spot) > TOLERANCE) {
			helper.fail("warp did not move the player back: " + player.position());
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void deletingAWarpRemovesIt(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();

		command(player, "warp set gone");
		command(player, "warp del gone");

		PlayerData data = PlayerDataSavedData.get(level(player).getServer()).dataFor(player.getUUID());
		if (data.warps().has("gone")) {
			helper.fail("warp del left the warp behind");
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void backGoesToTheDeathLocationAndClearsIt(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ServerLevel level = level(player);
		BlockPos deathSpot = BlockPos.containing(player.position()).offset(12, 0, 12);
		player.setLastDeathLocation(Optional.of(GlobalPos.of(level.dimension(), deathSpot)));

		command(player, "back");

		if (player.getLastDeathLocation().isPresent()) {
			helper.fail("back left the death location set, so a second back would reuse it");
			return;
		}

		Vec3 expected = Vec3.atBottomCenterOf(deathSpot);
		if (player.position().distanceTo(expected) > TOLERANCE) {
			helper.fail("back did not go to the death spot: " + player.position() + " expected " + expected);
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void backWithoutADeathLocationUndoesTheLastTeleport(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 origin = player.position();
		Vec3 elsewhere = new Vec3(origin.x + 25.0, origin.y, origin.z + 25.0);

		command(player, "sethome");
		player.teleportTo(elsewhere.x, elsewhere.y, elsewhere.z);
		command(player, "home");
		command(player, "back");

		if (player.position().distanceTo(elsewhere) > TOLERANCE) {
			helper.fail("back did not undo the teleport: " + player.position() + " expected " + elsewhere);
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void backReportsFailureWhenThereIsNowhereToGo(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 before = player.position();

		// Nothing has moved this player and they have not died, so back has no target.
		command(player, "back");

		if (player.position().distanceTo(before) > TOLERANCE) {
			helper.fail("back moved the player even though there was nowhere to go");
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void acceptingATeleportRequestMovesTheRequesterToTheTarget(GameTestHelper helper) {
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		ServerPlayer requester = helper.makeMockServerPlayerInLevel();
		Vec3 targetSpot = target.position();

		requester.teleportTo(targetSpot.x + 40.0, targetSpot.y, targetSpot.z);
		TpaService.registry().request(requester.getUUID(), target.getUUID());

		command(target, "tpaccept");

		if (requester.position().distanceTo(targetSpot) > TOLERANCE) {
			helper.fail("the accepted requester did not arrive: " + requester.position() + " expected " + targetSpot);
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void denyingATeleportRequestLeavesTheRequesterWhereTheyAre(GameTestHelper helper) {
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		ServerPlayer requester = helper.makeMockServerPlayerInLevel();
		Vec3 requesterSpot = requester.position();
		Vec3 away = new Vec3(requesterSpot.x + 40.0, requesterSpot.y, requesterSpot.z);

		requester.teleportTo(away.x, away.y, away.z);
		TpaService.registry().request(requester.getUUID(), target.getUUID());

		command(target, "tpadeny");

		if (requester.position().distanceTo(away) > TOLERANCE) {
			helper.fail("a denied requester was moved anyway: " + requester.position());
			return;
		}
		if (TpaService.registry().hasPendingFor(target.getUUID())) {
			helper.fail("the denied request is still pending");
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void acceptingIsRefusedWhenSeveralRequestsArePending(GameTestHelper helper) {
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		ServerPlayer first = helper.makeMockServerPlayerInLevel();
		ServerPlayer second = helper.makeMockServerPlayerInLevel();
		Vec3 targetSpot = target.position();
		double farness = 30.0;

		first.teleportTo(targetSpot.x + farness, targetSpot.y, targetSpot.z);
		second.teleportTo(targetSpot.x - farness, targetSpot.y, targetSpot.z);

		TpaService.registry().request(first.getUUID(), target.getUUID());
		TpaService.registry().request(second.getUUID(), target.getUUID());
		if (TpaService.registry().pendingFor(target.getUUID()).size() != 2) {
			helper.fail("test setup failed: expected two pending requests");
			return;
		}

		// With more than one candidate, a bare /tpaccept must refuse rather than pick one.
		command(target, "tpaccept");

		if (TpaService.registry().pendingFor(target.getUUID()).size() != 2) {
			helper.fail("an ambiguous tpaccept consumed a request anyway");
			return;
		}
		if (first.position().distanceTo(targetSpot) < farness - TOLERANCE
				|| second.position().distanceTo(targetSpot) < farness - TOLERANCE) {
			helper.fail("an ambiguous tpaccept teleported somebody anyway");
			return;
		}
		helper.succeed();
	}

	@GameTest(structure = EMPTY_STRUCTURE, maxTicks = 100)
	public void acceptingWithNothingPendingDoesNotMoveAnybody(GameTestHelper helper) {
		ServerPlayer target = helper.makeMockServerPlayerInLevel();
		Vec3 before = target.position();

		command(target, "tpaccept");

		if (target.position().distanceTo(before) > TOLERANCE) {
			helper.fail("tpaccept moved the player even with no pending request");
			return;
		}
		helper.succeed();
	}

	private static void command(ServerPlayer player, String command) {
		level(player).getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
	}

	private static ServerLevel level(ServerPlayer player) {
		return player.level();
	}
}
