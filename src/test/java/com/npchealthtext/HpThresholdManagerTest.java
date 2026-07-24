package com.npchealthtext;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HpThresholdManagerTest
{
	private HpThresholdManager manager;

	@Before
	public void setUp()
	{
		manager = new HpThresholdManager();
	}

	@Test
	public void testParseGlobalRules()
	{
		List<HpThresholdRule> rules = manager.parseRules("10:red");
		Assert.assertEquals(1, rules.size());
		Assert.assertEquals("*", rules.get(0).getNpcPattern());
		Assert.assertEquals(10.0, rules.get(0).getThresholdPercent(), 0.001);
		Assert.assertEquals(Color.RED, rules.get(0).getColor());
	}

	@Test
	public void testParseNpcOverrideRules()
	{
		String overrides = "Ba-Ba:66:yellow, Ba-Ba:33:orange";
		List<HpThresholdRule> rules = manager.parseRules(overrides);
		Assert.assertEquals(2, rules.size());

		Assert.assertEquals("Ba-Ba", rules.get(0).getNpcPattern());
		Assert.assertEquals(66.0, rules.get(0).getThresholdPercent(), 0.001);
		Assert.assertEquals(Color.YELLOW, rules.get(0).getColor());

		Assert.assertEquals("Ba-Ba", rules.get(1).getNpcPattern());
		Assert.assertEquals(33.0, rules.get(1).getThresholdPercent(), 0.001);
		Assert.assertEquals(new Color(255, 140, 0), rules.get(1).getColor());
	}

	@Test
	public void testThresholdPrecedenceLowestPercentSelected()
	{
		String overrides = "Ba-Ba:66:yellow, Ba-Ba:33:orange";
		String global = "10:red";

		// At 70% HP -> No threshold active
		HpThresholdRule rule70 = manager.getMatchingThreshold("Ba-Ba", 70.0, overrides, global);
		Assert.assertNull(rule70);

		// At 50% HP -> Matches 66% Yellow
		HpThresholdRule rule50 = manager.getMatchingThreshold("Ba-Ba", 50.0, overrides, global);
		Assert.assertNotNull(rule50);
		Assert.assertEquals(66.0, rule50.getThresholdPercent(), 0.001);
		Assert.assertEquals(Color.YELLOW, rule50.getColor());

		// At 20% HP -> Matches both 66% Yellow and 33% Orange -> Selects 33% Orange!
		HpThresholdRule rule20 = manager.getMatchingThreshold("Ba-Ba", 20.0, overrides, global);
		Assert.assertNotNull(rule20);
		Assert.assertEquals(33.0, rule20.getThresholdPercent(), 0.001);
		Assert.assertEquals(new Color(255, 140, 0), rule20.getColor());
	}

	@Test
	public void testGlobalFallbackWhenNoOverrideMatches()
	{
		String overrides = "Ba-Ba:66:yellow, Ba-Ba:33:orange";
		String global = "10:red";

		// Goblin at 5% HP -> No override for Goblin, fallback to global 10% red
		HpThresholdRule rule = manager.getMatchingThreshold("Goblin", 5.0, overrides, global);
		Assert.assertNotNull(rule);
		Assert.assertEquals("*", rule.getNpcPattern());
		Assert.assertEquals(10.0, rule.getThresholdPercent(), 0.001);
		Assert.assertEquals(Color.RED, rule.getColor());
	}

	@Test
	public void testSerializeAndDeserializeRoundtrip()
	{
		HpThresholdRule r1 = new HpThresholdRule("Ba-Ba", 66.0, Color.YELLOW, null);
		HpThresholdRule r2 = new HpThresholdRule("Ba-Ba", 33.0, new Color(255, 140, 0), null);

		String serialized = manager.serializeRules(Arrays.asList(r1, r2));
		List<HpThresholdRule> parsed = manager.parseRules(serialized);

		Assert.assertEquals(2, parsed.size());
		Assert.assertEquals("Ba-Ba", parsed.get(0).getNpcPattern());
		Assert.assertEquals(66.0, parsed.get(0).getThresholdPercent(), 0.001);
		Assert.assertEquals("Ba-Ba", parsed.get(1).getNpcPattern());
		Assert.assertEquals(33.0, parsed.get(1).getThresholdPercent(), 0.001);
	}

	@Test
	public void testColorParsingHexAndNames()
	{
		Assert.assertEquals(Color.RED, manager.parseColor("red"));
		Assert.assertEquals(Color.YELLOW, manager.parseColor("yellow"));
		Assert.assertEquals(new Color(255, 215, 0), manager.parseColor("#FFD700"));
		Assert.assertEquals(new Color(255, 215, 0), manager.parseColor("FFD700"));
	}

	@Test
	public void testBorderGlowStyleParsing()
	{
		List<HpThresholdRule> rules = manager.parseRules("baba:66:red:border");
		Assert.assertEquals(1, rules.size());
		Assert.assertTrue(rules.get(0).containsStyle(IndicatorStyle.BORDER));
		Assert.assertFalse(rules.get(0).getIndicatorStyles().contains(IndicatorStyle.TILE));
	}

	@Test
	public void testCustomThresholdIconParsing()
	{
		String configStr = "Ba-Ba:50:yellow:⚡, Ba-Ba:20:orange:💀";
		List<HpThresholdRule> rules = manager.parseRules(configStr);
		Assert.assertEquals(2, rules.size());

		Assert.assertEquals("Ba-Ba", rules.get(0).getNpcPattern());
		Assert.assertEquals(50.0, rules.get(0).getThresholdPercent(), 0.001);
		Assert.assertEquals("⚡", rules.get(0).getIconSymbol());

		Assert.assertEquals("Ba-Ba", rules.get(1).getNpcPattern());
		Assert.assertEquals(20.0, rules.get(1).getThresholdPercent(), 0.001);
		Assert.assertEquals("💀", rules.get(1).getIconSymbol());

		HpThresholdRule rule20 = manager.getMatchingThreshold("Ba-Ba", 15.0, configStr, "");
		Assert.assertNotNull(rule20);
		Assert.assertEquals("💀", rule20.getIconSymbol());
	}
}
