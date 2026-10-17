// SPDX-License-Identifier: GPL-3.0-only
package local.maps.patches;

import java.util.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;

/** Pinned 26.09 insertion rules. No upstream method bodies are distributed. */
final class ClusterHooks {
 private static final String C="Llocal/maps/cluster/",NATIVE="Lcom/google/android/apps/gmm/car/embedded/auxiliarymap/EmbeddedClusterActivity;";
 private static final InstructionExtensions ASM=InstructionExtensions.INSTANCE;
 static void branchBlock(MutableMethod method,int index,String code){ASM.addInstructionsWithLabels(method,index,code);}
 private static MutableMethod method(BytecodePatchContext c,String type,String name,String descriptor){
  MutableMethod found=null;
  for(var m:c.mutableClassDefBy(type).getMethods()){
   StringBuilder d=new StringBuilder("(");for(var p:m.getParameterTypes())d.append(p);d.append(')').append(m.getReturnType());
   if(m.getName().equals(name)&&d.toString().equals(descriptor)){if(found!=null)throw new IllegalStateException("Ambiguous "+type+name);found=m;}
  }
  if(found==null||found.getImplementation()==null)throw new IllegalStateException("Missing pinned hook "+type+name+descriptor);return found;
 }
 private static void registers(MutableMethod m,int count){if(m.getImplementation().getRegisterCount()!=count)throw new IllegalStateException("Register schema changed: "+m);}
 private static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
 private static int returns(MutableMethod m,String code){
  int n=0;var body=m.getImplementation();for(int i=body.getInstructions().size()-1;i>=0;i--)if(body.getInstructions().get(i).getOpcode()==Opcode.RETURN_VOID){ASM.addInstructions(m,i,code);n++;}return n;
 }
 private static void oneReturn(MutableMethod m,String code){if(returns(m,code)!=1)throw new IllegalStateException("Expected one normal exit: "+m);}
 /** Preserve old encoded register coordinates while parameters move up one slot.
  * Copy low to high; the highest new register retains the last object parameter.
  */
 private static MutableMethod expand(BytecodePatchContext ctx,String type,MutableMethod m){
  var old=m.getImplementation();int units=(m.getAccessFlags()&AccessFlags.STATIC.getValue())==0?1:0;
  List<String> types=new ArrayList<>();if(units==1)types.add(type);
  for(var p:m.getParameterTypes()){String s=p.toString();types.add(s);units+=s.equals("J")||s.equals("D")?2:1;}
  var body=new MutableMethodImplementation(new ImmutableMethodImplementation(old.getRegisterCount()+1,old.getInstructions(),old.getTryBlocks(),old.getDebugItems()));
  int target=old.getRegisterCount()-units,index=0;
  for(String t:types){Opcode opcode=t.equals("J")||t.equals("D")?Opcode.MOVE_WIDE_FROM16:t.startsWith("L")||t.startsWith("[")?Opcode.MOVE_OBJECT_FROM16:Opcode.MOVE_FROM16;body.addInstruction(index++,new BuilderInstruction22x(opcode,target,target+1));target+=t.equals("J")||t.equals("D")?2:1;}
  var changed=new MutableMethod(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body));
  var c=ctx.mutableClassDefBy(type);var all=c.getMethods();var direct=c.getDirectMethods();var virtual=c.getVirtualMethods();
  boolean wasDirect=direct.remove(m);virtual.remove(m);all.remove(m);all.add(changed);if(wasDirect)direct.add(changed);else virtual.add(changed);
  return changed;
 }
 private static void adjustReturns(MutableMethod m,int owner,String hook){
  int n=0;var body=m.getImplementation();
  for(int i=body.getInstructions().size()-1;i>=0;i--){var ins=body.getInstructions().get(i);if(ins.getOpcode()!=Opcode.RETURN_OBJECT)continue;int point=((OneRegisterInstruction)ins).getRegisterA();ASM.addInstructions(m,i,"invoke-static {v"+owner+", v"+point+"}, "+C+"PointerHooks;->"+hook+"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;\nmove-result-object v"+point+"\ncheck-cast v"+point+", Luun;");n++;}
  if(n!=1)throw new IllegalStateException("Expected one anchor return: "+m);
 }
 static void apply(BytecodePatchContext ctx){
  // These are not authorization hooks: native sii/account/token implementations are retained.
  var m=method(ctx,"Lfmv;","attachBaseContext","(Landroid/content/Context;)V");
  ASM.addInstruction(m,0,"invoke-static/range {p1 .. p1}, "+C+"PatchBootstrap;->attach(Landroid/content/Context;)V");
  m=method(ctx,"Liia;","<init>","(Landroid/content/Context;Lgee;Lalno;Lalno;Ljava/util/concurrent/ScheduledExecutorService;Lalno;Llfq;Laolt;)V");
  oneReturn(m,"invoke-static/range {p0 .. p0}, "+C+"ClusterHost;->registerViewportCandidate(Ljava/lang/Object;)V");
  m=method(ctx,"Lwih;","<init>","(Ljava/util/concurrent/Executor;)V");oneReturn(m,"invoke-static {p0}, "+C+"BydGuidance;->registerSource(Ljava/lang/Object;)V");
  m=method(ctx,"Lijy;","<init>","(Ladqy;Lamic;Lqhj;Lfxh;Ltfh;)V");oneReturn(m,"invoke-static/range {p0 .. p0}, "+C+"ReportButtonRecovery;->register(Ljava/lang/Object;)V");
  m=method(ctx,"Lmx;","onUserLeaveHint","()V");ASM.addInstruction(m,0,"invoke-static {p0}, "+C+"ClusterSettings;->leavingMaps(Landroid/app/Activity;)V");
  m=method(ctx,NATIVE,"ld","()Landroid/content/Context;");int count=0;
  for(int i=m.getImplementation().getInstructions().size()-1;i>=0;i--){var ins=m.getImplementation().getInstructions().get(i);if(ins.getOpcode()!=Opcode.RETURN_OBJECT)continue;int r=((OneRegisterInstruction)ins).getRegisterA();ASM.addInstructions(m,i,"invoke-static {v"+r+"}, "+C+"ClusterHost;->clusterContext(Landroid/content/Context;)Landroid/content/Context;\nmove-result-object v"+r);count++;}
  if(count!=2)throw new IllegalStateException("Native cluster context returns changed");
  for(String name:List.of("j","k")){
   m=method(ctx,"Liia;",name,"(Lihw;Liht;)V");registers(m,5);
   branchBlock(m,0,"invoke-static {p0, p1}, "+C+"ClusterHost;->ignoreRouteCardViewport(Ljava/lang/Object;Ljava/lang/Object;)Z\nmove-result v0\nif-eqz v0, :stock\nreturn-void\n:stock\nnop");
  }
  m=method(ctx,"Liia;","f","()Luun;");registers(m,1);m=expand(ctx,"Liia;",m);adjustReturns(m,1,"adjust");
  m=method(ctx,"Ldba;","k","(FLihy;)Luun;");registers(m,8);m=expand(ctx,"Ldba;",m);adjustReturns(m,8,"adjustCamera");
  m=method(ctx,"Liia;","q","()V");registers(m,21);count=0;
  for(int i=m.getImplementation().getInstructions().size()-1;i>=0;i--){var ins=m.getImplementation().getInstructions().get(i);if(ins.getOpcode()==Opcode.MOVE_OBJECT_FROM16&&((TwoRegisterInstruction)ins).getRegisterA()==16&&((TwoRegisterInstruction)ins).getRegisterB()==3){ASM.addInstructions(m,i,"invoke-static {v0, v3}, "+C+"PointerHooks;->adjust(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;\nmove-result-object v3\ncheck-cast v3, Luun;");count++;}}
  if(count!=1)throw new IllegalStateException("Viewport anchor dispatch changed");
  m=method(ctx,"Ljfl;","run","()V");registers(m,10);int holders=0,anchors=0;
  for(int i=m.getImplementation().getInstructions().size()-1;i>=0;i--){var ins=m.getImplementation().getInstructions().get(i);String reference=ref(ins);
   if(reference.equals("Laehr;->d:Ljava/lang/Object;")){ASM.addInstruction(m,i,"iget-object v5, p0, Laehr;->c:Ljava/lang/Object;");holders++;}
   if(reference.equals("Lupq;->c:F")||reference.equals("Luun;->c:F")){ASM.addInstructions(m,i+1,"invoke-static {v5, v3}, "+C+"ClusterHost;->applyClusterPointerY(Ljava/lang/Object;F)F\nmove-result v3");anchors++;}
  }
  if(holders!=2||anchors!=2)throw new IllegalStateException("Live anchor callbacks changed");
  m=method(ctx,"Lggh;","b","(Lafpg;)Lgge;");registers(m,15);count=0;
  for(int i=m.getImplementation().getInstructions().size()-1;i>=0;i--)if(ref(m.getImplementation().getInstructions().get(i)).equals("Lgge;-><init>(ZLafpa;ZZLlfp;ILafou;Lafox;)V")){
   branchBlock(m,i,"invoke-static {}, "+C+"ClusterHost;->nativeClusterActive()Z\nmove-result v0\nif-eqz v0, :stock\nsget-object v6, Lafpa;->b:Lafpa;\ninvoke-static {}, "+C+"ClusterHost;->nativeNight()Z\nmove-result v7\nmove v8, v7\n:stock\nnop");count++;
  }
  if(count!=1)throw new IllegalStateException("Cluster OEM options changed");
  m=method(ctx,"Lnym;","f","(Z)V");registers(m,15);count=0;
  for(int i=m.getImplementation().getInstructions().size()-1;i>=0;i--)if(ref(m.getImplementation().getInstructions().get(i)).equals("Lacel;->n(Ljava/lang/Iterable;)Labtb;")){
   // Native empty-override and loop exits branch directly to this instruction.
   // Keep those labels on our hook; inserting before it leaves those paths unhooked.
   m.getImplementation().replaceInstruction(i,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,13,6,0,0,0,new ImmutableMethodReference(C+"ClusterHost;","configureClusterLayers",List.of("Ljava/lang/Object;","Ljava/util/Set;"),"V")));
   ASM.addInstruction(m,i+1,"invoke-static {v6}, Lacel;->n(Ljava/lang/Iterable;)Labtb;");count++;
  }
  if(count!=1)throw new IllegalStateException("Cluster renderer layers changed");
  System.out.println("Cluster hooks: bootstrap attach, viewport/card/anchor, native context/theme/layers, guidance, PiP, incident recovery");
 }
 static void location(BytecodePatchContext ctx){
  String action="com.google.android.location.internal.GoogleLocationManagerService.START";int count=0;
  for(var c:ctx.getAllClassesWithString(action)){
   if(c.getType().startsWith("Llocal/"))continue;
   for(var source:c.getMethods()){
    if(!source.getReturnType().equals("Ljava/lang/String;")||!source.getParameterTypes().isEmpty()||source.getImplementation()==null)continue;
    var instructions=new ArrayList<Instruction>();for(var i:source.getImplementation().getInstructions())instructions.add(i);
    if(instructions.size()!=2||!(instructions.get(0) instanceof ReferenceInstruction)||!ref(instructions.get(0)).equals(action)||instructions.get(1).getOpcode()!=Opcode.RETURN_OBJECT)continue;
    var m=method(ctx,c.getType(),source.getName(),"()Ljava/lang/String;");int r=((OneRegisterInstruction)instructions.get(1)).getRegisterA();
    m.getImplementation().replaceInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,0,0,0,0,0,0,new ImmutableMethodReference(C+"MicrogProvider;","locationAction",List.of(),"Ljava/lang/String;")));
    m.getImplementation().addInstruction(1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,r));count++;
   }
  }
  if(count!=2)throw new IllegalStateException("Location action getters changed: "+count);
  System.out.println("Location action: two native getters use installed provider alias selection");
 }
}
