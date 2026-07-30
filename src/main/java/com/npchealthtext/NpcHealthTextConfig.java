package com.npchealthtext;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Configuration interface for NPC Health Text plugin.
 * Organizes settings into logical sections with collapsible style panels and HTML text-wrapped tooltips.
 */
@ConfigGroup("npchealthtext")
public interface NpcHealthTextConfig extends Config
{
	@ConfigSection(
		name = "HP Format Options",
		description = "<html>Configure format and timing options for HP text</html>",
		position = 0
	)
	String hpFormattingSection = "hpFormatting";

	@ConfigSection(
		name = "Position & Placement",
		description = "<html>Configure dynamic overlay positioning relative to NPCs</html>",
		position = 1
	)
	String positioningSection = "positioning";

	@ConfigSection(
		name = "Text & Font Style",
		description = "<html>Configure font, styling, and color gradients for HP text</html>",
		position = 2,
		closedByDefault = true
	)
	String textStyleSection = "textStyle";

	@ConfigSection(
		name = "Background Bubble Style",
		description = "<html>Configure optional background bubble container behind text</html>",
		position = 3,
		closedByDefault = true
	)
	String bgSection = "bgStyle";

	// ──────────────────────────────────────────────
	//  HP FORMAT OPTIONS
	// ──────────────────────────────────────────────

	@ConfigItem(
		keyName = "displayMode",
		name = "HP Format",
		description = "<html>Choose how health is displayed:<br>"
			+ "- Value/Max (%): 325 / 900 (36.1%)<br>"
			+ "- Value (%): 325 (36.1%)<br>"
			+ "- % (Value): 36.1% (325)<br>"
			+ "- % (Value HP): 36.1% (325 HP)<br>"
			+ "- % (Value/Max): 36.1% (325 / 900)<br>"
			+ "- Value/Max: 325 / 900<br>"
			+ "- Value Only: 325<br>"
			+ "- % Only: 36.1%<br>"
			+ "- Custom: Use Custom Template Format string below</html>",
		position = 0,
		section = "hpFormatting"
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.BOTH_PERCENT_FIRST_HP_SUFFIX;
	}

	@ConfigItem(
		keyName = "targetDisplayMode",
		name = "Target Format",
		description = "<html>HP text format to use specifically for active targets or NPCs targeting you.<br>"
			+ "Set to 'Default' to use standard HP Format.</html>",
		position = 1,
		section = "hpFormatting"
	)
	default TargetDisplayMode targetDisplayMode()
	{
		return TargetDisplayMode.DEFAULT;
	}

	@ConfigItem(
		keyName = "customHpFormat",
		name = "Custom Template Format",
		description = "<html>Template string used when HP Format or Target Format is set to 'Custom'.<br>"
			+ "Placeholders:<br>"
			+ "- <b>{curr}</b>: Current HP (e.g. 325)<br>"
			+ "- <b>{max}</b>: Max HP (e.g. 900)<br>"
			+ "- <b>{pct}</b>: Percentage (e.g. 36.1%)</html>",
		position = 2,
		section = "hpFormatting"
	)
	default String customHpFormat()
	{
		return "{pct} ({curr} HP)";
	}

