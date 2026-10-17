// SPDX-License-Identifier: GPL-3.0-only
package local.maps.patches;

import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;

/** Eight pinned native EV insertion rules. No Google method bodies are bundled. */
final class VehicleEvHooks {
    private static final String REPO="Lhlh;", BRIDGE="Llocal/maps/cluster/VehicleEvBridge;";
    private static final String CTOR="<init>(Lrco;Lhlz;Ldpp;Ldep;Laolt;Lrlr;Lqqk;Lsnf;Lfxe;)V";
    private static final String PUBLISH="f(Lafpv;Laoeu;)Ljava/lang/Object;";
    private static final String FACTORY="m(Landroid/content/pm/PackageManager;Lrlr;Laobv;)Lfxe;";
    private static final Set<String> READS=Set.of("a()Laeru;","b()Lafpv;","c()Lj$/time/Instant;","e()Z");
    private static final String NATIVE_PUBLISH="Lrco;->d(Lqvo;)V", ORIGINAL_INPUT="Lhlh;->k:Ljava/util/concurrent/atomic/AtomicReference;";
    private static final String ATOMIC_SET="Ljava/util/concurrent/atomic/AtomicReference;->set(Ljava/lang/Object;)V", UNLOCK="Laowt;->d()V";
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static String descriptor(MutableMethod m){StringBuilder d=new StringBuilder(m.getName()).append('(');for(var t:m.getParameterTypes())d.append(t);return d.append(')').append(m.getReturnType()).toString();}
    private static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    private static MutableMethod method(BytecodePatchContext ctx,String type,String descriptor){
        MutableMethod found=null;
        for(var m:ctx.mutableClassDefBy(type).getMethods())if(descriptor(m).equals(descriptor)){check(found==null,"Ambiguous EV hook "+type+descriptor);found=m;}
        check(found!=null&&found.getImplementation()!=null,"Missing pinned EV hook "+type+descriptor);return found;
    }
    private static void invoke(Instruction i,Opcode opcode,int... registers){
        check(i.getOpcode()==opcode&&i instanceof FiveRegisterInstruction,"EV invocation opcode changed");
        var r=(FiveRegisterInstruction)i;int[] actual={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};
        check(r.getRegisterCount()==registers.length,"EV invocation register count changed");
        for(int n=0;n<registers.length;n++)check(actual[n]==registers[n],"EV invocation registers changed");
    }
    private static int onlyRef(List<Instruction> code,String reference){int at=-1;for(int i=0;i<code.size();i++)if(ref(code.get(i)).equals(reference)){check(at<0,"Repeated pinned EV reference "+reference);at=i;}check(at>=0,"Missing pinned EV reference "+reference);return at;}
    /** Package-visible for tests using the exact original methods, without a mock patch context. */
    static void patch(MutableMethod m){
        String d=descriptor(m),type=m.getDefiningClass();boolean repo=type.equals(REPO),factory=type.equals("Lfkn;")&&d.equals(FACTORY);
        check(factory||repo&&(d.equals(CTOR)||d.equals(PUBLISH)||READS.contains(d))||type.equals("Lrco;")&&d.equals("c(Z)V"),"Unexpected EV method");
        List<Instruction> code=new ArrayList<>(m.getImplementation().getInstructions());
        for(var instruction:code)check(!ref(instruction).startsWith(BRIDGE),"Native EV method already hooked");
        var out=m.getImplementation();
        if(factory){
            check(out.getRegisterCount()==6&&(m.getAccessFlags()&AccessFlags.STATIC.getValue())!=0,"Vehicle info factory register/static schema changed");int at=code.size()-2;
            check(ref(code.get(at)).equals("Ljava/lang/Object;->getClass()Ljava/lang/Class;"),"Vehicle info final non-null check changed");invoke(code.get(at),Opcode.INVOKE_VIRTUAL,3);
            check(code.get(at+1).getOpcode()==Opcode.RETURN_OBJECT&&((OneRegisterInstruction)code.get(at+1)).getRegisterA()==3,"Vehicle info final return changed");
            int returns=0;for(var i:code)if(i.getOpcode()==Opcode.RETURN_OBJECT)returns++;check(returns==1,"Vehicle info return count changed");
            // Replace the labeled native check, so native branches execute the wrapper too.
            out.replaceInstruction(at,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,3,0,0,0,0,new ImmutableMethodReference(BRIDGE,"wrapVehicleInfo",List.of("Ljava/lang/Object;"),"Ljava/lang/Object;")));
            out.addInstruction(at+1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,3));
            out.addInstruction(at+2,new BuilderInstruction21c(Opcode.CHECK_CAST,3,new ImmutableTypeReference("Lfxe;")));
            out.addInstruction(at+3,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,1,3,0,0,0,0,new ImmutableMethodReference("Ljava/lang/Object;","getClass",List.of(),"Ljava/lang/Class;")));
        }else if(!repo){
            check(out.getRegisterCount()==2,"Carrier availability register schema changed");
            check(ref(code.get(0)).equals("Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;"),"Carrier availability entry changed");invoke(code.get(0),Opcode.INVOKE_STATIC,1);
            out.replaceInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,0,1,0,0,0,new ImmutableMethodReference(BRIDGE,"availability",List.of("Ljava/lang/Object;","Z"),"Z")));
            out.addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT,1));
            out.addInstruction(2,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,1,0,0,0,0,new ImmutableMethodReference("Ljava/lang/Boolean;","valueOf",List.of("Z"),"Ljava/lang/Boolean;")));
        }else if(READS.contains(d)){
            int expected=d.startsWith("c(")?2:1;check(out.getRegisterCount()==expected,"Repository read register schema changed");
            String field=d.startsWith("b(")?ORIGINAL_INPUT:"Lhlh;->i:Lrco;";
            check(code.get(0).getOpcode()==Opcode.IGET_OBJECT&&ref(code.get(0)).equals(field),"Repository read entry changed");
            int self=expected-1;var original=(TwoRegisterInstruction)code.get(0);check(original.getRegisterA()==self&&original.getRegisterB()==self,"Repository read self register changed");
            out.replaceInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,self,0,0,0,0,new ImmutableMethodReference(BRIDGE,"beforeNativeRead",List.of("Ljava/lang/Object;"),"V")));
            out.addInstruction(1,new BuilderInstruction22c(Opcode.IGET_OBJECT,self,self,((ReferenceInstruction)code.get(0)).getReference()));
        }else if(d.equals(CTOR)){
            check(out.getRegisterCount()==10,"EV constructor register schema changed");int exit=-1;
            for(int i=0;i<code.size();i++)if(code.get(i).getOpcode()==Opcode.RETURN_VOID){check(exit<0,"Multiple EV constructor exits");exit=i;}
            check(exit==code.size()-1,"EV constructor normal exit changed");
            out.replaceInstruction(exit,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,0,0,0,0,0,new ImmutableMethodReference(BRIDGE,"register",List.of("Ljava/lang/Object;"),"V")));
            out.addInstruction(exit+1,new BuilderInstruction10x(Opcode.RETURN_VOID));
        }else{
            check(out.getRegisterCount()==13,"EV publisher register schema changed");int at=onlyRef(code,NATIVE_PUBLISH);
            invoke(code.get(at),Opcode.INVOKE_VIRTUAL,1,12);
            check(ref(code.get(at+1)).equals(ORIGINAL_INPUT),"Original EV input field changed");
            var field=(TwoRegisterInstruction)code.get(at+1);check(field.getRegisterA()==10&&field.getRegisterB()==10,"Original EV input field registers changed");
            check(ref(code.get(at+2)).equals(ATOMIC_SET),"Original EV input write changed");invoke(code.get(at+2),Opcode.INVOKE_VIRTUAL,10,0);
            check(ref(code.get(at+3)).equals(UNLOCK),"Native mutex unlock missing after EV publish pair");invoke(code.get(at+3),Opcode.INVOKE_VIRTUAL,11);
            Label unlock=out.newLabelForIndex(at+3);
            out.replaceInstruction(at,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,10,0,12,0,0,new ImmutableMethodReference(BRIDGE,"handleOwnedPublish",List.of("Ljava/lang/Object;","Ljava/lang/Object;","Ljava/lang/Object;"),"Z")));
            out.addInstruction(at+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
            out.addInstruction(at+2,new BuilderInstruction21t(Opcode.IF_NEZ,2,unlock));
            // Unowned data retains the exact native publish + original-input write pair.
            out.addInstruction(at+3,new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL,2,1,12,0,0,0,new ImmutableMethodReference("Lrco;","d",List.of("Lqvo;"),"V")));
        }
    }
    static void apply(BytecodePatchContext ctx){
        int count=0;
        for(String descriptor:READS){patch(method(ctx,REPO,descriptor));count++;}
        patch(method(ctx,REPO,CTOR));count++;
        patch(method(ctx,REPO,PUBLISH));count++;
        patch(method(ctx,"Lrco;","c(Z)V"));count++;
        patch(method(ctx,"Lfkn;",FACTORY));count++;
        check(count==8,"Expected eight native EV hooks");
        System.out.println("Native EV hooks: 8; repository registration/freshness, owned publication, availability and scoped vehicle profile");
    }
}
