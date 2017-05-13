package fr.zeamateis.test.proxy;

import com.leviathanstudio.craftstudio.CraftStudioApi;

import net.minecraft.util.ResourceLocation;

public class CommonProxy
{
    public void preInit()
    {
    	CraftStudioApi.registerAnim(new ResourceLocation("testmod", "craftstudio/animations/Position.csjsmodelanim"),
                "Position");
    	CraftStudioApi.registerAnim(new ResourceLocation("testmod", "craftstudio/animations/Rotation.csjsmodelanim"),
                "Rotation");
    }

    public void init()
    {
    }
}