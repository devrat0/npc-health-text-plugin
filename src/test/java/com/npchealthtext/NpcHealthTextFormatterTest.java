package com.npchealthtext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NpcHealthTextFormatterTest
{
	private NpcHealthTextFormatter formatter;

	@Before
	public void setUp()
	{
		formatter = new NpcHealthTextFormatter();
	}

	@Test
	public void testBothPreset()
	{
		String result = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.BOTH, true, false, null);
		Assert.assertEquals("325 / 900 (36.1%)", result);
	}

	@Test
	public void testBothValueOnlyPreset()
	{
		String result = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.BOTH_VALUE_ONLY, true, false, null);
		Assert.assertEquals("325 (36.1%)", result);
	}

	@Test
	public void testBothPercentFirstValuePreset()
	{
		String result = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.BOTH_PERCENT_FIRST_VALUE, true, false, null);
		Assert.assertEquals("36.1% (325)", result);
	}

	@Test
	public void testBothPercentFirstHpSuffixPreset()
	{
		String result = formatter.formatHealthText(15, 30, 15, 30, false, DisplayMode.BOTH_PERCENT_FIRST_HP_SUFFIX, true, false, null);
		Assert.assertEquals("50.0% (15 HP)", result);
	}

	@Test
	public void testBothPercentFirstMaxPreset()
	{
		String result = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.BOTH_PERCENT_FIRST_MAX, true, false, null);
		Assert.assertEquals("36.1% (325 / 900)", result);
	}

	@Test
	public void testHpValuePreset()
	{
		String result = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.HP_VALUE, true, false, null);
		Assert.assertEquals("325 / 900", result);
	}

	@Test
	public void testHpValueOnlyPreset()
	{
		String result = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.HP_VALUE_ONLY, true, false, null);
		Assert.assertEquals("325", result);
	}

	@Test
	public void testHpPercentagePreset()
	{
		String result = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.HP_PERCENTAGE, true, false, null);
		Assert.assertEquals("36.1%", result);
	}

	@Test
	public void testCustomTemplateEngine()
	{
		String custom1 = formatter.formatHealthText(15, 30, 15, 30, false, DisplayMode.CUSTOM, true, false, "{pct} ({curr} HP)");
		Assert.assertEquals("50.0% (15 HP)", custom1);

		String custom2 = formatter.formatHealthText(325, 900, 325, 900, false, DisplayMode.CUSTOM, true, true, "{curr}/{max} [{pct}]");
		Assert.assertEquals("325/900 [36.1]", custom2);
	}

	@Test
	public void testUnknownMaxHpFallback()
	{
		String result = formatter.formatHealthText(0, 0, 50, 100, false, DisplayMode.BOTH, true, false, null);
		Assert.assertEquals("50.0%", result);
	}
}
