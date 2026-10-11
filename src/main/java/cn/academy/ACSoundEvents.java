package cn.academy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Registers every sound event declared in assets/academy/sounds.json, so that they can be used by commands
 *  and be sent over network by reference.
 */
public final class ACSoundEvents {

    private ACSoundEvents() {}

    public static void init() {
        try (InputStream is = ACSoundEvents.class.getResourceAsStream("/assets/academy/sounds.json")) {
            if (is == null) {
                AcademyCraft.log.error("sounds.json not found, no sound event registered");
                return;
            }
            JsonObject obj = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : obj.keySet()) {
                Identifier id = Identifier.fromNamespaceAndPath(AcademyCraft.MODID, key);
                if (!BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                    Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
                }
            }
        } catch (Exception e) {
            AcademyCraft.log.error("Failed to register sound events", e);
        }
    }

}
