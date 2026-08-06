package com.npchealthtext;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * RuneLite Overlay responsible for rendering custom NPC health text overlays
 * above NPC health bars in game.
 */
public class NpcHealthTextOverlay extends Overlay
{
	private final Client client;
	private final NpcHealthTextConfig config;
	private final NPCManager npcManager;

	// Injected modular component helpers
	private final BossHealthManager bossHealthManager;
	private final NpcFilterManager npcFilterManager;
	private final NpcHealthTextFormatter npcHealthTextFormatter;
	private final OverlayPositionResolver overlayPositionResolver;
	private final NpcFontRenderer npcFontRenderer;

	// Health ratio and Max HP caches
	private final Map<Integer, int[]> lastHpMap = new ConcurrentHashMap<>();
	private final Map<String, Integer> bossMaxHpCache = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> npcIndexMaxHpCache = new ConcurrentHashMap<>();
	private final Map<Integer, Long> pulseMap = new ConcurrentHashMap<>();

	// Target tracking state
	private NPC lastTargetNpc = null;
	private long lastTargetTime = 0;

	@Inject
	public NpcHealthTextOverlay(Client client, NpcHealthTextConfig config, NPCManager npcManager)
	{
		this(
			client,
			config,
			npcManager,
			new BossHealthManager(),
			new NpcFilterManager(new BossHealthManager()),
			new NpcHealthTextFormatter(),
			new OverlayPositionResolver(),
			new NpcFontRenderer()
		);
	}

