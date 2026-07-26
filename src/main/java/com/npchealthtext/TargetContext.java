package com.npchealthtext;

import java.util.Map;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;

/**
 * Immutable parameter object encapsulating frame-level targeting and boss widget state.
 */
public class TargetContext
{
	private final Actor currentInteracting;
	private final Player localPlayer;
	private final NPC lastTargetNpc;
	private final long lastTargetTime;
	private final boolean bossBarActive;
	private final int matchingBossCount;
	private final BossHealthManager.BossHealthData bossWidgetData;
	private final Map<Integer, int[]> lastHpMap;

	public TargetContext(
		Actor currentInteracting,
		Player localPlayer,
		NPC lastTargetNpc,
		long lastTargetTime,
		boolean bossBarActive,
		int matchingBossCount,
		BossHealthManager.BossHealthData bossWidgetData,
		Map<Integer, int[]> lastHpMap)
	{
		this.currentInteracting = currentInteracting;
		this.localPlayer = localPlayer;
		this.lastTargetNpc = lastTargetNpc;
		this.lastTargetTime = lastTargetTime;
		this.bossBarActive = bossBarActive;
		this.matchingBossCount = matchingBossCount;
		this.bossWidgetData = bossWidgetData;
		this.lastHpMap = lastHpMap;
	}

	public Actor getCurrentInteracting()
	{
		return currentInteracting;
	}

	public Player getLocalPlayer()
	{
		return localPlayer;
	}

	public NPC getLastTargetNpc()
	{
		return lastTargetNpc;
	}

	public long getLastTargetTime()
	{
		return lastTargetTime;
	}

	public boolean isBossBarActive()
	{
		return bossBarActive;
	}

	public int getMatchingBossCount()
	{
		return matchingBossCount;
	}

	public BossHealthManager.BossHealthData getBossWidgetData()
	{
		return bossWidgetData;
	}

	public Map<Integer, int[]> getLastHpMap()
	{
		return lastHpMap;
	}
}
