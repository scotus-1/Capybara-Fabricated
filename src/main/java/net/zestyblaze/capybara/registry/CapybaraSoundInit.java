package net.zestyblaze.capybara.registry;

import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.zestyblaze.capybara.Capybara;

public class CapybaraSoundInit {
    public static SoundEvent CAPYBARA_AMBIENT = SoundEvent.of(new Identifier(Capybara.MODID, "capybara.ambient"));
    public static SoundEvent CAPYBARA_DEATH = SoundEvent.of(new Identifier(Capybara.MODID, "capybara.death"));
    public static SoundEvent CAPYBARA_HURT = SoundEvent.of(new Identifier(Capybara.MODID, "capybara.hurt"));

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, new Identifier(Capybara.MODID, "capybara.ambient"), CAPYBARA_AMBIENT);
        Registry.register(Registries.SOUND_EVENT, new Identifier(Capybara.MODID, "capybara.death"), CAPYBARA_DEATH);
        Registry.register(Registries.SOUND_EVENT, new Identifier(Capybara.MODID, "capybara.hurt"), CAPYBARA_HURT);
    }
}
