package com.npchealthtext;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class NpcHealthTextOverlay extends Overlay
{
	private static final Pattern HP_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

	private final Client client;
	private final NpcHealthTextConfig config;
	private final NPCManager npcManager;
	private final Map<NPC, int[]> lastHpMap = new ConcurrentHashMap<>();

	private NPC lastTargetNpc = null;
	private String lastTargetName = null;
	private long lastTargetTime = 0;

	@Inject
	public NpcHealthTextOverlay(Client client, NpcHealthTextConfig config, NPCManager npcManager)
	{
		this.client = client;
		this.config = config;
		this.npcManager = npcManager;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event != null && event.getNpc() != null)
		{
			lastHpMap.remove(event.getNpc());
			if (lastTargetNpc == event.getNpc())
			{
				lastTargetNpc = null;
			}
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		configureRenderingHints(graphics);

		Font font = resolveFont();
		graphics.setFont(font);
		FontMetrics fm = graphics.getFontMetrics(font);

		Player localPlayer = client.getLocalPlayer();
		Actor currentInteracting = localPlayer != null ? localPlayer.getInteracting() : null;

		Iterable<NPC> npcs = client.getNpcs();
		if (npcs == null)
		{
			return null;
		}

		if (currentInteracting instanceof NPC)
		{
			lastTargetNpc = (NPC) currentInteracting;
			if (lastTargetNpc.getName() != null)
			{
				lastTargetName = lastTargetNpc.getName();
			}
			lastTargetTime = System.currentTimeMillis();
		}
		else if (localPlayer != null)
		{
			for (NPC n : npcs)
			{
				if (n != null && n.getInteracting() == localPlayer)
				{
					lastTargetNpc = n;
					if (n.getName() != null)
					{
						lastTargetName = n.getName();
					}
					lastTargetTime = System.currentTimeMillis();
					break;
				}
			}
		}

		for (NPC npc : npcs)
		{
			if (npc == null || npc.getName() == null || npc.getName().trim().isEmpty())
			{
				continue;
			}

			String npcName = npc.getName();

			// Filter based on NPC Display Mode (Show Target NPC vs Show All)
			NpcDisplayMode npcMode = config.npcDisplayMode();
			if (npcMode == null || npcMode == NpcDisplayMode.SHOW_TARGET_NPC)
			{
				boolean isTarget = false;
				if (currentInteracting == npc)
				{
					isTarget = true;
				}
				else if (localPlayer != null && npc.getInteracting() == localPlayer)
				{
					isTarget = true;
				}
				else if (lastTargetNpc == npc && !npc.isDead() && (System.currentTimeMillis() - lastTargetTime < 5000))
				{
					isTarget = true;
				}
				else if (lastTargetName != null && npcName.equalsIgnoreCase(lastTargetName) && !npc.isDead() && (System.currentTimeMillis() - lastTargetTime < 5000))
				{
					isTarget = true;
				}

				if (!isTarget)
				{
					continue;
				}
			}

			// Filter based on NPC Names List if specified
			String nameList = config.npcNames();
			if (nameList != null && !nameList.trim().isEmpty())
			{
				if (!isNameInList(npcName, nameList))
				{
					continue;
				}
			}

			int ratio = npc.getHealthRatio();
			int scale = npc.getHealthScale();

			if (npc.isDead())
			{
				ratio = 0;
				if (scale <= 0 && lastHpMap.containsKey(npc))
				{
					scale = lastHpMap.get(npc)[1];
				}
				if (scale <= 0)
				{
					scale = 30;
				}
			}
			else if (ratio >= 0 && scale > 0)
			{
				lastHpMap.put(npc, new int[]{ratio, scale});
			}
			else if (lastHpMap.containsKey(npc))
			{
				if (config.showAfterHealthBarDisappears())
				{
					int[] cached = lastHpMap.get(npc);
					ratio = cached[0];
					scale = cached[1];
				}
			}
			else if (config.showWithoutHealthBar())
			{
				ratio = 100;
				scale = 100;
			}

			// Only display overlay if ratio >= 0 and scale > 0
			if (ratio < 0 || scale <= 0)
			{
				continue;
			}

			boolean isFull = (ratio == scale);
			if (isFull && config.hideIfFull())
			{
				continue;
			}

			int maxHp = 0;
			int overrideCurrentHp = -1;

			// 1. Try standard RuneLite function first (npcManager.getHealth)
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

			// 2. Fallback: If standard function returns <= 0 (or for live scaled boss HP), check top-of-screen Boss Bar widget
			int[] bossWidgetHp = getBossHealthFromWidget();
			if (bossWidgetHp != null && bossWidgetHp.length >= 2)
			{
				overrideCurrentHp = bossWidgetHp[0];
				maxHp = bossWidgetHp[1];
			}

			DisplayMode mode = config.displayMode();
			if (mode == null)
			{
				mode = DisplayMode.BOTH;
			}

			String text;
			if (maxHp > 0)
			{
				int currentHp = (overrideCurrentHp >= 0) ? overrideCurrentHp : (int) Math.round((double) maxHp * ratio / scale);
				if (currentHp == 0 && ratio > 0)
				{
					currentHp = 1;
				}

				String valStr = String.format("%d / %d", currentHp, maxHp);

				String pctStr;
				double hpFraction = (overrideCurrentHp >= 0 && maxHp > 0) ? ((double) currentHp / maxHp) : ((double) ratio / scale);
				if (config.showDecimalPercentage())
				{
					pctStr = String.format("%.1f%%", hpFraction * 100.0);
				}
				else
				{
					int pctInt = (int) Math.round(hpFraction * 100.0);
					if (pctInt == 0 && ratio > 0)
					{
						pctInt = 1;
					}
					pctStr = String.format("%d%%", pctInt);
				}

				switch (mode)
				{
					case HP_VALUE:
						text = valStr;
						break;
					case HP_PERCENTAGE:
						text = pctStr;
						break;
					case BOTH:
					default:
						text = String.format("%s (%s)", valStr, pctStr);
						break;
				}
			}
			else
			{
				if (config.showDecimalPercentage())
				{
					text = String.format("%.1f%%", ((double) ratio / scale) * 100.0);
				}
				else
				{
					int pctInt = (int) Math.round(((double) ratio / scale) * 100.0);
					if (pctInt == 0 && ratio > 0)
					{
						pctInt = 1;
					}
					text = String.format("%d%%", pctInt);
				}
			}

			// Determine dynamic canvas text location above NPC's health bar
			int logicalHeight = Math.max(0, npc.getLogicalHeight());
			int zOffset = logicalHeight + config.heightOffset();
			Point textLocation = npc.getCanvasTextLocation(graphics, text, zOffset);

			if (textLocation == null)
			{
				textLocation = npc.getCanvasTextLocation(graphics, text, logicalHeight);
			}
			if (textLocation == null)
			{
				textLocation = npc.getCanvasTextLocation(graphics, text, 0);
			}

			if (textLocation == null)
			{
				continue;
			}

			int drawX = textLocation.getX();
			int drawY = Math.max(20, textLocation.getY());
			Point location = new Point(drawX, drawY);

			// Determine text color (dynamic gradient vs static)
			Color textColor;
			if (config.dynamicTextColor())
			{
				double hpFraction = Math.max(0.0, Math.min(1.0, (double) ratio / scale));
				textColor = getHpGradientColor(config.lowHpColor(), config.highHpColor(), hpFraction);
			}
			else
			{
				textColor = config.textColor();
			}

			// Optional background bubble
			Color bgColor = config.bgColor();
			if (bgColor.getAlpha() > 0)
			{
				int paddingX = config.bubblePaddingX();
				int paddingY = config.bubblePaddingY();
				int textWidth = fm.stringWidth(text);
				int textHeight = fm.getHeight();

				int bubbleWidth = textWidth + paddingX * 2;
				int bubbleHeight = textHeight + paddingY * 2;
				int bubbleX = location.getX() - paddingX;
				int bubbleY = location.getY() - fm.getAscent() - paddingY;

				graphics.setColor(bgColor);
				int roundness = config.bubbleRoundness();
				graphics.fillRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, roundness, roundness);
			}

			drawText(graphics, text, location.getX(), location.getY(), fm, textColor);
		}

		return null;
	}

	private boolean isNameInList(String npcName, String rawList)
	{
		if (rawList == null || rawList.trim().isEmpty())
		{
			return false;
		}

		String lowerName = npcName.trim().toLowerCase();
		String[] parts = rawList.split(",");
		for (String part : parts)
		{
			if (part.trim().toLowerCase().equals(lowerName))
			{
				return true;
			}
		}
		return false;
	}

	private Color getHpGradientColor(Color lowColor, Color highColor, double ratio)
	{
		ratio = Math.max(0.0, Math.min(1.0, ratio));

		int r, g, b, a;
		if (ratio >= 0.5)
		{
			double factor = (ratio - 0.5) * 2.0;
			int midR = 255;
			int midG = 255;
			int midB = 0;

			r = (int) (midR + factor * (highColor.getRed() - midR));
			g = (int) (midG + factor * (highColor.getGreen() - midG));
			b = (int) (midB + factor * (highColor.getBlue() - midB));
			a = highColor.getAlpha();
		}
		else
		{
			double factor = ratio * 2.0;
			int midR = 255;
			int midG = 255;
			int midB = 0;

			r = (int) (lowColor.getRed() + factor * (midR - lowColor.getRed()));
			g = (int) (lowColor.getGreen() + factor * (midG - lowColor.getGreen()));
			b = (int) (lowColor.getBlue() + factor * (midB - lowColor.getBlue()));
			a = lowColor.getAlpha();
		}

		return new Color(
			Math.max(0, Math.min(255, r)),
			Math.max(0, Math.min(255, g)),
			Math.max(0, Math.min(255, b)),
			Math.max(0, Math.min(255, a))
		);
	}

	private int getSnappedFontSize()
	{
		FontType type = config.fontType();
		boolean isRuneScape = type == FontType.RUNESCAPE
			|| type == FontType.RUNESCAPE_SMALL
			|| type == FontType.RUNESCAPE_BOLD;

		int currentSize = config.fontSize();
		if (isRuneScape)
		{
			Font nativeFont;
			switch (type)
			{
				case RUNESCAPE:
					nativeFont = FontManager.getRunescapeFont();
					break;
				case RUNESCAPE_SMALL:
					nativeFont = FontManager.getRunescapeSmallFont();
					break;
				default:
					nativeFont = FontManager.getRunescapeBoldFont();
					break;
			}
			int nativeSize = nativeFont.getSize();
			int scale = Math.max(1, Math.round((float) currentSize / nativeSize));
			return nativeSize * scale;
		}
		return currentSize;
	}

	private Font resolveFont()
	{
		Font base;

		switch (config.fontType())
		{
			case RUNESCAPE:
				base = FontManager.getRunescapeFont();
				break;
			case RUNESCAPE_SMALL:
				base = FontManager.getRunescapeSmallFont();
				break;
			case RUNESCAPE_BOLD:
				base = FontManager.getRunescapeBoldFont();
				break;
			case ARIAL:
				base = new Font("Arial", Font.PLAIN, config.fontSize());
				break;
			case DIALOG:
				base = new Font(Font.DIALOG, Font.PLAIN, config.fontSize());
				break;
			case SANS_SERIF:
				base = new Font(Font.SANS_SERIF, Font.PLAIN, config.fontSize());
				break;
			case SERIF:
				base = new Font(Font.SERIF, Font.PLAIN, config.fontSize());
				break;
			case MONOSPACED:
				base = new Font(Font.MONOSPACED, Font.PLAIN, config.fontSize());
				break;
			case CUSTOM:
				base = new Font(config.customFontName(), Font.PLAIN, config.fontSize());
				break;
			default:
				base = FontManager.getRunescapeSmallFont();
				break;
		}
		return base.deriveFont((float) getSnappedFontSize());
	}

	private void configureRenderingHints(Graphics2D graphics)
	{
		FontType type = config.fontType();
		boolean isRuneScape = type == FontType.RUNESCAPE
			|| type == FontType.RUNESCAPE_SMALL
			|| type == FontType.RUNESCAPE_BOLD;

		if (isRuneScape)
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		}
		else
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		}
	}

	private void drawStyledString(Graphics2D graphics, String text, int x, int y, Color mainColor)
	{
		TextStyle style = config.textStyle();
		int alpha = mainColor.getAlpha();
		Color shadowColorWithAlpha = new Color(0, 0, 0, alpha);

		if (style == TextStyle.OUTLINE || style == TextStyle.OUTLINE_SHADOW)
		{
			graphics.setColor(shadowColorWithAlpha);
			graphics.drawString(text, x - 1, y);
			graphics.drawString(text, x + 1, y);
			graphics.drawString(text, x, y - 1);
			graphics.drawString(text, x, y + 1);

			if (config.fontType() != FontType.RUNESCAPE_SMALL)
			{
				graphics.drawString(text, x - 1, y - 1);
				graphics.drawString(text, x + 1, y - 1);
				graphics.drawString(text, x - 1, y + 1);
				graphics.drawString(text, x + 1, y + 1);
			}
		}

		if (style == TextStyle.SHADOW || style == TextStyle.OUTLINE_SHADOW)
		{
			graphics.setColor(shadowColorWithAlpha);
			graphics.drawString(text, x + 1, y + 1);
		}
		else if (style == TextStyle.SHADOW_BOLD)
		{
			graphics.setColor(shadowColorWithAlpha);
			graphics.drawString(text, x + 1, y + 1);
			graphics.drawString(text, x + 1, y + 2);
			graphics.drawString(text, x + 2, y + 1);
			graphics.drawString(text, x + 2, y + 2);
		}

		graphics.setColor(mainColor);
		graphics.drawString(text, x, y);
	}

	private void drawText(Graphics2D graphics, String text, int x, int y, FontMetrics fm, Color textColor)
	{
		FontType type = config.fontType();
		boolean isRuneScape = type == FontType.RUNESCAPE
			|| type == FontType.RUNESCAPE_SMALL
			|| type == FontType.RUNESCAPE_BOLD;

		if (isRuneScape)
		{
			Font nativeFont;
			switch (type)
			{
				case RUNESCAPE:
					nativeFont = FontManager.getRunescapeFont();
					break;
				case RUNESCAPE_SMALL:
					nativeFont = FontManager.getRunescapeSmallFont();
					break;
				default:
					nativeFont = FontManager.getRunescapeBoldFont();
					break;
			}
			int nativeSize = nativeFont.getSize();
			int currentSize = getSnappedFontSize();

			if (currentSize != nativeSize)
			{
				int scale = currentSize / nativeSize;
				FontMetrics nativeFm = graphics.getFontMetrics(nativeFont);
				int nativeW = nativeFm.stringWidth(text);
				int nativeH = nativeFm.getHeight();

				if (nativeW <= 0 || nativeH <= 0)
				{
					return;
				}

				java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
					nativeW + 8, nativeH + 8, java.awt.image.BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = img.createGraphics();
				g2d.setFont(nativeFont);
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

				drawStyledString(g2d, text, 4, nativeFm.getAscent() + 4, textColor);
				g2d.dispose();

				int scaledW = (nativeW + 8) * scale;
				int scaledH = (nativeH + 8) * scale;
				int topY = y - nativeFm.getAscent() * scale;
				int drawX = x - 4 * scale;
				int drawY = topY - 4 * scale;

				Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				graphics.drawImage(img, drawX, drawY, scaledW, scaledH, null);
				if (oldInterpolation != null)
				{
					graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
				}
				return;
			}
		}

		drawStyledString(graphics, text, x, y, textColor);
	}

	private int[] getBossHealthFromWidget()
	{
		if (client == null)
		{
			return null;
		}

		try
		{
			Widget bossWidgetGroup = client.getWidget(303, 0);
			if (bossWidgetGroup != null)
			{
				Widget[] children = bossWidgetGroup.getChildren();
				if (children != null)
				{
					for (Widget child : children)
					{
						if (child == null || child.isHidden() || child.getText() == null)
						{
							continue;
						}
						int[] parsed = parseHpString(child.getText());
						if (parsed != null)
						{
							return parsed;
						}
					}
				}
			}

			for (int childId = 1; childId <= 25; childId++)
			{
				Widget w = client.getWidget(303, childId);
				if (w != null && !w.isHidden() && w.getText() != null)
				{
					int[] parsed = parseHpString(w.getText());
					if (parsed != null)
					{
						return parsed;
					}
				}
			}
		}
		catch (Exception ignored) {}

		return null;
	}

	private int[] parseHpString(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return null;
		}
		try
		{
			String cleanText = text.replaceAll("<[^>]*>", "").trim();
			Matcher matcher = HP_PATTERN.matcher(cleanText);
			if (matcher.find())
			{
				int cur = Integer.parseInt(matcher.group(1));
				int max = Integer.parseInt(matcher.group(2));
				if (max > 0 && cur >= 0)
				{
					return new int[]{cur, max};
				}
			}
		}
		catch (Exception ignored) {}
		return null;
	}
}
