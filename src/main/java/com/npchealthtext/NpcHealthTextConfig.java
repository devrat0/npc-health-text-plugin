package com.npchealthtext;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("npchealthtext")
public interface NpcHealthTextConfig extends Config
{
	@ConfigSection(
		name = "HP Options",
		description = "Configure how the HP text overlay behaves",
		position = 0
	)
	String hpSection = "hpOptions";

	@ConfigSection(
		name = "Text Style",
		description = "Configure the appearance of the HP text",
		position = 1
	)
	String textStyleSection = "textStyle";

	@ConfigSection(
		name = "Background Style",
		description = "Configure optional background bubble around the text",
		position = 2
	)
	String bgSection = "bgStyle";

	// ──────────────────────────────────────────────
	//  HP OPTIONS
	// ──────────────────────────────────────────────

	@ConfigItem(
		keyName = "npcDisplayMode",
		name = "NPC Display Mode",
		description = "Choose which NPCs to show HP text on: Show Target NPC (only the NPC you are currently attacking) or Show All (all visible NPCs)",
		position = 0,
		section = "hpOptions"
	)
	default NpcDisplayMode npcDisplayMode()
	{
		return NpcDisplayMode.SHOW_TARGET_NPC;
	}

	@ConfigItem(
		keyName = "npcNames",
		name = "NPC Whitelist",
		description = "Comma-separated list of NPC names to display overlay for. Supports wildcards",
		position = 1,
		section = "hpOptions"
	)
	default String npcNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "npcBlacklist",
		name = "NPC Blacklist",
		description = "Comma-separated list of NPC names to hide overlay for. Supports wildcards",
		position = 2,
		section = "hpOptions"
	)
	default String npcBlacklist()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hideIfFull",
		name = "Hide if HP is Full",
		description = "Hide the HP text overlay when the NPC is at full health",
		position = 3,
		section = "hpOptions"
	)
	default boolean hideIfFull()
	{
		return false;
	}

	@ConfigItem(
		keyName = "displayMode",
		name = "HP Text Format",
		description = "Choose how health is displayed: HP Value (325/900), HP Percentage (36%), or Both (325/900 (36%))",
		position = 4,
		section = "hpOptions"
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.BOTH;
	}

	@ConfigItem(
		keyName = "showDecimalPercentage",
		name = "Show Decimal Percentage",
		description = "Include 1 decimal place in percentage (e.g., 36.1% vs 36%)",
		position = 5,
		section = "hpOptions"
	)
	default boolean showDecimalPercentage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayPosition",
		name = "Overlay Position",
		description = "Anchor position for HP text overlay relative to the NPC: Top (default), Middle, or Bottom",
		position = 6,
		section = "hpOptions"
	)
	default OverlayPositionMode overlayPosition()
	{
		return OverlayPositionMode.TOP;
	}

	@ConfigItem(
		keyName = "heightOffset",
		name = "Height Offset",
		description = "Fine-tune vertical offset in game height units relative to the selected Overlay Position",
		position = 7,
		section = "hpOptions"
	)
	@Range(min = -200, max = 200)
	default int heightOffset()
	{
		return 36;
	}

	@ConfigItem(
		keyName = "showAfterHealthBarDisappears",
		name = "Keep Text After HP Bar Disappears",
		description = "Continue displaying the health text overlay even after the in-game health bar times out and disappears",
		position = 8,
		section = "hpOptions"
	)
	default boolean showAfterHealthBarDisappears()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWithoutHealthBar",
		name = "Show Before HP Bar Appears",
		description = "Display HP text overlay for visible NPCs even before they take damage or show an in-game overhead health bar (assumes 100% HP)",
		position = 9,
		section = "hpOptions"
	)
	default boolean showWithoutHealthBar()
	{
		return true;
	}

	// ──────────────────────────────────────────────
	//  TEXT STYLE
	// ──────────────────────────────────────────────

	@ConfigItem(
		keyName = "fontType",
		name = "Font",
		description = "Font used for the HP overlay text",
		position = 0,
		section = "textStyle"
	)
	default FontType fontType()
	{
		return FontType.RUNESCAPE_SMALL;
	}

	@ConfigItem(
		keyName = "customFontName",
		name = "Font Name (if custom)",
		description = "The name of the system font to use when 'Custom / System Font' is selected",
		position = 1,
		section = "textStyle"
	)
	default String customFontName()
	{
		return "Arial";
	}

	@ConfigItem(
		keyName = "fontSize",
		name = "Font Size",
		description = "Font size for the HP overlay text",
		position = 2,
		section = "textStyle"
	)
	@Range(min = 8, max = 32)
	default int fontSize()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "textStyle",
		name = "Text Style",
		description = "Visual style and accent options for the overlay text",
		position = 3,
		section = "textStyle"
	)
	default TextStyle textStyle()
	{
		return TextStyle.SHADOW_BOLD;
	}

	@Alpha
	@ConfigItem(
		keyName = "textColor",
		name = "Static Text Color",
		description = "The static color of the overlay text (used when Dynamic Text Color is disabled)",
		position = 4,
		section = "textStyle"
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "dynamicTextColor",
		name = "Dynamic Text Color",
		description = "Transition text color from Green (100% HP) to Yellow (50% HP) to Red (0% HP)",
		position = 5,
		section = "textStyle"
	)
	default boolean dynamicTextColor()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "highHpColor",
		name = "High HP Color",
		description = "Text color at high/full health",
		position = 6,
		section = "textStyle"
	)
	default Color highHpColor()
	{
		return new Color(0, 255, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "lowHpColor",
		name = "Low HP Color",
		description = "Text color at low health",
		position = 7,
		section = "textStyle"
	)
	default Color lowHpColor()
	{
		return new Color(255, 0, 0);
	}

	// ──────────────────────────────────────────────
	//  BACKGROUND STYLE
	// ──────────────────────────────────────────────

	@Alpha
	@ConfigItem(
		keyName = "bgColor",
		name = "Background Color",
		description = "Background bubble color behind text. Set opacity to 0 for no background.",
		position = 0,
		section = "bgStyle"
	)
	default Color bgColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@ConfigItem(
		keyName = "bubblePaddingX",
		name = "Padding (Horizontal)",
		description = "Horizontal padding in pixels",
		position = 1,
		section = "bgStyle"
	)
	@Range(min = 0, max = 30)
	default int bubblePaddingX()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "bubblePaddingY",
		name = "Padding (Vertical)",
		description = "Vertical padding in pixels",
		position = 2,
		section = "bgStyle"
	)
	@Range(min = 0, max = 20)
	default int bubblePaddingY()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "bubbleRoundness",
		name = "Bubble Roundness",
		description = "Corner radius of background bubble in pixels",
		position = 3,
		section = "bgStyle"
	)
	@Range(min = 0, max = 30)
	default int bubbleRoundness()
	{
		return 6;
	}
}
