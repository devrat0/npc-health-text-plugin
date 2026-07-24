package com.npchealthtext;

public enum NpcDisplayMode
{
	SHOW_TARGET_NPC("Show Target NPC"),
	SHOW_ALL("Show All"),
	SHOW_WHITELISTED("Show Whitelisted"),
	SHOW_WHITELIST_TARGET("Show Whitelist Target");

	private final String name;

	NpcDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
