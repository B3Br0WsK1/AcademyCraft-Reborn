package cn.lambdalib2.render.obj;

import cn.lambdalib2.render.obj.ObjModel.Face;
import cn.lambdalib2.render.obj.ObjModel.Vertex;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.ResourceUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses obj model file into the runtime {@link ObjModel}.
 */
public class ObjParser {

    public static ObjModel parse(Identifier res) {
        return parse(new InputStreamReader(ResourceUtils.getResourceStream(res), StandardCharsets.UTF_8));
    }

    private static ObjModel parse(Reader rdr0) {
        List<Vector3f> vs = new ArrayList<>();
        List<Vector2f> vts = new ArrayList<>();
        Multimap<String, ObjFace> faces = LinkedHashMultimap.create();

        try (BufferedReader rdr = new BufferedReader(rdr0)) {
            String currentGroup = "Default";

            String ln;
            while ((ln = rdr.readLine()) != null) {
                ln = ln.trim();
                if (ln.isEmpty() || ln.charAt(0) == '#') {
                    continue;
                }

                Scanner scanner = new Scanner(ln).useLocale(Locale.ROOT);

                String token = scanner.next();
                switch (token) {
                    case "v":
                        vs.add(new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
                        break;

                    case "vt":
                        vts.add(new Vector2f(scanner.nextFloat(), scanner.nextFloat()));
                        break;

                    case "g":
                        currentGroup = scanner.next();
                        break;

                    case "f":
                        int[] v1 = parseFaceVertex(scanner.next());
                        int[] v2 = parseFaceVertex(scanner.next());
                        int[] v3 = parseFaceVertex(scanner.next());
                        if (scanner.hasNext()) { // quad
                            int[] v4 = parseFaceVertex(scanner.next());
                            faces.put(currentGroup, new ObjFace(v1[0], v1[1], v2[0], v2[1], v3[0], v3[1]));
                            faces.put(currentGroup, new ObjFace(v1[0], v1[1], v3[0], v3[1], v4[0], v4[1]));
                        } else {
                            faces.put(currentGroup, new ObjFace(v1[0], v1[1], v2[0], v2[1], v3[0], v3[1]));
                        }
                        break;

                    case "usemtl":
                    case "mtllib":
                    case "vn":
                    case "s":
                    case "o":
                        break;

                    default:
                        Debug.debug("Unknown token " + token);
                        break;
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        // Convert into runtime format which is more GL-friendly
        List<Vertex> vertices = new ArrayList<>();
        Map<VertexIdt, Integer> generated = new HashMap<>();
        Multimap<Integer, Face> vertFaceSharing = ArrayListMultimap.create();

        GenContext ctx = new GenContext(vs, vts, generated, vertices);

        ObjModel ret = new ObjModel();

        for (String group : faces.keySet()) {
            for (ObjFace face : faces.get(group)) {
                int i0 = genIndex(ctx, new VertexIdt(face.v0, face.vt0));
                int i1 = genIndex(ctx, new VertexIdt(face.v1, face.vt1));
                int i2 = genIndex(ctx, new VertexIdt(face.v2, face.vt2));

                Face f = new Face(i0, i1, i2);

                // Calculate plane tangent
                Vertex v1 = vertices.get(i0), v2 = vertices.get(i1), v3 = vertices.get(i2);
                Vector3f edge1 = new Vector3f(v2.pos).sub(v1.pos);
                Vector3f edge2 = new Vector3f(v3.pos).sub(v1.pos);
                Vector2f duv1 = new Vector2f(v2.uv).sub(v1.uv);
                Vector2f duv2 = new Vector2f(v3.uv).sub(v1.uv);

                Vector3f t = f.tangent, n = f.normal;
                float ff = 1.0f / (duv1.x * duv2.y - duv2.x * duv1.y);
                t.x = ff * (duv2.y * edge1.x - duv1.y * edge2.x);
                t.y = ff * (duv2.y * edge1.y - duv1.y * edge2.y);
                t.z = ff * (duv2.y * edge1.z - duv1.y * edge2.z);
                if (t.lengthSquared() > 0 && Float.isFinite(t.x)) t.normalize();

                edge1.cross(edge2, n);
                if (n.lengthSquared() > 0) n.normalize();

                ret.faces.put(group, f);
                vertFaceSharing.put(i0, f);
                vertFaceSharing.put(i1, f);
                vertFaceSharing.put(i2, f);
            }
        }

        for (int i = 0; i < vertices.size(); ++i) {
            Collection<Face> usedFaces = vertFaceSharing.get(i);
            if (!usedFaces.isEmpty()) {
                Vector3f accum = new Vector3f(), accum1 = new Vector3f();
                for (Face f : usedFaces) {
                    accum.add(f.tangent);
                    accum1.add(f.normal);
                }

                if (accum.lengthSquared() > 0 && Float.isFinite(accum.x))
                    accum.normalize();
                if (accum1.lengthSquared() > 0)
                    accum1.normalize();
                vertices.get(i).tangent.set(accum);
                vertices.get(i).normal.set(accum1);
            }
        }

        ret.vertices.addAll(vertices);

        return ret;
    }

    private static int[] parseFaceVertex(String v) {
        String[] arr = v.split("/");
        Debug.require(arr.length >= 2);
        int[] ret = new int[arr.length];
        for (int i = 0; i < arr.length; ++i) {
            ret[i] = arr[i].isEmpty() ? 0 : Integer.parseInt(arr[i]) - 1;
        }

        return ret;
    }

    private static int genIndex(GenContext ctx, VertexIdt idt) {
        Integer idx = ctx.generated.get(idt);
        if (idx == null) {
            idx = ctx.vertices.size();
            ctx.generated.put(idt, idx);
            ctx.vertices.add(new Vertex(new Vector3f(ctx.vs.get(idt.vert)), new Vector2f(ctx.vts.get(idt.tex))));
        }
        return idx;
    }

    private record GenContext(List<Vector3f> vs, List<Vector2f> vts,
                              Map<VertexIdt, Integer> generated, List<Vertex> vertices) {
    }

    private record ObjFace(int v0, int vt0, int v1, int vt1, int v2, int vt2) {
    }

    private record VertexIdt(int vert, int tex) {
    }

}
