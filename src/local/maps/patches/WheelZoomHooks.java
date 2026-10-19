// SPDX-License-Identifier: GPL-3.0-only
package local.maps.patches;

import java.util.List;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Exact static and navigation camera entries; no full-pose dispatcher interception. */
final class WheelZoomHooks {
    static final String TYPE="Lxcl;",DESCRIPTOR="d(Lagxc;)Lj$/util/Optional;";
    static final String PIN="a288fab9cb66752f125520793c3c87cf6a7776f4379f63047d0ceab8d6196b02";
    static final String NAV="e(Lagyb;Lwww;Z)Lj$/util/Optional;",NAV_PIN="a9a588a857b23242b01a88c35314df69f93f704f2eb38276bb98165850ff6f4f";
    static final String OVERRIDE="n(Ljava/lang/Float;Ljava/lang/Float;I)Lj$/util/Optional;",OVERRIDE_PIN="91e963e12a06fb932425c74073f89fcb8140ae02b9940cd3e5034ed04916f8f0";
    private static final ImmutableMethodReference CHANGED=new ImmutableMethodReference("Llocal/maps/cluster/ClusterZoom;","cameraChanged",List.of("Ljava/lang/Object;"),"V");
    private static void pin(MutableMethod method,String descriptor,String hash,int registers){
        if(!TYPE.equals(method.getDefiningClass())||!descriptor.equals(Api29Hooks.descriptor(method))||!hash.equals(Api29Hooks.fingerprint(method))||method.getImplementation()==null||method.getImplementation().getRegisterCount()!=registers)throw new IllegalStateException("Wheel camera fingerprint changed "+descriptor);
    }
    private static void changedAfter(MutableMethod method,int address,int receiver,String field,String type){
        var body=method.getImplementation();int at=0,index=0,matches=0,found=-1;
        for(var instruction:body.getInstructions()){
            if(at==address&&instruction.getOpcode()==Opcode.IPUT_OBJECT&&instruction instanceof com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction&&instruction instanceof com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction){
                var registers=(com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction)instruction;
                var reference=((com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)instruction).getReference();
                if(registers.getRegisterB()==receiver&&reference instanceof com.android.tools.smali.dexlib2.iface.reference.FieldReference){var f=(com.android.tools.smali.dexlib2.iface.reference.FieldReference)reference;if(TYPE.equals(f.getDefiningClass())&&field.equals(f.getName())&&type.equals(f.getType())){found=index+1;matches++;}}
            }
            at+=instruction.getCodeUnits();index++;
        }
        if(matches!=1)throw new IllegalStateException("Camera cached state completion boundary changed");
        body.addInstruction(found,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,receiver,0,0,0,0,CHANGED));
    }
    static void patchNavigation(MutableMethod method){pin(method,NAV,NAV_PIN,19);changedAfter(method,0x48,15,"A","Lj$/util/Optional;");}
    static void patchOverride(MutableMethod method){
        pin(method,OVERRIDE,OVERRIDE_PIN,14);var body=method.getImplementation();
        body.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,10,11,0,0,0,new ImmutableMethodReference("Llocal/maps/cluster/ClusterZoom;","adjustNavigationZoom",List.of("Ljava/lang/Object;","Ljava/lang/Float;"),"Ljava/lang/Float;")));
        body.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,11));
    }
    private static MutableMethod method(BytecodePatchContext ctx,String descriptor){MutableMethod found=null;for(var method:ctx.mutableClassDefBy(TYPE).getMethods())if(descriptor.equals(Api29Hooks.descriptor(method))){if(found!=null)throw new IllegalStateException("Duplicate wheel camera method");found=method;}if(found==null)throw new IllegalStateException("Missing wheel camera method "+descriptor);return found;}
    private static void original(BytecodePatchContext ctx,String descriptor,String hash,int registers){pin(method(ctx,descriptor),descriptor,hash,registers);}
    static void patch(MutableMethod method){
        pin(method,DESCRIPTOR,PIN,14);changedAfter(method,0x57,12,"B","Lj$/util/Optional;");
        var body=method.getImplementation();
        // Original p0=v12/p1=v13. No locals, result, callbacks or try handlers are clobbered.
        body.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,12,13,0,0,0,
            new ImmutableMethodReference("Llocal/maps/cluster/ClusterZoom;","adjustState",List.of("Ljava/lang/Object;","Ljava/lang/Object;"),"Ljava/lang/Object;")));
        body.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,13));
        body.addInstruction(2,new BuilderInstruction21c(Opcode.CHECK_CAST,13,new ImmutableTypeReference("Lagxc;")));
    }
    private static void field(BytecodePatchContext ctx,String type,String name,String expected){
        int matches=0;var owner=ctx.classDefByOrNull(type);
        if(owner!=null)for(var field:owner.getFields())if(name.equals(field.getName())&&expected.equals(field.getType()))matches++;
        if(matches!=1)throw new IllegalStateException("Wheel zoom native camera field changed "+type+name);
    }
    static void apply(BytecodePatchContext ctx){
        field(ctx,"Lgvx;","Y","Lfuz;");field(ctx,"Lfvi;","aC","Ligf;");field(ctx,"Lgfx;","n","Ligf;");field(ctx,"Ligc;","au","Lujr;");field(ctx,"Ligc;","F","Lalno;");
        field(ctx,"Lujr;","c","Lalno;");field(ctx,"Lujr;","d","Lalno;");field(ctx,"Lujr;","e","Lalno;");field(ctx,"Lutj;","c","Ljava/lang/Object;");field(ctx,"Lukh;","d","Lalno;");field(ctx,"Lujs;","a","[Luuf;");
        field(ctx,"Lxci;","b","Lxcl;");field(ctx,"Lxci;","c","Lcom/google/android/libraries/geo/navcore/ui/camera/jni/CameraControllerJni;");
        field(ctx,TYPE,"a","Lcom/google/android/libraries/geo/navcore/ui/camera/jni/CameraControllerJni;");field(ctx,TYPE,"x","Lxci;");field(ctx,TYPE,"E","Lujr;");field(ctx,TYPE,"C","Z");field(ctx,TYPE,"A","Lj$/util/Optional;");field(ctx,TYPE,"B","Lj$/util/Optional;");field(ctx,TYPE,"d","Lagxd;");field(ctx,TYPE,"n","Lfqe;");
        field(ctx,"Lagxc;","b","I");field(ctx,"Lagxc;","c","Ljava/lang/Object;");field(ctx,"Lagyd;","b","I");field(ctx,"Lagyd;","e","D");field(ctx,"Lakcr;","b","Lakcx;");
        field(ctx,"Lagxd;","b","I");field(ctx,"Lagxd;","c","D");field(ctx,"Lagxd;","d","D");field(ctx,"Lagyb;","b","I");field(ctx,"Lagyb;","c","Ljava/lang/Object;");
        original(ctx,"i(Ljava/lang/Float;)Lj$/util/Optional;","5cc12411d203faca6501fb4833988fc6c4d0765420fe8a69de9b5787f6e6bbb9",4);
        original(ctx,"o()Lj$/util/Optional;","70f6518707732880a3e677ed466994eee84a71ef7e75c3ad969ab0eceec2fcef",14);
        patch(method(ctx,DESCRIPTOR));patchNavigation(method(ctx,NAV));patchOverride(method(ctx,OVERRIDE));
        System.out.println("Wheel zoom: pinned static8 and navigation3/6 camera families; owned zoom scalar only");
    }
}
