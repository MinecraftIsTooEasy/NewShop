package com.inf1nlty.newshop.mixin.world;

import net.minecraft.*;
import net.minecraft.ItemMonsterPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@SuppressWarnings("unchecked")
@Mixin(ItemMonsterPlacer.class)
public class ItemMonsterPlacerMixin {

    @Inject(method = "onItemRightClick", at = @At("TAIL"))
    public void onItemRightClick(EntityPlayer player, float partial_tick, boolean ctrl_is_down, CallbackInfoReturnable<Boolean> cir) {

        World world = player.worldObj;

        if (world.isRemote) return;

        ItemStack stack = player.getHeldItemStack();

        if (stack == null || stack.stackTagCompound == null || !stack.stackTagCompound.getBoolean("ShopBaby")) return;

        int id = stack.getItemSubtype();

        if (id == 90 || id == 91 || id == 92 || id == 100 || id == 95) {
            List<Entity> entities = world.getEntitiesWithinAABB(
                    EntityList.createEntityByID(id, world).getClass(),
                    AxisAlignedBB.getAABBPool().getAABB(
                            (int)(player.posX) - 2, (int)(player.posY) - 2, (int)(player.posZ) - 2,
                            (int)(player.posX) + 2, (int)(player.posY) + 2, (int)(player.posZ) + 2
                    ));

            for (Entity ent : entities) {
                if (ent instanceof EntityAgeable) {
                    ((EntityAgeable) ent).setGrowingAge(-24000);
                }
            }
        }
    }

    @Inject(method = "tryEntityInteraction", at = @At("TAIL"))
    public void onTryEntityInteraction(Entity entity, EntityPlayer player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        World world = entity.worldObj;
        if (world.isRemote) return;

        if (stack == null || stack.stackTagCompound == null || !stack.stackTagCompound.getBoolean("ShopBaby")) return;

        int id = stack.getItemSubtype();

        if (id == 90 || id == 91 || id == 92 || id == 100 || id == 95) {
            List<Entity> entities = world.getEntitiesWithinAABB(
                    EntityList.createEntityByID(id, world).getClass(),
                    AxisAlignedBB.getAABBPool().getAABB(
                            (int)(entity.posX) - 2, (int)(entity.posY) - 2, (int)(entity.posZ) - 2,
                            (int)(entity.posX) + 2, (int)(entity.posY) + 2, (int)(entity.posZ) + 2
                    ));

            for (Entity ent : entities) {
                if (ent instanceof EntityAgeable) {
                    ((EntityAgeable) ent).setGrowingAge(-24000);
                }
            }
        }
    }
}