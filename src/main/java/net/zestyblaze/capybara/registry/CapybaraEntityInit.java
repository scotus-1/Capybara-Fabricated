package net.zestyblaze.capybara.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.zestyblaze.capybara.Capybara;
import net.zestyblaze.capybara.entity.CapybaraEntity;

public class CapybaraEntityInit {

    public static final EntityType<CapybaraEntity> CAPYBARA = Registry.register(Registries.ENTITY_TYPE, new Identifier(Capybara.MODID, "capybara"), FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CapybaraEntity::new).dimensions(EntityDimensions.fixed(0.8f, 1.1f)).build());

    public static void registerEntities() {
        FabricDefaultAttributeRegistry.register(CAPYBARA, CapybaraEntity.createAttributes());
    }

}
