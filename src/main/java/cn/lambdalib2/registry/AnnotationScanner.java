package cn.lambdalib2.registry;

import cn.lambdalib2.util.Debug;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Replacement of Forge's ASMDataTable. Scans the class files of given mods (without loading them) and collects
 *  every annotated class, method and field whose annotation lives in one of the scanned packages.
 */
public final class AnnotationScanner {

    public enum Kind { CLASS, METHOD, FIELD }

    public static final class Entry {
        public final Kind kind;
        public final String className; // binary name, e.g. a.b.C$D
        public final String memberName; // null for class
        public final String desc; // method/field descriptor, null for class
        public final Map<String, Object> values;
        final boolean clientOnly;

        Entry(Kind kind, String className, String memberName, String desc, Map<String, Object> values, boolean clientOnly) {
            this.kind = kind;
            this.className = className;
            this.memberName = memberName;
            this.desc = desc;
            this.values = values;
            this.clientOnly = clientOnly;
        }

        @Override
        public String toString() {
            return kind + ":" + className + (memberName == null ? "" : "#" + memberName + desc);
        }
    }

    private static final String ENV_DESC = "Lnet/fabricmc/api/Environment;";

    private static final Map<String, List<Entry>> table = new HashMap<>();
    private static final Set<String> clientOnlyClasses = new HashSet<>();
    private static boolean scanned = false;

    private AnnotationScanner() {}

    public static synchronized void scan(String... modIds) {
        if (scanned) return;
        scanned = true;
        long begin = System.currentTimeMillis();
        int count = 0;

        List<Entry> pending = new ArrayList<>();
        for (String modId : modIds) {
            Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
            if (container.isEmpty()) continue;
            for (Path root : container.get().getRootPaths()) {
                try (Stream<Path> stream = Files.walk(root)) {
                    Iterator<Path> itr = stream.iterator();
                    while (itr.hasNext()) {
                        Path p = itr.next();
                        if (p.toString().endsWith(".class")) {
                            try (InputStream is = Files.newInputStream(p)) {
                                scanClass(is.readAllBytes(), pending);
                                ++count;
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to scan mod " + modId, e);
                }
            }
        }

        for (Entry e : pending) {
            table.computeIfAbsent(e.values.get("$anno").toString(), k -> new ArrayList<>()).add(e);
        }
        Debug.log("AnnotationScanner: scanned " + count + " classes in " + (System.currentTimeMillis() - begin) + "ms");
    }

    private static void scanClass(byte[] bytes, List<Entry> out) {
        ClassReader reader = new ClassReader(bytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            String className;
            boolean classClient;
            final List<Entry> local = new ArrayList<>();

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                className = name.replace('/', '.');
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.equals(ENV_DESC)) {
                    return envVisitor(() -> classClient = true);
                }
                if (!interesting(descriptor)) return null;
                Map<String, Object> values = new HashMap<>();
                values.put("$anno", annoName(descriptor));
                local.add(new Entry(Kind.CLASS, className, null, null, values, false));
                return valueCollector(values);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    boolean client;
                    final List<Map<String, Object>> annos = new ArrayList<>();

                    @Override
                    public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                        if (adesc.equals(ENV_DESC)) {
                            return envVisitor(() -> client = true);
                        }
                        if (!interesting(adesc)) return null;
                        Map<String, Object> values = new HashMap<>();
                        values.put("$anno", annoName(adesc));
                        annos.add(values);
                        return valueCollector(values);
                    }

                    @Override
                    public void visitEnd() {
                        for (Map<String, Object> v : annos) {
                            local.add(new Entry(Kind.METHOD, className, name, descriptor, v, client));
                        }
                    }
                };
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM9) {
                    boolean client;
                    final List<Map<String, Object>> annos = new ArrayList<>();

                    @Override
                    public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                        if (adesc.equals(ENV_DESC)) {
                            return envVisitor(() -> client = true);
                        }
                        if (!interesting(adesc)) return null;
                        Map<String, Object> values = new HashMap<>();
                        values.put("$anno", annoName(adesc));
                        annos.add(values);
                        return valueCollector(values);
                    }

                    @Override
                    public void visitEnd() {
                        for (Map<String, Object> v : annos) {
                            local.add(new Entry(Kind.FIELD, className, name, descriptor, v, client));
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (classClient) {
                    clientOnlyClasses.add(className);
                }
                out.addAll(local);
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static boolean interesting(String desc) {
        return desc.startsWith("Lcn/lambdalib2/") || desc.startsWith("Lcn/academy/");
    }

    private static String annoName(String desc) {
        return Type.getType(desc).getClassName();
    }

    private static AnnotationVisitor envVisitor(Runnable onClient) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visitEnum(String name, String descriptor, String value) {
                if ("CLIENT".equals(value)) onClient.run();
            }
        };
    }

    private static AnnotationVisitor valueCollector(Map<String, Object> values) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                values.put(name, value);
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                values.put(name, value);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                List<Object> list = new ArrayList<>();
                values.put(name, list);
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String n, Object value) {
                        list.add(value);
                    }

                    @Override
                    public void visitEnum(String n, String descriptor, String value) {
                        list.add(value);
                    }
                };
            }
        };
    }

    private static boolean isClientOnly(Entry e) {
        if (e.clientOnly) return true;
        String name = e.className;
        while (true) {
            if (clientOnlyClasses.contains(name)) return true;
            int idx = name.lastIndexOf('$');
            if (idx == -1) return false;
            name = name.substring(0, idx);
        }
    }

    /**
     * @return All entries annotated with given annotation, with entries unavailable at current physical side removed.
     */
    public static List<Entry> getAll(String annoName) {
        boolean isClient = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
        List<Entry> list = table.getOrDefault(annoName, Collections.emptyList());
        List<Entry> ret = new ArrayList<>();
        for (Entry e : list) {
            if (isClient || !isClientOnly(e)) {
                ret.add(e);
            }
        }
        return ret;
    }

    /**
     * @return Whether this entry is marked client-only (either itself or its declaring class).
     */
    public static boolean isClientOnlyEntry(Entry e) {
        return isClientOnly(e);
    }
}
