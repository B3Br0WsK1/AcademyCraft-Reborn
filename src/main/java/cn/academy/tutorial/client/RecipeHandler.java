package cn.academy.tutorial.client;
import static cn.lambdalib2.render.legacy.GL11.*;
import cn.academy.Resources;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.client.gui.EnergyUIHelper;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.GameTimer;
import cn.academy.item.ItemMeta;
import cn.lambdalib2.render.gui.GuiDraw;
import cn.academy.util.ACI18n;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;





// TODO Add ImagFusor and MetalFormer handlers
@Environment(EnvType.CLIENT)
public enum RecipeHandler {
    instance;

    private WidgetContainer windows;
    private Identifier tex = Resources.getTexture("guis/tutorial/crafting_grid");

    @StateEventCallback
    private static void __init(StateEvents.Init ev) {
        instance.windows = CGUIDocument.read(Identifier.parse("academy:guis/tutorial_windows.xml"));
    }

    /**
     * This class displays a 3x3 crafting grid of a recipe.
     */
    static class CraftingGridDisplay extends Widget {

        static final FontOption option = new FontOption(24, FontAlign.CENTER, Colors.white());

        static final int STEP = 43;

        private final StackDisplay[] stacks;
        private final StackDisplay output;
        private final String description;

        CraftingGridDisplay(StackDisplay _output, StackDisplay[] _stacks, String desc) {
            stacks = _stacks;
            output = _output;
            description = desc;

            size(196, 128).centered().scale(0.6f);

            for(int i = 0; i < stacks.length; ++i) {
                int col = i % 3, row = i / 3;

                StackDisplay original = stacks[i];
                if(original != null) {
                    original.disposed = false;
                    original.dirty = true;

                    original.pos(5 + col * STEP, 5 + row * STEP);
                    addWidget(original);
                }
            }

            output.pos(148 + 5, 44 + 5);
            addWidget(output);

            listen(FrameEvent.class, (w, e) ->
            {
                // Renders recipe type hint
                String str = ACI18n.translateFormatted("ac.gui.crafttype." + description);
                Resources.font().draw(str, transform.width / 2 - 30, -28, option);
            });
            addComponent(new DrawTexture().setTex(instance.tex));
        }

    }

    /**
     * This class displays one stack slot, possibly multiple stacks alternating.
     */
    @Environment(EnvType.CLIENT)
    static class StackDisplay extends Widget {

    
//        private static RenderItem itemRender = RenderItem.getInstance();
        static final double ALTERNATE_TIME = 2;

        private final ItemStack[] stacks;
        double lastAlternate;

        private int current = 0;

        public StackDisplay(ItemStack... _stacks) {
            stacks = _stacks;
            transform.setPos(0, 0).setSize(32, 34);
            lastAlternate = GameTimer.getAbsTime() + RandUtils.ranged(-1, 1);

            if(stacks.length != 0) { // Don't draw anything for empty stack
                listen(FrameEvent.class, (w, e) ->
                {
                    double time = GameTimer.getAbsTime();
                    double dt = time - lastAlternate;
                    if(dt > ALTERNATE_TIME) {
                        current = (current + 1) % stacks.length;
                        lastAlternate = time;
                    }

                    ItemStack stack = stacks[current];

                    // Hover effect
                    if(e.hovering) {
                        glColor4d(1, 1, 1, 0.15);
                        HudUtils.colorRect(0, 0, transform.width, transform.height);
                    }

                    // Renders the stack

                    glPushMatrix();
                    glScaled(2, 2, 1);
                    if (GuiDraw.isActive()) {
                        GuiDraw.graphics().item(stack, 0, 0);
                    }
                    glPopMatrix();

                    glPushMatrix();
                    glTranslated(0, 0, 20);
                    if (e.hovering) {
                        EnergyUIHelper.drawTextBox(stack.getHoverName().getString(),
                                e.mx + 10, e.my - 17, 1000,
                                new FontOption(10 / w.scale, FontAlign.LEFT), true);
                    }
                    glPopMatrix();
                });
            }
        }

    }

    // ---- Crafting recipes, read from the mod's recipe JSON files (the client has no access to the server's
    //  recipe manager in 26.x) ----

