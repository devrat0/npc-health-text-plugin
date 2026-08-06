package com.npchealthtext;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;

/**
 * Handles font resolution, snapped font sizing for RuneScape bitmap fonts,
 * text antialiasing hints, text styling (Outline, Shadow, Shadow Bold),
 * and pixel-nearest-neighbor font scaling.
 */
public class NpcFontRenderer
{
	/**
	 * Configures Graphics2D rendering hints based on whether the selected font is a RuneScape bitmap font.
	 */
	public void configureRenderingHints(Graphics2D graphics, FontType type)
	{
		boolean isRuneScape = isRuneScapeFont(type);

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

	public boolean isRuneScapeFont(FontType type)
	{
		return type == FontType.RUNESCAPE
			|| type == FontType.RUNESCAPE_SMALL
			|| type == FontType.RUNESCAPE_BOLD;
	}

	/**
	 * Snaps configured font size to an integer multiple of the native RuneScape bitmap font size to prevent blurring.
	 */
	public int getSnappedFontSize(FontType type, int currentSize)
	{
		if (isRuneScapeFont(type))
		{
			Font nativeFont = getNativeRuneScapeFont(type);
			int nativeSize = nativeFont.getSize();
			int scale = Math.max(1, Math.round((float) currentSize / nativeSize));
			return nativeSize * scale;
		}
		return currentSize;
	}

	/**
	 * Resolves system or RuneLite Font instance matching configuration.
	 */
	public Font resolveFont(FontType type, int fontSize, String customFontName)
	{
		Font base;

		if (type == null)
		{
			type = FontType.RUNESCAPE_SMALL;
		}

		switch (type)
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
				base = new Font("Arial", Font.PLAIN, fontSize);
				break;
			case DIALOG:
				base = new Font(Font.DIALOG, Font.PLAIN, fontSize);
				break;
			case SANS_SERIF:
				base = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
				break;
			case SERIF:
				base = new Font(Font.SERIF, Font.PLAIN, fontSize);
				break;
			case MONOSPACED:
				base = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
				break;
			case CUSTOM:
				base = new Font(customFontName != null ? customFontName : "Arial", Font.PLAIN, fontSize);
				break;
			default:
				base = FontManager.getRunescapeSmallFont();
				break;
		}
		return base.deriveFont((float) getSnappedFontSize(type, fontSize));
	}

	/**
	 * Draws styled text onto canvas with outline, shadow, or shadow bold effects.
	 */
	public void drawStyledString(Graphics2D graphics, String text, int x, int y, Color mainColor, TextStyle style, FontType fontType)
	{
		if (style == null)
		{
			style = TextStyle.SHADOW_BOLD;
		}

		int alpha = mainColor != null ? mainColor.getAlpha() : 255;
		Color shadowColorWithAlpha = new Color(0, 0, 0, alpha);

		// Render text outline
		if (style == TextStyle.OUTLINE || style == TextStyle.OUTLINE_SHADOW)
		{
			graphics.setColor(shadowColorWithAlpha);
			graphics.drawString(text, x - 1, y);
			graphics.drawString(text, x + 1, y);
			graphics.drawString(text, x, y - 1);
			graphics.drawString(text, x, y + 1);

			if (fontType != FontType.RUNESCAPE_SMALL)
			{
				graphics.drawString(text, x - 1, y - 1);
				graphics.drawString(text, x + 1, y - 1);
				graphics.drawString(text, x - 1, y + 1);
				graphics.drawString(text, x + 1, y + 1);
			}
		}

		// Render text shadow
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

		// Draw foreground main text
		graphics.setColor(mainColor);
		graphics.drawString(text, x, y);
	}

	/**
	 * Draws text onto canvas. For scaled RuneScape bitmap fonts, renders to an off-screen BufferedImage
	 * and uses nearest-neighbor interpolation to prevent pixel distortion.
	 */
	public void drawText(Graphics2D graphics, String text, int x, int y, FontMetrics fm, Color textColor, FontType type, int fontSize, TextStyle style)
	{
		drawText(graphics, text, x, y, fm, textColor, type, fontSize, style, null, 0, 0.0);
	}

