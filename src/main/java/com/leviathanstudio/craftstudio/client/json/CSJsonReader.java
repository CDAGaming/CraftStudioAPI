package com.leviathanstudio.craftstudio.client.json;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import org.apache.commons.io.Charsets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leviathanstudio.craftstudio.CraftStudioApi;
import com.leviathanstudio.craftstudio.client.json.CSReadedAnimBlock.FrameType;
import com.leviathanstudio.craftstudio.common.exceptions.CSMalformedJsonException;
import com.leviathanstudio.craftstudio.common.exceptions.CSResourceNotFoundException;
import com.leviathanstudio.craftstudio.util.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Class used to read json and extract a {@link CSReadedModel} or a
 * {@link CSReadedAnim}.
 *
 * @author Timmypote
 * @author ZeAmateis
 * @author Phenix246
 */
@SideOnly(Side.CLIENT)
public class CSJsonReader
{
    private JsonObject root;
    private String     modid, ress;

    /**
     * Create a {@link CSJsonReader} link to the resource.
     *
     * @param resourceIn
     *            Location of the <i>model.csjsmodel</i> or the
     *            <i>anim.csjsmodelanim</i>.
     * @throws CraftStudioModelNotFound
     *             If the files doesn't exist.
     *
     * @see #readModel()
     * @see #readAnim()
     */
    public CSJsonReader(ResourceLocation resourceIn) throws CSResourceNotFoundException {
        JsonParser jsonParser = new JsonParser();
        BufferedReader reader = null;
        IResource iResource = null;
        StringBuilder strBuilder = new StringBuilder();
        this.ress = resourceIn.toString();

        try {
            iResource = Minecraft.getMinecraft().getResourceManager().getResource(resourceIn);
            reader = new BufferedReader(new InputStreamReader(iResource.getInputStream(), Charsets.UTF_8));
            String s;
            while ((s = reader.readLine()) != null)
                strBuilder.append(s);
            Object object = jsonParser.parse(strBuilder.toString());
            this.root = (JsonObject) object;
            this.modid = iResource.getResourceLocation().getResourceDomain();
        } catch (FileNotFoundException fnfe) {
            throw new CSResourceNotFoundException(this.ress);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (iResource != null)
                    iResource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Extract a {@link CSReadedModel} from a .csjsmodel file.
     *
     * @return A new {@link CSReadedModel} containing the informations of the
     *         file.
     * @throws CSMalformedJsonException
     *             If the json does match the model structure
     */
    public CSReadedModel readModel() throws CSMalformedJsonException {

        CSReadedModel model = new CSReadedModel();
        CSReadedModelBlock parent;
        JsonObject jsonBlock;
        JsonElement jsEl;

        model.setModid(strNormalize(this.modid));
        jsEl = this.root.get("title");
        if (jsEl == null)
            throw new CSMalformedJsonException("title", "String", this.ress);
        model.setName(strNormalize(jsEl.getAsString()));

        JsonArray tree = this.root.getAsJsonArray("tree");
        if (tree == null)
            throw new CSMalformedJsonException("tree", "Array", this.ress);
        for (JsonElement element : tree)
            if (element.isJsonObject()) {
                jsonBlock = element.getAsJsonObject();

                parent = new CSReadedModelBlock();
                model.getParents().add(parent);

                try {
                    readModelBlock(jsonBlock, parent);
                } catch (NullPointerException | ClassCastException | IllegalStateException e) {
                    // e.printStackTrace();
                    throw new CSMalformedJsonException(parent.getName() != null ? parent.getName() : "a parent block without name", this.ress);
                }
            }
        return model;
    }

    /**
     * Extract a block (and all its children) from a {@link JsonObject} and
     * place it in the {@link CSReadedModelBlock}.
     *
     * @param jsonBlock
     *            The object to read the information.
     * @param block
     *            The block to place the information.
     */
    private static void readModelBlock(JsonObject jsonBlock, CSReadedModelBlock block) {
        readModelBlock(jsonBlock, block, null);
    }

    /**
     * Extract a child block from a {@link JsonObject} and place it in the
     * {@link CSReadedModelBlock}.
     *
     * @param jsonBlock
     *            The object to read the information.
     * @param block
     *            The block to place the information.
     * @param parentOffset
     *            The offset from pivot of the parent block.
     */
    private static void readModelBlock(JsonObject jsonBlock, CSReadedModelBlock block, Vector3f parentOffset) {
        final int[] vertexOrderConvert = new int[] { 3, 2, 1, 0, 6, 7, 4, 5 };
        JsonObject jsonChild;
        CSReadedModelBlock child;

        block.setName(strNormalize(jsonBlock.get("name").getAsString()));

        JsonArray array = jsonBlock.getAsJsonArray("size"), vertexArray;
        float sizeX = array.get(0).getAsFloat();
        float sizeY = array.get(1).getAsFloat();
        float sizeZ = array.get(2).getAsFloat();

        array = jsonBlock.getAsJsonArray("position");
        float posX = array.get(0).getAsFloat();
        float posY = array.get(1).getAsFloat();
        float posZ = array.get(2).getAsFloat();

        array = jsonBlock.getAsJsonArray("rotation");
        float rotationX = array.get(0).getAsFloat();
        float rotationY = array.get(1).getAsFloat();
        float rotationZ = array.get(2).getAsFloat();

        array = jsonBlock.getAsJsonArray("offsetFromPivot");
        float pivotOffsetX = array.get(0).getAsFloat();
        float pivotOffsetY = array.get(1).getAsFloat();
        float pivotOffsetZ = array.get(2).getAsFloat();

        // It may need improvement

        array = jsonBlock.getAsJsonArray("vertexCoords");
        Vector3f vertex;
        if (array != null) {
            block.setVertex(new float[8][3]);
            for (int i = 0; i < 8; i++) {
                vertexArray = array.get(vertexOrderConvert[i]).getAsJsonArray();
                block.getVertex()[i][0] = vertexArray.get(0).getAsFloat() + pivotOffsetX;
                block.getVertex()[i][1] = -vertexArray.get(1).getAsFloat() - pivotOffsetY;
                block.getVertex()[i][2] = -vertexArray.get(2).getAsFloat() - pivotOffsetZ;
            }
        }
        else
            block.setBoxSetup(new Vector3f(-sizeX / 2 + pivotOffsetX, sizeY / 2 - pivotOffsetY, sizeZ / 2 - pivotOffsetZ));

        if (parentOffset == null)
            block.setRotationPoint(new Vector3f(posX, -posY + 24, -posZ));
        else
            block.setRotationPoint(new Vector3f(posX + parentOffset.x, -posY + parentOffset.y, -posZ + parentOffset.z));
        block.setRotation(new Vector3f(rotationX, -rotationY, -rotationZ));

        block.setSize(new Vector3f(sizeX, -sizeY, -sizeZ));

        array = jsonBlock.getAsJsonArray("texOffset");
        block.getTexOffset()[0] = array.get(0).getAsInt();
        block.getTexOffset()[1] = array.get(1).getAsInt();

        array = jsonBlock.getAsJsonArray("children");
        for (JsonElement element : array) {
            jsonChild = element.getAsJsonObject();
            child = new CSReadedModelBlock();
            block.getChilds().add(child);
            readModelBlock(jsonChild, child, new Vector3f(pivotOffsetX, -pivotOffsetY, -pivotOffsetZ));
        }

    }

    /**
     * Extract a {@link CSReadedAnim} from a .csjsmodelanim file.
     *
     * @return A new {@link CSReadedAnim} containing the informations of the
     *         file.
     * @throws CSMalformedJsonException
     *             If the json does match the animation structure
     */
    public CSReadedAnim readAnim() throws CSMalformedJsonException {

        CSReadedAnim anim = new CSReadedAnim();
        CSReadedAnimBlock block;
        JsonObject jsonBlock;
        JsonElement jsEl;

        anim.setModid(strNormalize(this.modid));
        jsEl = this.root.get("title");
        if (jsEl == null)
            throw new CSMalformedJsonException("title", "String", this.ress);
        anim.setName(strNormalize(jsEl.getAsString()));
        jsEl = this.root.get("duration");
        if (jsEl == null)
            throw new CSMalformedJsonException("duration", "Integer", this.ress);
        anim.setDuration(jsEl.getAsInt());
        jsEl = this.root.get("holdLastKeyframe");
        if (jsEl == null)
            throw new CSMalformedJsonException("holdLastKeyframe", "Boolean", this.ress);
        anim.setHoldLastK(jsEl.getAsBoolean());

        jsEl = this.root.get("nodeAnimations");
        if (jsEl == null)
            throw new CSMalformedJsonException("nodeAnimations", "Object", this.ress);
        JsonObject nodeAnims = jsEl.getAsJsonObject();
        for (Entry<String, JsonElement> entry : nodeAnims.entrySet()) {
            block = new CSReadedAnimBlock();
            anim.getBlocks().add(block);
            try {
                readAnimBlock(entry, block);
            } catch (Exception e) {
                CraftStudioApi.getLogger().error(e.getMessage());
                throw new CSMalformedJsonException(block.getName() != null ? block.getName() : "a block without name", this.ress);
            }
        }
        return anim;
    }

    /**
     * Extract a block's informations and place them in a
     * {@link CSReadedAnimBlock}.
     *
     * @param entry
     *            The entry containing the informations.
     * @param block
     *            The block to store the informations.
     */
    private static void readAnimBlock(Entry<String, JsonElement> entry, CSReadedAnimBlock block) {
        block.setName(strNormalize(entry.getKey()));
        JsonObject objBlock = entry.getValue().getAsJsonObject(), objField;

        objField = objBlock.get("position").getAsJsonObject();
        addKFElement(objField, block, FrameType.POSITION);
        objField = objBlock.get("offsetFromPivot").getAsJsonObject();
        addKFElement(objField, block, FrameType.OFFSET);
        objField = objBlock.get("size").getAsJsonObject();
        addKFElement(objField, block, FrameType.SIZE);
        objField = objBlock.get("rotation").getAsJsonObject();
        addKFElement(objField, block, FrameType.ROTATION);
        objField = objBlock.get("stretch").getAsJsonObject();
        addKFElement(objField, block, FrameType.STRETCH);
    }

    /**
     * Extract the element asked of all the keyframes.
     *
     * @param obj
     *            The object with the keyframes.
     * @param block
     *            The block to store the keyframes.
     * @param type
     *            type of element to add. See {@link CSReadedAnimBlock}.
     */
    private static void addKFElement(JsonObject obj, CSReadedAnimBlock block, FrameType type) {
        int keyFrame;
        Vector3f value;
        JsonArray array;

        for (Entry<String, JsonElement> entry : obj.entrySet()) {
            keyFrame = Integer.parseInt(entry.getKey());
            array = entry.getValue().getAsJsonArray();
            switch (type) {
                case POSITION:
                    value = new Vector3f(array.get(0).getAsFloat(), -array.get(1).getAsFloat(), -array.get(2).getAsFloat());
                    break;
                case ROTATION:
                    value = new Vector3f(array.get(0).getAsFloat(), -array.get(1).getAsFloat(), -array.get(2).getAsFloat());
                    break;
                default:
                    value = new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
            }
            block.addKFElement(keyFrame, type, value);
        }
    }

    private static String strNormalize(String str) {
        return str.replaceAll("[^\\dA-Za-z ]", "_").replaceAll("\\s+", "_").replaceAll("[^\\p{ASCII}]", "_");
    }

}