	public NpcHealthTextOverlay(
		Client client,
		NpcHealthTextConfig config,
		NPCManager npcManager,
		BossHealthManager bossHealthManager,
		NpcFilterManager npcFilterManager,
		NpcHealthTextFormatter npcHealthTextFormatter,
		OverlayPositionResolver overlayPositionResolver,
		NpcFontRenderer npcFontRenderer)
	{
		this.client = client;
		this.config = config;
		this.npcManager = npcManager;
		this.bossHealthManager = bossHealthManager;
		this.npcFilterManager = npcFilterManager;
		this.npcHealthTextFormatter = npcHealthTextFormatter;
		this.overlayPositionResolver = overlayPositionResolver;
		this.npcFontRenderer = npcFontRenderer;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	/**
	 * Clears cached NPC health state when an NPC despawns.
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event != null && event.getNpc() != null)
		{
			int index = event.getNpc().getIndex();
			lastHpMap.remove(index);
			npcIndexMaxHpCache.remove(index);
			pulseMap.remove(index);
			if (lastTargetNpc == event.getNpc())
			{
				lastTargetNpc = null;
			}
		}
	}

	/**
	 * Resets cached health state when hopping worlds, logging out, or loading regions.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.HOPPING || state == GameState.LOGIN_SCREEN || state == GameState.LOADING)
		{
			lastHpMap.clear();
			bossMaxHpCache.clear();
			npcIndexMaxHpCache.clear();
			pulseMap.clear();
			lastTargetNpc = null;
		}
	}

	/**
	 * Triggers a pulse effect when the player lands a damage hit (> 0 damage) on an NPC.
	 */
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!config.enableDamagePulse() || event == null || event.getActor() == null || !(event.getActor() instanceof NPC))
		{
			return;
		}

		Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat == null || !hitsplat.isMine())
		{
			return;
		}

		int amount = hitsplat.getAmount();
		int type = hitsplat.getHitsplatType();

		// Only trigger pulse if damage is > 0 and not a heal event
		if (amount > 0 && type != HitsplatID.HEAL)
		{
			NPC npc = (NPC) event.getActor();
			pulseMap.put(npc.getIndex(), System.currentTimeMillis());
		}
	}

	/**
	 * Primary overlay render loop called every frame by RuneLite OverlayManager.
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Configure rendering antialiasing hints and fonts
		npcFontRenderer.configureRenderingHints(graphics, config.fontType());

		Font font = npcFontRenderer.resolveFont(config.fontType(), config.fontSize(), config.customFontName());
		graphics.setFont(font);
		FontMetrics fm = graphics.getFontMetrics(font);

		Player localPlayer = client.getLocalPlayer();
		Actor currentInteracting = localPlayer != null ? localPlayer.getInteracting() : null;

		Iterable<NPC> npcs = client.getNpcs();
		if (npcs == null)
		{
			return null;
		}

		// Update active target tracking state
		if (currentInteracting instanceof NPC)
		{
			lastTargetNpc = (NPC) currentInteracting;
			lastTargetTime = System.currentTimeMillis();
		}
		else if (localPlayer != null)
		{
			if (lastTargetNpc != null && !lastTargetNpc.isDead() && lastTargetNpc.getInteracting() == localPlayer)
			{
				lastTargetTime = System.currentTimeMillis();
			}
			else
			{
				for (NPC n : npcs)
				{
					if (n != null && n.getInteracting() == localPlayer)
					{
						lastTargetNpc = n;
						lastTargetTime = System.currentTimeMillis();
						break;
					}
				}
			}
		}

		// Scrape Boss Bar widget (ID 303) and count matching NPCs in scene if enabled
		BossHealthManager.BossHealthData bossWidgetData = config.enableBossHealthScraping()
			? bossHealthManager.getBossHealthFromWidget(client)
			: null;

		int matchingBossCount = 0;
		if (bossWidgetData != null && bossWidgetData.getBossName() != null && !bossWidgetData.getBossName().trim().isEmpty())
		{
			matchingBossCount = bossHealthManager.countMatchingNpcs(npcs, bossWidgetData.getBossName());
		}

		// Iterate over visible NPCs and render overlays
		for (NPC npc : npcs)
		{
			try
			{
				renderNpcOverlay(graphics, fm, npc, localPlayer, currentInteracting, bossWidgetData, matchingBossCount);
			}
			catch (Exception ignored)
			{
			}
		}

		return null;
	}

	/**
	 * Renders individual HP text overlay for a single NPC.
	 */
	private void renderNpcOverlay(
		Graphics2D graphics,
		FontMetrics fm,
		NPC npc,
		Player localPlayer,
		Actor currentInteracting,
		BossHealthManager.BossHealthData bossWidgetData,
		int matchingBossCount)
	{
		if (npc == null || npc.getName() == null || npc.getName().trim().isEmpty())
		{
			return;
		}

		String npcName = npc.getName();

		boolean bossBarActive = bossWidgetData != null
			&& bossWidgetData.getBossName() != null
			&& !bossWidgetData.getBossName().trim().isEmpty()
			&& bossHealthManager.bossNameMatches(npcName, bossWidgetData.getBossName());

		int npcIndex = npc.getIndex();
		int ratio = npc.getHealthRatio();
		int scale = npc.getHealthScale();

		// Construct TargetContext parameter object for target evaluation
		TargetContext targetContext = new TargetContext(
			currentInteracting,
			localPlayer,
			lastTargetNpc,
			lastTargetTime,
			bossBarActive,
			matchingBossCount,
			bossWidgetData,
			lastHpMap
		);

		boolean isTarget = npcFilterManager.isTarget(npc, targetContext);

		// Evaluate NpcDisplayMode, whitelist, and blacklist filters
		if (!npcFilterManager.shouldRenderForDisplayMode(npcName, isTarget, config.npcDisplayMode(), config.npcNames(), config.npcBlacklist()))
		{
			return;
		}

		// Update or cache health ratio state
		if (npc.isDead())
		{
			ratio = 0;
			if (scale <= 0 && lastHpMap.containsKey(npcIndex))
			{
				scale = lastHpMap.get(npcIndex)[1];
			}
			if (scale <= 0)
			{
				scale = 30;
			}
		}
		else if (ratio >= 0 && scale > 0)
		{
			lastHpMap.put(npcIndex, new int[]{ratio, scale});
		}
		else if (config.showAfterHealthBarDisappears() && lastHpMap.containsKey(npcIndex))
		{
			int[] cached = lastHpMap.get(npcIndex);
			ratio = cached[0];
			scale = cached[1];
		}
		else if (config.showWithoutHealthBar())
		{
			ratio = 100;
			scale = 100;
		}

		if (ratio < 0 || scale <= 0)
		{
			return;
		}

		boolean isFull = (ratio == scale);
		if (isFull && config.hideIfFull())
		{
			return;
		}

		int maxHp = 0;
		int overrideCurrentHp = -1;

		// Lookup Max HP via RuneLite NPCManager first
		if (npcManager != null)
		{
			try
			{
				maxHp = npcManager.getHealth(npc.getId());
				if (maxHp <= 0 && npc.getTransformedComposition() != null)
				{
					maxHp = npcManager.getHealth(npc.getTransformedComposition().getId());
				}
			}
			catch (Exception ignored)
			{
				maxHp = 0;
			}
		}

		// Correlate Max HP & exact current HP with Boss Bar widget
		if (bossBarActive)
		{
			if (bossWidgetData.getMaxHp() > 0)
			{
				maxHp = bossWidgetData.getMaxHp();
			}

			if (bossWidgetData.getCurrentHp() >= 0)
			{
				if (matchingBossCount <= 1)
				{
					overrideCurrentHp = bossWidgetData.getCurrentHp();
				}
				else if (isTarget || bossHealthManager.isNpcRatioMatchingBossWidget(npc, ratio, scale, bossWidgetData, lastHpMap))
				{
					overrideCurrentHp = bossWidgetData.getCurrentHp();
				}
			}
		}

		// Update Max HP caches
		if (maxHp > 0)
		{
			npcIndexMaxHpCache.put(npcIndex, maxHp);
			String baseName = bossHealthManager.getNormalizedBaseName(npcName);
			if (!baseName.isEmpty())
			{
				bossMaxHpCache.put(baseName, maxHp);
			}
		}
		else
		{
			if (npcIndexMaxHpCache.containsKey(npcIndex))
			{
				maxHp = npcIndexMaxHpCache.get(npcIndex);
			}
			else
			{
				String baseName = bossHealthManager.getNormalizedBaseName(npcName);
				if (bossMaxHpCache.containsKey(baseName))
				{
					maxHp = bossMaxHpCache.get(baseName);
				}
			}
		}

		// Determine HP format mode (DisplayMode vs TargetDisplayMode)
		DisplayMode mode = config.displayMode();
		if (mode == null)
		{
			mode = DisplayMode.BOTH;
		}

		if (isTarget)
		{
			TargetDisplayMode targetMode = config.targetDisplayMode();
			if (targetMode != null && targetMode != TargetDisplayMode.DEFAULT)
			{
				switch (targetMode)
				{
					case BOTH:
						mode = DisplayMode.BOTH	;
						break;
					case BOTH_VALUE_ONLY:
						mode = DisplayMode.BOTH_VALUE_ONLY;
						break;
					case BOTH_PERCENT_FIRST_VALUE:
						mode = DisplayMode.BOTH_PERCENT_FIRST_VALUE;
						break;
					case BOTH_PERCENT_FIRST_HP_SUFFIX:
						mode = DisplayMode.BOTH_PERCENT_FIRST_HP_SUFFIX;
						break;
					case BOTH_PERCENT_FIRST_MAX:
						mode = DisplayMode.BOTH_PERCENT_FIRST_MAX;
						break;
					case HP_VALUE:
						mode = DisplayMode.HP_VALUE;
						break;
					case HP_VALUE_ONLY:
						mode = DisplayMode.HP_VALUE_ONLY;
						break;
					case HP_PERCENTAGE:
						mode = DisplayMode.HP_PERCENTAGE;
						break;
					case CUSTOM:
						mode = DisplayMode.CUSTOM;
						break;
				}
			}
		}

		boolean overrideActive = (overrideCurrentHp >= 0);
		int calcCurrentHp = overrideActive ? overrideCurrentHp : (int) Math.round((double) maxHp * ratio / scale);
		if (maxHp > 0 && calcCurrentHp == 0 && ratio > 0)
		{
			calcCurrentHp = 1;
		}

		// Format output health string
		String text = npcHealthTextFormatter.formatHealthText(
			calcCurrentHp,
			maxHp,
			ratio,
			scale,
			overrideActive,
			mode,
			config.showDecimalPercentage(),
			config.hidePercentageSymbol(),
			config.customHpFormat()
		);

		// Calculate dynamic canvas text location
		Point location = overlayPositionResolver.calculateCanvasLocation(
			graphics,
			npc,
			npcName,
			text,
			config.overlayPosition(),
			config.heightOffset(),
			config.positionOverrides()
		);

		if (location == null)
		{
			return;
		}

		// Determine text color (dynamic gradient vs static color)
		Color textColor;
		if (config.dynamicTextColor())
		{
			double hpFraction;
			if (maxHp > 0)
			{
				hpFraction = Math.max(0.0, Math.min(1.0, (double) calcCurrentHp / maxHp));
			}
			else
			{
				hpFraction = Math.max(0.0, Math.min(1.0, (double) ratio / scale));
			}
			textColor = npcHealthTextFormatter.getHpGradientColor(config.lowHpColor(), config.highHpColor(), hpFraction);
		}
		else
		{
			textColor = config.textColor();
		}

		// Calculate pulse pop and glow animation state
		int pulseIntensity = config.enableDamagePulse() ? config.pulseIntensity() : 0;
		int pulseDuration = config.pulseDuration();
		double pulseFactor = 0.0;
		int popYOffset = 0;

		if (pulseIntensity > 0 && pulseDuration > 0 && pulseMap.containsKey(npcIndex))
		{
			long startTime = pulseMap.get(npcIndex);
			long elapsed = System.currentTimeMillis() - startTime;
			if (elapsed < pulseDuration)
			{
				double progress = (double) elapsed / pulseDuration;
				pulseFactor = 1.0 - progress;
				popYOffset = (int) Math.round(2 * pulseFactor * (pulseIntensity / 5.0));
			}
			else
			{
				pulseMap.remove(npcIndex);
			}
		}

		int drawX = location.getX();
		int drawY = location.getY() - popYOffset;

		// Render background bubble if enabled
		Color bgColor = config.bgColor();
		if (bgColor != null && bgColor.getAlpha() > 0)
		{
			int paddingX = config.bubblePaddingX();
			int paddingY = config.bubblePaddingY();
			int textWidth = fm.stringWidth(text);
			int textHeight = fm.getHeight();

			int bubbleWidth = textWidth + paddingX * 2;
			int bubbleHeight = textHeight + paddingY * 2;
			int bubbleX = drawX - paddingX;
			int bubbleY = drawY - fm.getAscent() - paddingY;
			int roundness = config.bubbleRoundness();

			// Render feathered glow behind bubble if pulsing
			Color pulseColor = config.pulseColor();
			if (pulseFactor > 0.0 && pulseColor != null && pulseIntensity > 0)
			{
				int maxRadius = Math.max(1, Math.round((pulseIntensity / 2.0f) * (float) pulseFactor));
				int baseAlpha = pulseColor.getAlpha();
				int r = pulseColor.getRed();
				int g = pulseColor.getGreen();
				int b = pulseColor.getBlue();

				for (int rad = maxRadius; rad >= 1; rad--)
				{
					double layerFactor = 1.0 - ((double) (rad - 1) / maxRadius);
					int layerAlpha = (int) Math.round(baseAlpha * pulseFactor * layerFactor * 0.4);
					if (layerAlpha <= 0)
					{
						continue;
					}
					graphics.setColor(new Color(r, g, b, Math.min(255, layerAlpha)));
					graphics.fillRoundRect(bubbleX - rad, bubbleY - rad, bubbleWidth + rad * 2, bubbleHeight + rad * 2, roundness + rad, roundness + rad);
				}
			}

			graphics.setColor(bgColor);
			graphics.fillRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, roundness, roundness);
		}

		// Draw styled overlay text on canvas
		npcFontRenderer.drawText(
			graphics,
			text,
			drawX,
			drawY,
			fm,
			textColor,
			config.fontType(),
			config.fontSize(),
			config.textStyle(),
			config.pulseColor(),
			pulseIntensity,
			pulseFactor
		);
	}
}