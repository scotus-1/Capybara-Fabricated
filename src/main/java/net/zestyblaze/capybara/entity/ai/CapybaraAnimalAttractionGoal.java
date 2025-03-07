package net.zestyblaze.capybara.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.zestyblaze.capybara.config.CapybaraModConfig;

public class CapybaraAnimalAttractionGoal extends Goal {
    private final MobEntity entity;

    public CapybaraAnimalAttractionGoal(MobEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean canStart() {
        return entity.age % 60 == 0 && entity.getPassengerList().isEmpty();
    }

    @Override
    public boolean shouldContinue() {
        return entity.age % 80 != 0;
    }

    @Override
    public void start() {
        super.start();
        if(CapybaraModConfig.get().animalAttractionGoal) {
            for (MobEntity mobEntity : entity.getWorld().getEntitiesByClass(MobEntity.class, entity.getBoundingBox().expand(5), e -> e != entity && e.getVehicle() == null)) {
                if (mobEntity.getWidth() <= 0.75f && mobEntity.getHeight() <= 0.75f) {
                    if (mobEntity instanceof TameableEntity mob) {
                        if (mob.isSitting()) {
                            return;
                        }
                    }
                    mobEntity.getNavigation().startMovingTo(entity, mobEntity.getMovementSpeed() + 0.4);
                }
            }
        }
    }
}
