// SPDX-License-Identifier: GPL-3.0-only
package local.maps.patches;

import java.util.*;
import kotlin.Unit;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Enables the single native 3D-settings experiment consistently at its eight pinned reads.
 * Native user preferences, eligibility, thermal protection and rendering algorithms stay intact.
 * Applied before the other hooks so schema checks refer to the exact original26.09 input.
 */
final class Buildings3dHooks {
    private static final String FLAG = "Lalkj;->m:Z";
    private static final String CLUSTER = "Lcom/google/android/apps/gmm/car/embedded/auxiliarymap/EmbeddedClusterActivity;";
    private static final String CAMERA_PARAMETERS = "[Lwuw;, Lwnp;, Lqxk;, Lrql;, Ltph;, Lwwh;, Lxcr;, Lqiy;, Lryg;, Ladqy;, Lxdl;, Luhe;, Lujr;, Lfqq;, Lolc;, Lmye;, Lnyq;, Lpph;, Lfqe;, Lei;, Ljava/util/concurrent/Executor;, Lqfl;, Lgee;, Ljio;, Lxef;, Ljdt;, Lqld;, Lmhy;, Lei;, Lgwz;, Lalno;, Lhbn;, Lmwt;, Lmwq;, Ljfe;, Lhrp;, Lxdi;, Lei;, Lnyn;, Lxhk;, Lnip;, Llwi;, Lqqb;, Ladqy;, Lalno;, Liyy;, Ljdr;, Lqfj;, Lwxy;, Lrqz;, Lvwh;, Lpim;, Lei;, Lxen;, Lakea;, Lxeb;, Lxed;, Ljfa;, Ljge;]";
    private record Site(String type, String key, int registers, int count,
                        int index, int address, int output, int owner) {}
    private static final List<Site> SITES = List.of(
        new Site(CLUSTER, "onStart[]V", 42, 425, 68, 147, 15, 6),
        new Site("Lhrq;", "h[]Z", 1, 10, 4, 8, 0, 0),
        new Site("Ligc;", "f[Z]V", 15, 558, 326, 655, 2, 2),
        new Site("Lmyz;", "Q[Lajly;]Z", 3, 38, 3, 6, 0, 0),
        new Site("Lnui;", "f[Lrlr;, Ladfk;, Lqqb;]Lnyn;", 13, 76, 22, 46, 4, 4),
        new Site("Lwxh;", "c[]Lugf;", 5, 25, 6, 12, 1, 1),
        new Site("Litk;", "<init>" + CAMERA_PARAMETERS + "V", 93, 346, 93, 192, 9, 9),
        new Site("Litk;", "<init>" + CAMERA_PARAMETERS + "V", 93, 346, 119, 239, 2, 2)
    );
    private static String key(Method m) {
        return m.getName() + m.getParameterTypes() + m.getReturnType();
    }
    private static String reference(Instruction i) {
        return i instanceof ReferenceInstruction ? ((ReferenceInstruction) i).getReference().toString() : "";
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Native 3D Buildings schema: " + message);
    }
    static void apply(BytecodePatchContext context) {
        // Audit every read before mutating anything: fail closed on an extra consumer,
        // changed method, opcode, register, address or instruction count.
        Set<Site> seen = new HashSet<>();
        context.classDefForEach(c -> {
            for (var m : c.getMethods()) {
                var body = m.getImplementation();
                if (body == null) continue;
                List<Instruction> code = new ArrayList<>();
                body.getInstructions().forEach(code::add);
                int address = 0;
                for (int index = 0; index < code.size(); index++) {
                    var instruction = code.get(index);
                    if (reference(instruction).equals(FLAG)) {
                        require(instruction.getOpcode() == Opcode.IGET_BOOLEAN
                            && instruction.getCodeUnits() == 2, "unexpected flag access");
                        var read = (TwoRegisterInstruction) instruction;
                        var site = new Site(c.getType(), key(m), body.getRegisterCount(), code.size(),
                            index, address, read.getRegisterA(), read.getRegisterB());
                        require(SITES.contains(site) && seen.add(site), "unexpected/duplicate flag read: " + site);
                    }
                    address += instruction.getCodeUnits();
                }
            }
            return Unit.INSTANCE;
        });
        require(seen.size() == SITES.size(), "expected eight experiment reads, found " + seen.size());
        for (var site : SITES) {
            MutableMethod method = null;
            for (var m : context.mutableClassDefBy(site.type()).getMethods()) {
                if (!key(m).equals(site.key())) continue;
                require(method == null, "ambiguous method");
                method = m;
            }
            require(method != null, "method disappeared");
            // Same two code units as the native IGET_BOOLEAN. Registers, branch targets,
            // switches and try/catch boundaries remain in their original positions.
            method.getImplementation().replaceInstruction(site.index(),
                new BuilderInstruction21s(Opcode.CONST_16, site.output(), 1));
        }
        System.out.println("Native 3D Buildings: enabled one settings experiment at eight audited reads in seven methods; native requested/active state, thermal and satellite rules preserved");
    }
    private Buildings3dHooks() {}
}
