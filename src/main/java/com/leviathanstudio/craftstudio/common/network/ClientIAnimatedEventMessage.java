package com.leviathanstudio.craftstudio.common.network;

import java.util.UUID;

import com.leviathanstudio.craftstudio.client.animation.ClientAnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.IAnimated;
import com.leviathanstudio.craftstudio.common.animation.InfoChannel;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientIAnimatedEventMessage extends IAnimatedEventMessage
{
    public ClientIAnimatedEventMessage(){}
    
    public ClientIAnimatedEventMessage(EnumIAnimatedEvent event, IAnimated animated, short animId){
        super(event, animated, animId);
    }
    
    public ClientIAnimatedEventMessage(EnumIAnimatedEvent event, IAnimated animated, short animId, float keyframeInfo){
        super(event, animated, animId, keyframeInfo);
    }
    
    public ClientIAnimatedEventMessage(EnumIAnimatedEvent event, IAnimated animated, short animId, float keyframeInfo, short optAnimId){
        super(event, animated, animId, keyframeInfo, optAnimId);
    }
    
    public ClientIAnimatedEventMessage(IAnimatedEventMessage eventObj){
        super(eventObj);
    }
    
    public static class ClientIAnimatedEventHandler extends IAnimatedEventHandler implements IMessageHandler<ClientIAnimatedEventMessage, ServerIAnimatedEventMessage>
    {
        @Override
        public ServerIAnimatedEventMessage onMessage(ClientIAnimatedEventMessage message, MessageContext ctx) {
            if (!super.onMessage(message, ctx))
                return null;
            
            boolean succes = message.animated.getAnimationHandler().onClientIAnimatedEvent(message);
            if (succes && message.animated.getAnimationHandler() instanceof ClientAnimationHandler
                    && (message.event == EnumIAnimatedEvent.START_ANIM.getId() || message.event == EnumIAnimatedEvent.STOP_START_ANIM.getId())){
                ClientAnimationHandler hand = (ClientAnimationHandler) message.animated.getAnimationHandler();
                String animName = hand.getAnimNameFromId(message.animId);
                InfoChannel infoC = (InfoChannel) hand.getAnimChannels().get(animName);
                return new ServerIAnimatedEventMessage(EnumIAnimatedEvent.ANSWER_START_ANIM, message.animated, message.animId, infoC.totalFrames);
            }
            return null;
        }

        public Entity getEntityByUUID(MessageContext ctx, long most, long least) {
            UUID uuid = new UUID(most, least);
            for (Entity e : Minecraft.getMinecraft().world.loadedEntityList)
                if (e.getPersistentID().equals(uuid))
                    return e;
            return null;
        }
        
        public TileEntity getTileEntityByPos(MessageContext ctx, int x, int y, int z){
            BlockPos pos = new BlockPos(x, y, z);
            return Minecraft.getMinecraft().world.getTileEntity(pos);
        }
    }
}