	@ConfigItem(
		keyName = "showDecimalPercentage",
		name = "Show Decimal Percentage",
		description = "<html>Include 1 decimal place in percentage display (e.g., 36.1% vs 36%)</html>",
		position = 3,
		section = "hpFormatting"
	)
	default boolean showDecimalPercentage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hidePercentageSymbol",
		name = "Hide % Symbol",
		description = "<html>Hide the '%' percentage symbol from the HP overlay text<br>"
			+ "(e.g. '50' instead of '50%' or '150 / 300 (50.0)' instead of '150 / 300 (50.0%)').</html>",
		position = 4,
		section = "hpFormatting"
	)
	default boolean hidePercentageSymbol()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideIfFull",
		name = "Hide if HP is Full",
		description = "<html>Hide the HP text overlay when the NPC is at full health (100% HP)</html>",
		position = 5,
		section = "hpFormatting"
	)
	default boolean hideIfFull()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showAfterHealthBarDisappears",
		name = "Keep Text After Bar Fade",
		description = "<html>Continue displaying the health text overlay even after the in-game health bar times out and disappears</html>",
		position = 6,
		section = "hpFormatting"
	)
	default boolean showAfterHealthBarDisappears()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWithoutHealthBar",
		name = "Show Before Bar Appears",
		description = "<html>Display HP text overlay for visible NPCs even before they take damage or show an in-game overhead health bar (assumes 100% HP)</html>",
		position = 7,
		section = "hpFormatting"
	)
	default boolean showWithoutHealthBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableBossHealthScraping",
		name = "Boss Health Scraping",
		description = "<html>Correlate OSRS Boss Bar widget health to display exact current HP and scaled Max HP for bosses.<br>"
			+ "When disabled, bosses display standard ratio/percentage health.</html>",
		position = 8,
		section = "hpFormatting"
	)
	default boolean enableBossHealthScraping()
	{
		return true;
	}

	@ConfigItem(
		keyName = "npcDisplayMode",
		name = "NPC Display Mode",
		description = "<html>Choose which NPCs to show HP text on:<br>"
			+ "- Show Target NPC: Only your current target<br>"
			+ "- Show All: All visible NPCs<br>"
			+ "- Show Whitelisted: All visible NPCs on the whitelist<br>"
			+ "- Show Whitelist Target: Target NPC if on the whitelist</html>",
		position = 9,
		section = "hpFormatting"
	)
	default NpcDisplayMode npcDisplayMode()
	{
		return NpcDisplayMode.SHOW_TARGET_NPC;
	}

	@ConfigItem(
		keyName = "npcNames",
		name = "NPC Whitelist",
		description = "<html>Comma-separated list of NPC names to display overlay for.<br>"
			+ "Used when NPC Display Mode is Show Whitelisted or Show Whitelist Target.<br>"
			+ "Supports wildcards (e.g. Vanguard*, Great Olm*).</html>",
		position = 10,
		section = "hpFormatting"
	)
	default String npcNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "npcBlacklist",
		name = "NPC Blacklist",
		description = "<html>Comma-separated list of NPC names to hide overlay for.<br>"
			+ "Supports wildcards (e.g. Goblin*, Guard*).</html>",
		position = 11,
		section = "hpFormatting"
	)
	default String npcBlacklist()
	{
		return "";
	}

	// ──────────────────────────────────────────────
	//  POSITION & PLACEMENT
	// ──────────────────────────────────────────────

	@ConfigItem(
		keyName = "overlayPosition",
		name = "Overlay Position",
		description = "<html>Anchor position for HP text overlay relative to the NPC:<br>"
			+ "- Top (default)<br>"
			+ "- Middle<br>"
			+ "- Bottom</html>",
		position = 0,
		section = "positioning"
	)
	default OverlayPositionMode overlayPosition()
	{
		return OverlayPositionMode.TOP;
	}

	@ConfigItem(
		keyName = "heightOffset",
		name = "Height Offset",
		description = "<html>Fine-tune vertical offset in game height units relative to the selected Overlay Position</html>",
		position = 1,
		section = "positioning"
	)
	@Range(min = -200, max = 200)
	default int heightOffset()
	{
		return 36;
	}

	@ConfigItem(
		keyName = "positionOverrides",
		name = "NPC Position Overrides",
		description = "<html>Comma-separated list of NPC names with optional positions.<br>"
			+ "e.g. Great Olm*:Bottom, General Graardor:Middle, Corporeal Beast<br>"
			+ "Defaults to Bottom if unspecified. Supports wildcards.</html>",
		position = 2,
		section = "positioning"
	)
	default String positionOverrides()
	{
		return "";
	}

	// ──────────────────────────────────────────────
	//  TEXT & FONT STYLE (CLOSED BY DEFAULT)
	// ──────────────────────────────────────────────

	@ConfigItem(
		keyName = "fontType",
		name = "Font",
		description = "<html>Font used for the HP overlay text</html>",
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
		description = "<html>The name of the system font to use when 'Custom / System Font' is selected</html>",
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
		description = "<html>Font size for the HP overlay text</html>",
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
		description = "<html>Visual style and accent options for the overlay text (Shadow Bold, Shadow, Outline, Outline Shadow)</html>",
		position = 3,
		section = "textStyle"
	)
	default TextStyle textStyle()
	{
		return TextStyle.SHADOW_BOLD;
	}

	@ConfigItem(
		keyName = "dynamicTextColor",
		name = "Dynamic Text Color",
		description = "<html>Transition text color from Green (100% HP) to Yellow (50% HP) to Red (0% HP)</html>",
		position = 4,
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
		description = "<html>Text color at high/full health</html>",
		position = 5,
		section = "textStyle"
	)
	default Color highHpColor()
	{
		return new Color(0, 100, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "lowHpColor",
		name = "Low HP Color",
		description = "<html>Text color at low health</html>",
		position = 6,
		section = "textStyle"
	)
	default Color lowHpColor()
	{
		return new Color(255, 0, 0);
	}

	@Alpha
	@ConfigItem(
		keyName = "textColor",
		name = "Static Text Color",
		description = "<html>The static color of the overlay text (used when Dynamic Text Color is disabled)</html>",
		position = 7,
		section = "textStyle"
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	// ──────────────────────────────────────────────
	//  BACKGROUND BUBBLE STYLE (CLOSED BY DEFAULT)
	// ──────────────────────────────────────────────

	@Alpha
	@ConfigItem(
		keyName = "bgColor",
		name = "Background Color",
		description = "<html>Background bubble color behind text.<br>Set opacity to 0 for no background.</html>",
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
		description = "<html>Horizontal padding in pixels for background bubble</html>",
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
		description = "<html>Vertical padding in pixels for background bubble</html>",
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
		description = "<html>Corner radius of background bubble in pixels</html>",
		position = 3,
		section = "bgStyle"
	)
	@Range(min = 0, max = 30)
	default int bubbleRoundness()
	{
		return 6;
	}
}
