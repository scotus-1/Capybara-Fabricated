package net.zestyblaze.capybara.registry;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.zestyblaze.capybara.Capybara;

public class CapybaraItemInit {
    public static final Item CAPYBARA_SPAWN_EGG = new SpawnEggItem(CapybaraEntityInit.CAPYBARA, 0x9e5d39, 0x412f24, new FabricItemSettings());

    public static void registerItems() {
        Registry.register(Registries.ITEM, new Identifier(Capybara.MODID, "capybara_spawn_egg"), CAPYBARA_SPAWN_EGG);
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(content -> {
    	content.add(CAPYBARA_SPAWN_EGG);
    });
    }

}
