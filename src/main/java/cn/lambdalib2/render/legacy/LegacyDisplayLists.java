package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Emulated display lists. While a list is being compiled, immediate mode calls (begin/end, vertices, tex coords,
 *  colors) are recorded in local coordinates instead of being executed; calling a list replays them with the
 *  current matrix.
 */
@Environment(EnvType.CLIENT)
final class LegacyDisplayLists {

    private LegacyDisplayLists() {}

    private interface Op {
        void run();
    }

    private static final Map<Integer, List<Op>> lists = new HashMap<>();
    private static int nextId = 1;
    private static List<Op> recording;

    static int genLists(int range) {
        int id = nextId;
        nextId += Math.max(1, range);
        return id;
    }

    static void newList(int id) {
        recording = new ArrayList<>();
        lists.put(id, recording);
    }

    static void endList() {
        recording = null;
    }

    static boolean isRecording() {
        return recording != null;
    }

    static void callList(int id) {
        List<Op> ops = lists.get(id);
        if (ops == null) return;
        for (Op op : ops) {
            op.run();
        }
    }

    static void recordBegin(int mode) {
        recording.add(() -> LegacyGL.begin(mode));
    }

    static void recordEnd() {
        recording.add(LegacyGL::end);
    }

    static void recordVertex(double x, double y, double z) {
        recording.add(() -> LegacyGL.vertex(x, y, z));
    }

    static void recordTexCoord(double u, double v) {
        recording.add(() -> LegacyGL.texCoord(u, v));
    }

    static void recordColor(float r, float g, float b, float a) {
        recording.add(() -> LegacyGL.color(r, g, b, a));
    }

}
