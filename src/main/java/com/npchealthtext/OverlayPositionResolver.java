package com.npchealthtext;

import java.awt.Graphics2D;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.util.WildcardMatcher;

/**
 * Resolves dynamic canvas text locations (Top, Middle, Bottom) and parses custom
 * NPC position override configuration rules (e.g., "Great Olm*:Bottom").
 */
public class OverlayPositionResolver
{
	/**
	 * Parses position override configuration string for matching NPC names.
	 * E.g., "Great Olm*:Bottom, General Graardor:Middle".
	 */
	public OverlayPositionMode getPositionOverride(String npcName, String rawOverrides)
	{
		if (rawOverrides == null || rawOverrides.trim().isEmpty() || npcName == null)
		{
			return null;
		}

		String lowerName = npcName.trim().toLowerCase();
		String[] parts = rawOverrides.split(",");
		for (String part : parts)
		{
			String entry = part.trim();
			if (entry.isEmpty())
			{
				continue;
			}

			String pattern = entry;
			OverlayPositionMode mode = OverlayPositionMode.BOTTOM;

			int colonIdx = entry.lastIndexOf(':');
			if (colonIdx > 0 && colonIdx < entry.length() - 1)
			{
				pattern = entry.substring(0, colonIdx).trim();
				String posStr = entry.substring(colonIdx + 1).trim().toLowerCase();
				switch (posStr)
				{
					case "top":
						mode = OverlayPositionMode.TOP;
						break;
					case "middle":
					case "mid":
					case "center":
						mode = OverlayPositionMode.MIDDLE;
						break;
					case "bottom":
					default:
						mode = OverlayPositionMode.BOTTOM;
						break;
				}
			}

			if (!pattern.isEmpty() && WildcardMatcher.matches(pattern.toLowerCase(), lowerName))
			{
				return mode;
			}
		}

		return null;
	}

	/**
	 * Calculates the canvas text placement location Point for an NPC, handling vertical offsets
	 * and safe location fallbacks.
	 */
	public Point calculateCanvasLocation(
		Graphics2D graphics,
		NPC npc,
		String npcName,
		String text,
		OverlayPositionMode defaultPosMode,
		int heightOffset,
		String rawOverrides)
	{
		if (npc == null || graphics == null || text == null)
		{
			return null;
		}

		int logicalHeight = Math.max(0, npc.getLogicalHeight());
		int baseHeight;

		// Check for position overrides before falling back to default configuration
		OverlayPositionMode posMode = getPositionOverride(npcName, rawOverrides);
		if (posMode == null)
		{
			posMode = defaultPosMode;
		}
		if (posMode == null)
		{
			posMode = OverlayPositionMode.TOP;
		}

		switch (posMode)
		{
			case MIDDLE:
				baseHeight = logicalHeight / 2;
				break;
			case BOTTOM:
				baseHeight = 0;
				break;
			case TOP:
			default:
				baseHeight = logicalHeight;
				break;
		}

		// Apply user height offset
		int zOffset = baseHeight + heightOffset;
		Point textLocation = getSafeCanvasTextLocation(graphics, npc, text, zOffset);

		// Fallback to baseHeight if custom zOffset location fails
		if (textLocation == null)
		{
			textLocation = getSafeCanvasTextLocation(graphics, npc, text, baseHeight);
		}
		// Fallback to ground level (zOffset = 0) if baseHeight fails
		if (textLocation == null)
		{
			textLocation = getSafeCanvasTextLocation(graphics, npc, text, 0);
		}

		if (textLocation == null)
		{
			return null;
		}

		int drawX = textLocation.getX();
		int drawY = Math.max(20, textLocation.getY());
		return new Point(drawX, drawY);
	}

	private Point getSafeCanvasTextLocation(Graphics2D graphics, NPC npc, String text, int zOffset)
	{
		if (npc == null || graphics == null || text == null)
		{
			return null;
		}
		try
		{
			return npc.getCanvasTextLocation(graphics, text, zOffset);
		}
		catch (Exception ignored)
		{
			return null;
		}
	}
}