	/**
	 * Draws text onto canvas with optional damage pulse feathered glow aura effect.
	 */
	public void drawText(
		Graphics2D graphics,
		String text,
		int x,
		int y,
		FontMetrics fm,
		Color textColor,
		FontType type,
		int fontSize,
		TextStyle style,
		Color pulseColor,
		int pulseIntensity,
		double pulseFactor)
	{
		int maxRadius = (pulseColor != null && pulseIntensity > 0 && pulseFactor > 0.0)
			? Math.max(1, Math.round((pulseIntensity / 2.0f) * (float) pulseFactor))
			: 0;

		if (isRuneScapeFont(type))
		{
			Font nativeFont = getNativeRuneScapeFont(type);
			int nativeSize = nativeFont.getSize();
			int currentSize = getSnappedFontSize(type, fontSize);

			// Off-screen nearest-neighbor scaling for enlarged RuneScape fonts
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

				int glowPad = 4 + maxRadius;
				BufferedImage img = new BufferedImage(
					nativeW + glowPad * 2, nativeH + glowPad * 2, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = img.createGraphics();
				g2d.setFont(nativeFont);
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

				int fontAscent = nativeFm.getAscent();
				int textX = glowPad;
				int textY = fontAscent + glowPad;

				if (maxRadius > 0)
				{
					drawFeatheredGlow(g2d, text, textX, textY, pulseColor, pulseIntensity, pulseFactor);
				}

				drawStyledString(g2d, text, textX, textY, textColor, style, type);
				g2d.dispose();

				int scaledW = (nativeW + glowPad * 2) * scale;
				int scaledH = (nativeH + glowPad * 2) * scale;
				int topY = y - fontAscent * scale;
				int drawX = x - glowPad * scale;
				int drawY = topY - glowPad * scale;

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

		if (maxRadius > 0)
		{
			drawFeatheredGlow(graphics, text, x, y, pulseColor, pulseIntensity, pulseFactor);
		}

		drawStyledString(graphics, text, x, y, textColor, style, type);
	}

	/**
	 * Renders concentric translucent feathered glow halo around text.
	 */
	public void drawFeatheredGlow(Graphics2D graphics, String text, int x, int y, Color pulseColor, int pulseIntensity, double pulseFactor)
	{
		if (pulseColor == null || pulseIntensity <= 0 || pulseFactor <= 0.0)
		{
			return;
		}

		int maxRadius = Math.max(1, Math.round((pulseIntensity / 2.0f) * (float) pulseFactor));
		int baseAlpha = pulseColor.getAlpha();
		int r = pulseColor.getRed();
		int g = pulseColor.getGreen();
		int b = pulseColor.getBlue();

		for (int radius = maxRadius; radius >= 1; radius--)
		{
			double layerFactor = 1.0 - ((double) (radius - 1) / maxRadius);
			int layerAlpha = (int) Math.round(baseAlpha * pulseFactor * layerFactor * 0.35);
			if (layerAlpha <= 0)
			{
				continue;
			}

			graphics.setColor(new Color(r, g, b, Math.min(255, layerAlpha)));
			for (int dx = -radius; dx <= radius; dx++)
			{
				for (int dy = -radius; dy <= radius; dy++)
				{
					if (dx * dx + dy * dy <= radius * radius + 1)
					{
						graphics.drawString(text, x + dx, y + dy);
					}
				}
			}
		}
	}

	private Font getNativeRuneScapeFont(FontType type)
	{
		if (type == null)
		{
			return FontManager.getRunescapeSmallFont();
		}
		switch (type)
		{
			case RUNESCAPE:
				return FontManager.getRunescapeFont();
			case RUNESCAPE_SMALL:
				return FontManager.getRunescapeSmallFont();
			case RUNESCAPE_BOLD:
			default:
				return FontManager.getRunescapeBoldFont();
		}
	}
}
