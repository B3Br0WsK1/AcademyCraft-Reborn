package cn.academy.ability.vanilla.electromaster;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.AcademyCraft;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.electromaster.skill.*;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import cn.lambdalib2.registry.StateEvents;
import java.util.HashSet;



/**
 * @author WeAthFolD
 *
 */
public class CatElectromaster extends Category {

    public static final Skill
        arcGen = ArcGen.instance,
        magManip = MagManip.INSTANCE,
        mineDetect = MineDetect.INSTANCE,
        railgun = Railgun.INSTANCE,
        magMovement = MagMovement.INSTANCE,
        currentCharging = CurrentCharging.INSTANCE,
        bodyIntensify = BodyIntensify.INSTANCE,
        thunderBolt = ThunderBolt.INSTANCE,
        thunderClap = ThunderClap.INSTANCE
        /* ironSand = ??? */;

    public CatElectromaster() {
        super("electromaster");

        setColorStyle(20, 113, 208, 100);

        arcGen.setPosition(24, 46);
        currentCharging.setPosition(55, 18);
        bodyIntensify.setPosition(97, 15);
        mineDetect.setPosition(225, 12);
        magMovement.setPosition(137, 35);
        thunderBolt.setPosition(86, 67);
        railgun.setPosition(164, 59);
        thunderClap.setPosition(204, 80);
        magManip.setPosition(204, 33);

        addSkill(arcGen);
        addSkill(currentCharging);
        addSkill(magMovement);
        addSkill(magManip);
        addSkill(mineDetect);

        // TODO Finish the skill
        // addSkill(ironSand = new IronSand());

        addSkill(bodyIntensify);
        addSkill(thunderBolt);
        addSkill(railgun);
        addSkill(thunderClap);

        VanillaCategories.addGenericSkills(this);

        // Assign deps
        currentCharging.setParent(arcGen, 0.3f);

        magMovement.setParent(arcGen);
        magMovement.addSkillDep(currentCharging, 0.7f);

        magManip.setParent(magMovement, 0.5f);

        bodyIntensify.setParent(arcGen, 1f);
        bodyIntensify.addSkillDep(currentCharging, 1f);

        mineDetect.setParent(magManip, 1f);

        thunderBolt.setParent(arcGen);
        thunderBolt.addSkillDep(currentCharging, 0.7f);

        railgun.setParent(thunderBolt, 0.3f);
        railgun.addSkillDep(magManip, 1f);

        // ironSand.setParent(magManip, 1f);

        thunderClap.setParent(thunderBolt, 1f);
    }

    public static boolean isOreBlock(Block block) {
        return block.builtInRegistryHolder().is(ConventionalBlockTags.ORES);
    }

    /**
     * Resolves a config entry: a block id (e.g. "minecraft:rail"), or a block tag ("#c:ores" or an
     *  ore-dictionary-like name, which is looked up as the "c:" tag of the same name). Tags are checked lazily,
     *  since they aren't bound during mod initialization.
     */
    private static boolean addBlocks(String name, HashSet<Block> target, java.util.List<TagKey<Block>> targetTags) {
        String tagName = name.startsWith("#") ? name.substring(1) : null;
        if (tagName == null) {
            Identifier id = Identifier.tryParse(name);
            if (id != null) {
                java.util.Optional<Block> b = BuiltInRegistries.BLOCK.getOptional(id);
                if (b.isPresent()) {
                    target.add(b.get());
                    return true;
                }
            }
            tagName = name.contains(":") ? name : "c:" + name;
        }
        Identifier tagId = Identifier.tryParse(tagName);
        if (tagId == null) return false;
        targetTags.add(TagKey.create(Registries.BLOCK, tagId));
        return true;
    }

    private static boolean matches(Block block, HashSet<Block> set, java.util.List<TagKey<Block>> tags) {
        if (set.contains(block)) return true;
        for (TagKey<Block> tag : tags) {
            if (block.builtInRegistryHolder().is(tag)) return true;
        }
        return false;
    }

    private static final java.util.List<TagKey<Block>> normalMetalTags = new java.util.ArrayList<>();
    private static final java.util.List<TagKey<Block>> weakMetalTags = new java.util.ArrayList<>();

    private static HashSet<Block> normalMetalBlocks = new HashSet<>();
    private static HashSet<Block> weakMetalBlocks = new HashSet<>();

    private static HashSet<EntityType<?>> metalEntities = new HashSet<>();

    @StateEventCallback
    public static void init(StateEvents.Init event) {

        String[] defaultNBlocks = {
            "rail",
            "iron_bars",
            "iron_block",
            "activator_rail",
            "detector_rail",
            "powered_rail",
            "sticky_piston",
            "piston"
        };
        String[] cfgNBlocks = AcademyCraft.config.getStringList("normalMetalBlocks", "ability", defaultNBlocks,
                "Supported Normal Metal Blocks of Electro Master. The block name and ore dictionary name can be used.");
        for (String block : cfgNBlocks) {
            if (!addBlocks(block, normalMetalBlocks, normalMetalTags)) {
                AcademyCraft.log.error("The block " + block + "is not found!");
            }
        }

        String[] defaultWBlocks = {
            "dispenser",
            "hopper",
            "iron_ore"
        };
        String[] cfgWBlocks = AcademyCraft.config.getStringList("weakMetalBlocks", "ability", defaultWBlocks,
             "Supported Weak Metal Blocks of Electro Master. The block name and ore dictionary name can be used.");
        for (String block : cfgWBlocks) {
            if (!addBlocks(block, weakMetalBlocks, weakMetalTags)) {
                AcademyCraft.log.error("The block " + block + "is not found!");
            }
        }

        String[] defaultEntities = {
            "minecart",
            "chest_minecart",
            "furnace_minecart",
            "tnt_minecart",
            "hopper_minecart",
            "spawner_minecart",
            "command_block_minecart",
            "academy:mag_hook",
            "iron_golem"
        };
        String[] cfgEntities = AcademyCraft.config.getStringList(
            "metalEntities",
            "ability",
            defaultEntities,
            "Supported Metal Entities of Electro Master. The entity name can be used."
        );
        for (String entityName : cfgEntities) {
            Identifier id = Identifier.tryParse(entityName);
            java.util.Optional<EntityType<?>> type = id == null ? java.util.Optional.empty()
                : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (type.isEmpty()) {
                AcademyCraft.log.error("Invalid entity name: " + entityName + " at academy.cfg.");
                continue;
            }

            metalEntities.add(type.get());
        }

    }

    public static boolean isMetalBlock(Block block) {
        return isNormalMetalBlock(block) || isWeakMetalBlock(block);
    }

    public static boolean isNormalMetalBlock(Block block) {
        return matches(block, normalMetalBlocks, normalMetalTags);
    }

    public static boolean isWeakMetalBlock(Block block) {
        return matches(block, weakMetalBlocks, weakMetalTags);
    }

    public static boolean isEntityMetallic(Entity ent) {
        if(metalEntities.isEmpty()) return false;
        return metalEntities.contains(ent.getType());
    }

}