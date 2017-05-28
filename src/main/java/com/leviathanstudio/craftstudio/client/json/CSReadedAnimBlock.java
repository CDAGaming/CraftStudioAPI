package com.leviathanstudio.craftstudio.client.json;

import java.util.HashMap;
import java.util.Map;

import com.leviathanstudio.craftstudio.util.math.Vector3f;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Class that store information relative to an animated block.</br>
 *
 * @author Timmypote
 */
@SideOnly(Side.CLIENT)
public class CSReadedAnimBlock
{
    private String                       name;
    private Map<Integer, ReadedKeyFrame> keyFrames = new HashMap<>();

    /**
     * Add an element to a keyframe. If the keyframe does exist it's created.
     *
     * @param keyFrame
     *            Key of the keyframe.
     * @param type
     *            Type of the element. See {@link CSReadedAnimBlock}.
     * @param value
     *            Value of the element.
     */
    public void addKFElement(int keyFrame, FrameType type, Vector3f value) {
        if (!this.keyFrames.containsKey(keyFrame))
            this.keyFrames.put(keyFrame, new ReadedKeyFrame());
        switch (type) {
            case POSITION:
                this.keyFrames.get(keyFrame).position = value;
                break;
            case ROTATION:
                this.keyFrames.get(keyFrame).rotation = value;
                break;
            case OFFSET:
                this.keyFrames.get(keyFrame).offset = value;
                break;
            case SIZE:
                this.keyFrames.get(keyFrame).size = value;
                break;
            case STRETCH:
                this.keyFrames.get(keyFrame).stretching = value;
                break;
        }
    }

    /**
     * Class used to store informations relative to keyframes.
     */
    public class ReadedKeyFrame
    {
        public Vector3f position, rotation, offset, size, stretching;
    }

    public enum FrameType {
        POSITION, ROTATION, OFFSET, SIZE, STRETCH;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, ReadedKeyFrame> getKeyFrames() {
        return this.keyFrames;
    }

    public void setKeyFrames(Map<Integer, ReadedKeyFrame> keyFrames) {
        this.keyFrames = keyFrames;
    }
}