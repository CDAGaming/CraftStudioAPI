package com.leviathanstudio.test.common;

import java.util.UUID;

import com.leviathanstudio.craftstudio.CraftStudioApi;
import com.leviathanstudio.craftstudio.common.animation.AnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.IAnimated;

import net.minecraft.entity.EntityCreature;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class EntityTest extends EntityCreature implements IAnimated
{
    protected static AnimationHandler animHandler = CraftStudioApi.getNewAnimationHandler(EntityTest.class);

    static {
        EntityTest.animHandler.addAnim(Mod_Test.MODID, "position", "craftstudio_api_test", true);
        EntityTest.animHandler.addAnim(Mod_Test.MODID, "offset", "craftstudio_api_test", true);
        EntityTest.animHandler.addAnim(Mod_Test.MODID, "streching", "craftstudio_api_test", true);
    }

    public EntityTest(World par1World) {
        super(par1World);
    }

    // Getter for animation handler
    @Override
    public AnimationHandler getAnimationHandler() {
        return EntityTest.animHandler;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        this.getAnimationHandler().animationsUpdate(this);
        
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT && !this.getAnimationHandler().isAnimationActive(Mod_Test.MODID, "streching", this))
            this.getAnimationHandler().clientStartAnimation(Mod_Test.MODID, "streching", this);
    }

    @Override
    public int getDimension() {
        return this.dimension;
    }

    @Override
    public double getX() {
        return this.posX;
    }

    @Override
    public double getY() {
        return this.posY;
    }

    @Override
    public double getZ() {
        return this.posZ;
    }

    @Override
    public boolean isWorldRemote() {
        return this.world.isRemote;
    }

}