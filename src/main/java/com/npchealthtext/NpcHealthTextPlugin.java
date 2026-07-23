package com.npchealthtext;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "NPC Health Text",
	description = "Adds a small, highly customizable text overlay above NPC health bars showing current health and percentage.",
	tags = {"health", "hp", "overlay", "npc", "bar", "text"},
	enabledByDefault = true
)
public class NpcHealthTextPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private NpcHealthTextOverlay overlay;

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(overlay);
		overlayManager.add(overlay);
		log.info("NPC Health Text plugin started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(overlay);
		overlayManager.remove(overlay);
		log.info("NPC Health Text plugin stopped");
	}

	@Provides
	NpcHealthTextConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcHealthTextConfig.class);
	}
}
