package cn.academy.tutorial;
import static cn.lambdalib2.render.legacy.GL11.*;
import cn.academy.util.LocalHelper;
import cn.academy.Resources;
import cn.academy.tutorial.ACTutorial.Tag;
import cn.academy.tutorial.client.RecipeHandler;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.vis.CompTransform;
import cn.academy.item.ItemMeta;
import cn.lambdalib2.render.legacy.LegacyGL;
import net.minecraft.world.item.ItemDisplayContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import cn.lambdalib2.util.Color;
import java.util.Random;
import net.fabricmc.api.EnvType;




public final class ViewGroups {

    // This class used a SideOnly hack to make user be able to specify display on init without considering side only issue.

    private static Random random = new Random();

    private static final ViewGroup nothing = new ViewGroup() {
        @Override
        public Tag getTag() {
            return null;
        }
    };

    public static ViewGroup drawsBlock(Block block) {
        return drawsBlock(block, 0);
    }

    @SuppressWarnings("sideonly")
    public static ViewGroup drawsBlock(Block block, int meta) {
        if (client()) return drawsBlockImpl(block, meta);
        return nothing;
    }

    public static ViewGroup drawsItem(Item item) {
        return drawsItem(item, 0);
    }

    @SuppressWarnings("sideonly")
    public static ViewGroup drawsItem(Item item, int metadata) {
        if (client()) return drawsItemImpl(item, metadata);
        return nothing;
    }

    @SuppressWarnings("sideonly")
    public static ViewGroup recipes(Item item) {
        if (client()) return recipesImpl(item);
        return nothing;
    }

    public static ViewGroup recipes(Block block) {
        return recipes(block.asItem());
    }

    @SuppressWarnings("sideonly")
    public static ViewGroup displayModel(
        String model,
        String textureName,
        CompTransform transform
    ) {
        if (client()) return displayModelImpl(model, textureName, transform);
        return nothing;
    }

    public static ViewGroup displayModel(String model, CompTransform transform) {
        return displayModel(model, model, transform);
    }

    public static ViewGroup displayIcon(
            String icon,
            float dx, float dy, 
            float scale, Color color) {
        Identifier icon_res = Resources.getTexture(icon);
        return new ViewGroup() {
            @Override
            @Environment(EnvType.CLIENT)
            public Widget[] getSubViews() {
                return new Widget[] { withDraw(() -> {
                    RenderUtils.loadTexture(icon_res);
                    Colors.bindToGL(color);

                    glDepthFunc(GL_ALWAYS);
                    glPushMatrix();
                    glTranslatef(dx, dy, 0);
                    glScalef(scale, scale, 1);
                    HudUtils.rect(-.5, -.5, 1, 1);
                    glPopMatrix();
                    glDepthFunc(GL_LEQUAL);
                })};
            }

            @Override
            public Tag getTag() {
                return Tag.VIEW;
            }
        };
    }





    // --------- Internal

    @Environment(EnvType.CLIENT)
    private static ViewGroup recipesImpl(Item item) {
        return new ViewGroup() {
            Widget[] result = RecipeHandler.instance.recipeOfItem(item);
            ItemStack stack = new ItemStack(item, 1);

            @Override
            public Widget[] getSubViews() {
                return result;
            }

            @Override
            public Tag getTag() {
                return Tag.CRAFT;
            }

            @Override
            public String getDisplayText() {
                return localCraft(stack);
            }
        };
    }

    @Environment(EnvType.CLIENT)
    private static ViewGroup displayModelImpl(String mdl, String texture, CompTransform transform) {
        ObjLegacyRender model = Resources.getModel(mdl);
        Identifier res_tex = Resources.getTexture("models/" + texture);
        return new ViewGroup() {
            @Override
            public Widget[] getSubViews() {
                return new Widget[] { withDraw(() -> {
                    glPushMatrix();
                    glRotated((GameTimer.getAbsTime() / 50.0) % 360.0, 0, 1, 0);
                    transform.doTransform();
                    RenderUtils.loadTexture(res_tex);
                    model.renderAll();
                    glPopMatrix();
                })};
            }

            @Override
            public Tag getTag() {
                return Tag.VIEW;
            }
        };
    }

    @Environment(EnvType.CLIENT)
    private static ViewGroup drawsBlockImpl(Block block, int metadata) {
        return new ViewGroup() {

            @Override
            public Widget[] getSubViews() {
                return new Widget[] { withDraw(() -> {
                    glTranslated(0.15, 0.1, -1);
                    glRotated((GameTimer.getAbsTime() / 80.0) % 360.0, 0, 1, 0);
                    glScaled(.8, .8, .8);
                    glTranslated(-.5, -.5, -.5);

                    LegacyGL.submitBlock(block.defaultBlockState());
                })};
            }

            @Override
            public Tag getTag() {
                return Tag.VIEW;
            }
        };
    }

    @Environment(EnvType.CLIENT)
    private static ViewGroup drawsItemImpl(Item item, int metadata) {
        ItemStack stack = ItemMeta.of(item, 1, metadata);
        return new ViewGroup() {

            @Override
            public Widget[] getSubViews() {
                return new Widget[] { withDraw(() -> {
                    glTranslated(0.15, 0.1, -1);
                    glRotated((GameTimer.getAbsTime() / 80.0) % 360.0, 0, 1, 0);
                    glScaled(1.2, 1.2, 1.2);
                    LegacyGL.submitItem(stack, ItemDisplayContext.FIXED);
                })};
            }

            @Override
            public Tag getTag() {
                return Tag.VIEW;
            }
        };
    }


    private static boolean client() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    private static String localCraft(ItemStack stack) {
        return LocalHelper.root.getFormatted("ac.tutorial.crafting", stack.getHoverName().getString());
    }

    private ViewGroups() {}
}