package net.zestyblaze.capybara.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.Ingredient;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Lazy;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.zestyblaze.capybara.entity.ai.CapybaraAnimalAttractionGoal;
import net.zestyblaze.capybara.registry.CapybaraEntityInit;
import net.zestyblaze.capybara.registry.CapybaraSoundInit;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
public class CapybaraEntity extends TameableEntity implements NamedScreenHandlerFactory {
    private static final Lazy<Set<ItemConvertible>> TEMPT_ITEMS = new Lazy<>(() -> {
        Stream<ItemConvertible> stream = Stream.of(Blocks.MELON, Items.APPLE, Items.SUGAR_CANE, Items.MELON_SLICE);
        return stream.map(ItemConvertible::asItem).collect(Collectors.toSet());
    });

    private static final TrackedData<Integer> CHESTS = DataTracker.registerData(CapybaraEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private Inventory inventory;

    public CapybaraEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new EscapeDangerGoal(this, 2.0d));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0d));
        this.goalSelector.add(4, new TemptGoal(this, 1.25d, Ingredient.ofItems(TEMPT_ITEMS.get().toArray(new ItemConvertible[0])), false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.25d));
        this.goalSelector.add(6, new FollowOwnerGoal(this, 1.0d, 10.0f, 2.0f, false));
        this.goalSelector.add(7, new WanderAroundGoal(this, 1.0d));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new CapybaraAnimalAttractionGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHESTS, 0);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0d).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2d);
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.65f;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.getItem() == Blocks.MELON.asItem();
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return CapybaraSoundInit.CAPYBARA_AMBIENT;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return CapybaraSoundInit.CAPYBARA_HURT;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return CapybaraSoundInit.CAPYBARA_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_COW_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(this.isInvulnerableTo(source) || source.getAttacker() == getOwner()) {
            return false;
        } else {
            Entity entity = source.getAttacker();
            this.setSitting(false);
            if(entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0f) / 2.0f;
            }
            return super.damage(source, amount);
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();

        if(!this.getWorld().isClient()) {
            if (this.isBreedingItem(stack) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                this.heal((float) Objects.requireNonNull(item.getFoodComponent()).getHunger());
                return ActionResult.SUCCESS;
            }

            if (player.isSneaking() && !this.isBaby()) {
                if (stack.getItem() == Blocks.CHEST.asItem() && this.isTamed()) {
                    if (inventory == null || inventory.size() < 27) {
                        inventory = new SimpleInventory(27);
                        dataTracker.set(CHESTS, 1);
                        if (!player.getAbilities().creativeMode) {
                            stack.decrement(1);
                        }
                        return ActionResult.SUCCESS;
                    } else if (inventory.size() < 54) {
                        Inventory inv = new SimpleInventory(54);
                        for (int i = 0; i < 27; i++) {
                            inv.setStack(i, inventory.getStack(i));
                        }
                        inventory = inv;
                        dataTracker.set(CHESTS, 2);
                        if (!player.getAbilities().creativeMode) {
                            stack.decrement(1);
                        }
                        return ActionResult.SUCCESS;
                    }
                } else if (stack.getItem() == Items.STICK && !this.isBaby()) {
                    this.setSitting(!this.isSitting());
                }
            } else if (TEMPT_ITEMS.get().contains(stack.getItem()) && !isTamed()) {
                if (this.random.nextInt(3) == 0) {
                    this.setOwner(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.getWorld().sendEntityStatus(this, (byte) 7);
                }
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                } else {
                    this.getWorld().sendEntityStatus(this, (byte) 6);
                }
                return ActionResult.SUCCESS;
            } else if (!this.isBaby() && this.isTamed()) {
                player.openHandledScreen(this);
                return ActionResult.SUCCESS;
            }
        }
        return super.interactMob(player, hand);
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return CapybaraEntityInit.CAPYBARA.create(world);
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return this.isBaby() ? 0.5f : 0.9f;
    }

    @Override
    public void tick() {
        super.tick();
        this.floatStrider();
        this.checkBlockCollision();
        if(getPassengerList().isEmpty()) {
            for(Entity e : getWorld().getOtherEntities(this, getBoundingBox().expand(0.5))) {
                if(e instanceof MobEntity && e.getWidth() <= 0.75f && e.getHeight() <= 0.75 && !this.isBaby() && ((MobEntity)e).getGroup() != EntityGroup.AQUATIC && !isSubmergedInWater()) {
                    e.startRiding(this);
                }
            }
        } else if(isSubmergedInWater()) {
            removeAllPassengers();
        }
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new WaterPathNavigator(this, world);
    }

    @Nullable
    public Entity getPrimaryPassenger() {
        Entity entity = this.getFirstPassenger();
        return entity != null && this.canBeControlledByRider() ? entity : null;
    }

    private boolean canBeControlledByRider() {
        return false;
    }

    private void floatStrider() {
        if(this.isSubmergedInWater()) {
            ShapeContext shapeContext = ShapeContext.of(this);
            if(shapeContext.isAbove(FluidBlock.COLLISION_SHAPE, this.getBlockPos(), true) && !this.getWorld().getFluidState(this.getBlockPos().up()).isIn(FluidTags.WATER)) {
                this.setOnGround(true);
            } else {
                this.setVelocity(this.getVelocity().multiply(0.5d).add(0.0d, 0.05d, 0.0d));
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (inventory != null) {
            final NbtList inv = new NbtList();
            for (int i = 0; i < this.inventory.size(); i++) {
                inv.add(inventory.getStack(i).writeNbt(new NbtCompound()));
            }
            nbt.put("Inventory", inv);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Inventory")) {
            final NbtList inv = nbt.getList("Inventory", 10);
            inventory = new SimpleInventory(inv.size());
            for (int i = 0; i < inv.size(); i++) {
                inventory.setStack(i, ItemStack.fromNbt(inv.getCompound(i)));
            }
            dataTracker.set(CHESTS, inv.size() > 27 ? 2 : 1);
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        if (inventory == null) {
            return null;
        }
        return inventory.size() < 54 ? GenericContainerScreenHandler.createGeneric9x3(syncId, inv, inventory) : GenericContainerScreenHandler.createGeneric9x6(syncId, inv, inventory);
        }

    public int getChestCount() {
        return dataTracker.get(CHESTS);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if(getChestCount() > 0) {
            if(!this.getWorld().isClient) {
                int c;
                for(c = 0; c < getChestCount(); c++) {
                    this.dropItem(Blocks.CHEST);
                }

                if(this.inventory != null) {
                    for(int i = 0; i < this.inventory.size(); i++) {
                        ItemStack stack = this.inventory.getStack(i);
                        if(!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) {
                            this.dropStack(stack);
                        }
                    }
                }
            }
        }
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        return super.getOwner();
    }


    static class WaterPathNavigator extends MobNavigation {
        WaterPathNavigator(CapybaraEntity mobEntity, World world) {
            super(mobEntity, world);
        }

        @Override
        protected PathNodeNavigator createPathNodeNavigator(int range) {
            this.nodeMaker = new LandPathNodeMaker();
            return new PathNodeNavigator(this.nodeMaker, range);
        }

        @Override
        protected boolean canWalkOnPath(PathNodeType pathType) {
            return pathType == PathNodeType.WATER || super.canWalkOnPath(pathType);
        }

        @Override
        public boolean isValidPosition(BlockPos pos) {
            return this.world.getBlockState(pos).isOf(Blocks.WATER) || super.isValidPosition(pos);
        }
    }
}
