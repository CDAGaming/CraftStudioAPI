package com.leviathanstudio.craftstudio.client.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.leviathanstudio.craftstudio.CraftStudioApi;
import com.leviathanstudio.craftstudio.client.model.CSModelRenderer;
import com.leviathanstudio.craftstudio.client.util.math.Quaternion;
import com.leviathanstudio.craftstudio.client.util.math.Vector3f;
import com.leviathanstudio.craftstudio.common.animation.AnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.Channel;
import com.leviathanstudio.craftstudio.common.animation.CustomChannel;
import com.leviathanstudio.craftstudio.common.animation.IAnimated;
import com.leviathanstudio.craftstudio.common.animation.InfoChannel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientAnimationHandler extends AnimationHandler
{
    /** List of all the activated animations of this element. */
    private List<InfoChannel>            animCurrentChannels = new ArrayList<>();
    /** Previous time of every active animation. */
    private Map<String, Long>              animPrevTime        = new HashMap<>();
    /** Current frame of every active animation. */
    private Map<String, Float>             animCurrentFrame    = new HashMap<>();
    /** Map with all the animations. */
    private HashMap<String, InfoChannel> animChannels        = new HashMap<>();

    public ClientAnimationHandler(IAnimated animated, Profiler profiler) {
        super(animated, profiler);
    }

    @Override
    public void addAnim(String modid, String animNameIn, String modelNameIn, boolean looped) {
        ResourceLocation anim = new ResourceLocation(modid, animNameIn), model = new ResourceLocation(modid, modelNameIn);
        this.profiler.startSection("putAnim");
        this.animChannels.put(anim.toString(), new CSAnimChannel(anim, model, false));
        this.profiler.endSection();
    }

    @Override
    public void addAnim(String modid, String animNameIn, String modelNameIn, CustomChannel customChannelIn) {
        ResourceLocation anim = new ResourceLocation(modid, animNameIn), model = new ResourceLocation(modid, modelNameIn);
        this.profiler.startSection("putAnim");
        this.animChannels.put(anim.toString(), customChannelIn);
        this.profiler.endSection();
    }

    @Override
    public void addAnim(String modid, String invertedAnimationName, String animationToInvert) {
        ResourceLocation anim = new ResourceLocation(modid, invertedAnimationName);
        ResourceLocation inverted = new ResourceLocation(modid, animationToInvert);
        if (this.animChannels.get(inverted.toString()) instanceof ClientChannel){
        	ClientChannel channel = ((ClientChannel)this.animChannels.get(inverted.toString())).getInvertedChannel(invertedAnimationName);
        	channel.name = anim.toString();
        	this.profiler.startSection("putAnim");
        	this.animChannels.put(anim.toString(), channel);
        	this.profiler.endSection();
        }
    }

    @Override
    public void startAnimation(String animationNameIn, float startingFrame) {
        if (AnimationHandler.isWorldRemote(this.animatedElement))
            this.clientStartAnimation(animationNameIn, startingFrame);
    }

    public void clientStartAnimation(String res, float startingFrame) {
        if (this.animChannels.get(res) != null) {
            InfoChannel selectedChannel = this.animChannels.get(res);
            int indexToRemove = this.animCurrentChannels.indexOf(selectedChannel);
            if (indexToRemove != -1)
                this.animCurrentChannels.remove(indexToRemove);

            this.animCurrentChannels.add(selectedChannel);
            this.animPrevTime.put(selectedChannel.name, System.nanoTime());
            this.animCurrentFrame.put(selectedChannel.name, startingFrame);
        }
        else
            CraftStudioApi.getLogger().warn("The animation called " + res + " doesn't exist!");
    }

    @Override
    public void stopAnimation(String res) {
        if (AnimationHandler.isWorldRemote(this.animatedElement))
            this.clientStopAnimation(res);
    }

    public void clientStopAnimation(String res) {
        InfoChannel selectedChannel = this.animChannels.get(res);
        if (selectedChannel != null) {
            int indexToRemove = this.animCurrentChannels.indexOf(selectedChannel);
            if (indexToRemove != -1) {
                this.animCurrentChannels.remove(indexToRemove);
                this.animPrevTime.remove(res);
                this.animCurrentFrame.remove(res);
            }
        }
        else
            CraftStudioApi.getLogger().warn("The animation stopped " + res + " doesn't exist!");
    }

    /**
     * Update the animation
     */
    @Override
    public void animationsUpdate() {

        for (Iterator<InfoChannel> it = this.animCurrentChannels.iterator(); it.hasNext();) {
            InfoChannel anim = it.next();
            float prevFrame = this.animCurrentFrame.get(anim.name);
            boolean canUpdate = this.canUpdateAnimation(anim);
            if (this.animCurrentFrame.get(anim.name) != null)
                this.fireAnimationEvent(anim, prevFrame, this.animCurrentFrame.get(anim.name));
            if (!canUpdate) {
                it.remove();
                this.animPrevTime.remove(anim.name);
                this.animCurrentFrame.remove(anim.name);
            }
        }
    }

    /**
     * Check if animation is active
     */
    @Override
    public boolean isAnimationActive(String name) {
        boolean animAlreadyUsed = false;
        for (InfoChannel anim : this.animCurrentChannels)
            if (anim.name != null)
                if (anim.name.equals(name) && this.animCurrentFrame.get(anim.name) < anim.totalFrames - 1) {
                    animAlreadyUsed = true;
                    break;
                }
        return animAlreadyUsed;
    }

    /**
     * Check if an hold animation is active
     */
    public boolean isHoldAnimationActive(String name) {
        boolean animAlreadyUsed = false;
        for (InfoChannel anim : this.animCurrentChannels)
            if (anim.name != null)
                if (anim.name.equals(name)) {
                    animAlreadyUsed = true;
                    break;
                }
        return animAlreadyUsed;
    }

    /**
     * Check if animation can be updated
     */
    @Override
    public boolean canUpdateAnimation(Channel channel) {
        long currentTime = System.nanoTime();
        if (!(channel instanceof InfoChannel))
            return false;
        InfoChannel infoChannel = (InfoChannel) channel;
        if (!ClientAnimationHandler.isGamePaused()) {
            if (infoChannel instanceof ClientChannel) {
            	ClientChannel clientChannel = (ClientChannel) infoChannel;
                long prevTime = this.animPrevTime.get(channel.name);
                float prevFrame = this.animCurrentFrame.get(channel.name);

                double deltaTime = (currentTime - prevTime) / 1000000000.0;
                float numberOfSkippedFrames = (float) (deltaTime * channel.fps);

                float currentFrame = prevFrame + numberOfSkippedFrames;

                /*
                 * -1 as the first frame mustn't be "executed" as it is the
                 * starting situation
                 */
                if (currentFrame < channel.totalFrames - 1) {
                    this.animPrevTime.put(channel.name, currentTime);
                    this.animCurrentFrame.put(channel.name, currentFrame);
                    return true;
                }
                else {
                    if (clientChannel.getAnimationMode() == EnumAnimationMode.LOOP) {
                        this.animPrevTime.put(channel.name, currentTime);
                        this.animCurrentFrame.put(channel.name, 0F);
                        return true;
                    }
                    else if (clientChannel.getAnimationMode() == EnumAnimationMode.HOLD) {
                        this.animPrevTime.put(channel.name, currentTime);
                        this.animCurrentFrame.put(channel.name, (float) channel.totalFrames - 1);
                        return true;
                    }
                    return false;
                }
            }
            else
                return true;
        }
        else {
            this.animPrevTime.put(channel.name, currentTime);
            return true;
        }

    }

    /**
     * Check if game is paused (Exit screen)
     */
    public static boolean isGamePaused() {
        Minecraft MC = Minecraft.getMinecraft();
        return MC.isSingleplayer() && MC.currentScreen != null && MC.currentScreen.doesGuiPauseGame() && !MC.getIntegratedServer().getPublic();
    }

    /**
     * Apply animations if running or apply initial values. Must be called only
     * by the model class.
     */
    public static void performAnimationInModel(List<CSModelRenderer> parts, IAnimated entity) {
        for (CSModelRenderer entry : parts)
            performAnimationForBlock(entry, entity);
    }

    /**
     * Apply animations for model block
     */
    public static void performAnimationForBlock(CSModelRenderer block, IAnimated entity) {
        String boxName = block.boxName;

        if (entity.getAnimationHandler() instanceof ClientAnimationHandler) {
            ClientAnimationHandler animHandler = (ClientAnimationHandler) entity.getAnimationHandler();

            if (block.childModels != null)
                for (ModelRenderer child : block.childModels)
                    if (child instanceof CSModelRenderer) {
                        CSModelRenderer childModel = (CSModelRenderer) child;
                        performAnimationForBlock(childModel, entity);
                    }

            Vector3f defaultPos = new Vector3f(block.getDefaultRotationPointX(), block.getDefaultRotationPointY(), block.getDefaultRotationPointZ());
            block.resetRotationPoint();
            block.resetRotationMatrix();

            for (InfoChannel channel : animHandler.animCurrentChannels)
                if (channel instanceof ClientChannel) {
                	ClientChannel clientChannel = (ClientChannel) channel;
                    float currentFrame = animHandler.animCurrentFrame.get(clientChannel.name);

                    // Rotations
                    KeyFrame prevRotationKeyFrame = clientChannel.getPreviousRotationKeyFrameForBox(boxName,
                            animHandler.animCurrentFrame.get(clientChannel.name));
                    int prevRotationKeyFramePosition = prevRotationKeyFrame != null ? clientChannel.getKeyFramePosition(prevRotationKeyFrame) : 0;

                    KeyFrame nextRotationKeyFrame = clientChannel.getNextRotationKeyFrameForBox(boxName, animHandler.animCurrentFrame.get(clientChannel.name));
                    int nextRotationKeyFramePosition = nextRotationKeyFrame != null ? clientChannel.getKeyFramePosition(nextRotationKeyFrame) : 0;

                    float SLERPProgress = (currentFrame - prevRotationKeyFramePosition)
                            / (nextRotationKeyFramePosition - prevRotationKeyFramePosition);
                    if (SLERPProgress > 1F || SLERPProgress < 0F)
                        SLERPProgress = 1F;

                    if (prevRotationKeyFramePosition == 0 && prevRotationKeyFrame == null && !(nextRotationKeyFramePosition == 0)) {
                        Quaternion currentQuat = new Quaternion();
                        currentQuat.slerp(block.getDefaultRotationAsQuaternion(), nextRotationKeyFrame.modelRenderersRotations.get(boxName),
                                SLERPProgress);
                        block.getRotationMatrix().set(currentQuat).transpose();
                    }
                    else if (prevRotationKeyFramePosition == 0 && prevRotationKeyFrame != null && !(nextRotationKeyFramePosition == 0)) {
                        Quaternion currentQuat = new Quaternion();
                        currentQuat.slerp(prevRotationKeyFrame.modelRenderersRotations.get(boxName),
                                nextRotationKeyFrame.modelRenderersRotations.get(boxName), SLERPProgress);
                        block.getRotationMatrix().set(currentQuat).transpose();
                    }
                    else if (prevRotationKeyFramePosition != 0 && nextRotationKeyFramePosition != 0) {
                        Quaternion currentQuat = new Quaternion();
                        currentQuat.slerp(prevRotationKeyFrame.modelRenderersRotations.get(boxName),
                                nextRotationKeyFrame.modelRenderersRotations.get(boxName), SLERPProgress);
                        block.getRotationMatrix().set(currentQuat).transpose();
                    }

                    // Translations
                    KeyFrame prevTranslationKeyFrame = clientChannel.getPreviousTranslationKeyFrameForBox(boxName,
                            animHandler.animCurrentFrame.get(clientChannel.name));
                    int prevTranslationsKeyFramePosition = prevTranslationKeyFrame != null ? clientChannel.getKeyFramePosition(prevTranslationKeyFrame) : 0;

                    KeyFrame nextTranslationKeyFrame = clientChannel.getNextTranslationKeyFrameForBox(boxName,
                            animHandler.animCurrentFrame.get(clientChannel.name));
                    int nextTranslationsKeyFramePosition = nextTranslationKeyFrame != null ? clientChannel.getKeyFramePosition(nextTranslationKeyFrame) : 0;

                    float LERPProgress = (currentFrame - prevTranslationsKeyFramePosition)
                            / (nextTranslationsKeyFramePosition - prevTranslationsKeyFramePosition);
                    if (LERPProgress > 1F)
                        LERPProgress = 1F;

                    if (prevTranslationsKeyFramePosition == 0 && prevTranslationKeyFrame == null && !(nextTranslationsKeyFramePosition == 0)) {
                        Vector3f startPosition = block.getPositionAsVector();
                        Vector3f endPosition = nextTranslationKeyFrame.modelRenderersTranslations.get(boxName);
                        Vector3f currentPosition = new Vector3f(startPosition);
                        currentPosition.interpolate(endPosition, LERPProgress);
                        block.setRotationPoint(currentPosition.x, currentPosition.y, currentPosition.z);
                    }
                    else if (prevTranslationsKeyFramePosition == 0 && prevTranslationKeyFrame != null && !(nextTranslationsKeyFramePosition == 0)) {
                        Vector3f startPosition = prevTranslationKeyFrame.modelRenderersTranslations.get(boxName);
                        Vector3f endPosition = nextTranslationKeyFrame.modelRenderersTranslations.get(boxName);
                        Vector3f currentPosition = new Vector3f(startPosition);
                        currentPosition.interpolate(endPosition, LERPProgress);
                        block.setRotationPoint(currentPosition.x, currentPosition.y, currentPosition.z);
                    }
                    else if (prevTranslationsKeyFramePosition != 0 && nextTranslationsKeyFramePosition != 0) {
                        Vector3f startPosition = prevTranslationKeyFrame.modelRenderersTranslations.get(boxName);
                        Vector3f endPosition = nextTranslationKeyFrame.modelRenderersTranslations.get(boxName);
                        Vector3f currentPosition = new Vector3f(startPosition);
                        currentPosition.interpolate(endPosition, LERPProgress);
                        block.setRotationPoint(currentPosition.x, currentPosition.y, currentPosition.z);
                    }
                }
                else if (channel instanceof CustomChannel)
                    ((CustomChannel) channel).update(block, entity);
        }

    }

    @Override
    public void fireAnimationEvent(Channel anim, float prevFrame, float frame) {}

    /** Getters */
    public List<InfoChannel> getAnimCurrentChannels() {
        return this.animCurrentChannels;
    }

    /** Getters */
    public Map<String, Long> getAnimPrevTime() {
        return this.animPrevTime;
    }

    /** Getters */
    public Map<String, Float> getAnimCurrentFrame() {
        return this.animCurrentFrame;
    }

    /** Getters */
    public HashMap<String, InfoChannel> getAnimChannels() {
        return this.animChannels;
    }

}