    private record CraftRecipe(ItemStack output, ItemStack[][] grid, boolean shaped) {}
    private record SmeltRecipe(ItemStack[] input, ItemStack output) {}

    private List<CraftRecipe> craftRecipes;
    private List<SmeltRecipe> smeltRecipes;

    private static ItemStack[] parseIngredient(JsonElement elem) {
        if (elem == null || elem.isJsonNull()) return new ItemStack[0];
        List<ItemStack> ret = new ArrayList<>();
        if (elem.isJsonArray()) {
            for (JsonElement e : elem.getAsJsonArray()) ret.addAll(Arrays.asList(parseIngredient(e)));
        } else if (elem.isJsonPrimitive()) {
            String str = elem.getAsString();
            if (str.startsWith("#")) {
                Identifier id = Identifier.tryParse(str.substring(1));
                if (id != null) {
                    for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id))) {
                        ret.add(new ItemStack(h));
                    }
                }
            } else {
                Identifier id = Identifier.tryParse(str);
                if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(i -> ret.add(new ItemStack(i)));
            }
        } else if (elem.isJsonObject()) { // Legacy {"item": ...} / {"tag": ...} format
            JsonObject o = elem.getAsJsonObject();
            if (o.has("item")) return parseIngredient(o.get("item"));
            if (o.has("tag")) return parseIngredient(new JsonPrimitive("#" + o.get("tag").getAsString()));
        }
        return ret.toArray(new ItemStack[0]);
    }

    private static ItemStack parseResult(JsonElement elem) {
        if (elem == null) return ItemStack.EMPTY;
        if (elem.isJsonPrimitive()) {
            ItemStack[] s = parseIngredient(elem);
            return s.length == 0 ? ItemStack.EMPTY : s[0];
        }
        JsonObject o = elem.getAsJsonObject();
        String id = o.has("id") ? o.get("id").getAsString() : o.has("item") ? o.get("item").getAsString() : null;
        if (id == null) return ItemStack.EMPTY;
        Optional<Item> item = Optional.ofNullable(Identifier.tryParse(id)).flatMap(BuiltInRegistries.ITEM::getOptional);
        if (item.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item.get(), o.has("count") ? o.get("count").getAsInt() : 1);
        if (o.has("components")) {
            JsonObject comps = o.getAsJsonObject("components");
            if (comps.has("academy:meta")) ItemMeta.set(stack, comps.get("academy:meta").getAsInt());
        }
        return stack;
    }

    private void loadRecipes() {
        if (craftRecipes != null) return;
        craftRecipes = new ArrayList<>();
        smeltRecipes = new ArrayList<>();

        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("academy");
        if (mod.isEmpty()) return;
        Optional<Path> dir = mod.get().findPath("data/academy/recipe");
        if (dir.isEmpty()) return;

        try (Stream<Path> files = Files.walk(dir.get())) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).collect(Collectors.toList())) {
                try (Reader r = Files.newBufferedReader(p)) {
                    parseRecipe(JsonParser.parseReader(r).getAsJsonObject());
                } catch (Exception e) {
                    // Ignore malformed recipe
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void parseRecipe(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "";
        switch (type) {
            case "minecraft:crafting_shaped" -> {
                JsonArray pattern = json.getAsJsonArray("pattern");
                JsonObject key = json.getAsJsonObject("key");
                ItemStack[][] grid = new ItemStack[9][];
                for (int row = 0; row < pattern.size() && row < 3; ++row) {
                    String line = pattern.get(row).getAsString();
                    for (int col = 0; col < line.length() && col < 3; ++col) {
                        char ch = line.charAt(col);
                        grid[col + row * 3] = ch == ' ' ? null : parseIngredient(key.get(String.valueOf(ch)));
                    }
                }
                craftRecipes.add(new CraftRecipe(parseResult(json.get("result")), grid, true));
            }
            case "minecraft:crafting_shapeless" -> {
                JsonArray ings = json.getAsJsonArray("ingredients");
                ItemStack[][] grid = new ItemStack[9][];
                for (int i = 0; i < ings.size() && i < 9; ++i) grid[i] = parseIngredient(ings.get(i));
                craftRecipes.add(new CraftRecipe(parseResult(json.get("result")), grid, false));
            }
            case "minecraft:smelting" -> smeltRecipes.add(
                new SmeltRecipe(parseIngredient(json.get("ingredient")), parseResult(json.get("result"))));
            default -> {}
        }
    }

    private boolean matchStack(ItemStack s1, ItemStack s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty())
            return false;
        return s1.getItem() == s2.getItem() && ItemMeta.get(s1) == ItemMeta.get(s2);
    }

    private Widget drawMFRecipe(MetalFormerRecipes.RecipeObject recipe) {
        // CGUI Rocks
        Widget ret = windows.getWidget("MetalFormer").copy();
        ret.getWidget("slot_in").addWidget(new StackDisplay(recipe.input).centered());
        ret.getWidget("slot_out").addWidget(new StackDisplay(recipe.output).centered());
        DrawTexture.get(ret.getWidget("mode")).setTex(recipe.mode.texture);
        return ret;
    }

    private Widget drawIFRecipe(IFRecipe recipe) {
        Widget ret = windows.getWidget("ImagFusor").copy();
        ret.getWidget("slot_in").addWidget(new StackDisplay(recipe.consumeType).centered());
        ret.getWidget("slot_out").addWidget(new StackDisplay(recipe.output).centered());
        TextBox.get(ret.getWidget("amount")).setContent(String.valueOf(recipe.consumeLiquid));

        return ret;
    }

    private Widget drawSmeltRecipe(ItemStack in, ItemStack out) {
        Widget ret = windows.getWidget("Smelting").copy();

        ret.getWidget("slot_in").addWidget(new StackDisplay(in).centered());
        ret.getWidget("slot_out").addWidget(new StackDisplay(out).centered());

        return ret;
    }

    public Widget[] recipeOfBlock(Block block) {
        return recipeOfItem(block.asItem());
    }

    public Widget[] recipeOfItem(Item item) {
        List<ItemStack> lst = new ArrayList<>();
        if (item instanceof cn.academy.item.ISubItemProvider sub) {
            sub.addSubItems(lst::add);
        } else {
            lst.add(new ItemStack(item));
        }

        return lst.stream().flatMap(stk -> Arrays.stream(recipeOfStack(stk))).toArray(Widget[]::new);
    }

    @SuppressWarnings("unchecked")
    public Widget[] recipeOfStack(ItemStack stack) {
        List<Widget> ret = new ArrayList<>();

        loadRecipes();
        for (CraftRecipe o : craftRecipes) {
            if (matchStack(o.output(), stack)) {
                StackDisplay[] arr = new StackDisplay[9];
                for (int i = 0; i < 9; ++i) {
                    if (o.grid()[i] != null) arr[i] = new StackDisplay(o.grid()[i]);
                }
                ret.add(new CraftingGridDisplay(new StackDisplay(o.output()), arr, o.shaped() ? "shaped" : "shapeless"));
            }
        }
        { // IF Recipes
            List<Widget> recipes = ImagFusorRecipes.INSTANCE.getAllRecipe().stream()
                    .filter(recipe -> itemDamageEqual(recipe.output, stack))
                    .map(instance::drawIFRecipe)
                    .collect(Collectors.toList());
            ret.addAll(recipes);
        }
        { // MF Recipes
            List<Widget> recipes = MetalFormerRecipes.INSTANCE.getAllRecipes().stream()
                    .filter(recipe -> itemDamageEqual(recipe.output, stack))
                    .map(instance::drawMFRecipe)
                    .collect(Collectors.toList());
            ret.addAll(recipes);
        }
        { // Smelting
            for (SmeltRecipe r : smeltRecipes) {
                if (r.output().getItem() == stack.getItem() && r.input().length > 0) {
                    ret.add(drawSmeltRecipe(r.input()[0], stack));
                }
            }
        }

        return ret.toArray(new Widget[ret.size()]);
    }

    private boolean itemDamageEqual(ItemStack s1, ItemStack s2) {
        if (s1 == null || s2 == null) { return false; }

        return s1.getItem() == s2.getItem() && ItemMeta.get(s1) == ItemMeta.get(s2);
    }

}