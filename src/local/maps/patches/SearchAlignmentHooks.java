// SPDX-License-Identifier: GPL-3.0-only
package local.maps.patches;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import kotlin.Unit;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Removes only the stock collapsed-search top margin through Maps' reactive layout DSL.
 * Expanded-card spacing, START/END margins and all prior value-function cases stay native.
 * Pinned to the original AAOS26.09 implementation; no Google implementation is bundled.
 */
final class SearchAlignmentHooks {
    private static final String FACTORY="Lion;", PROVIDER="Lilf;";
    private static final String FACTORY_KEY="d[[Ltwx;]Ltwr;";
    private static final String VALUE_KEY="a[Ltuy;]Ljava/lang/Object;";
    private static final String CONSTRUCTOR="Lilf;-><init>(I)V";
    private static final int SEARCH_CASE=21;
    private static final Map<String,String> ORIGINAL=Map.ofEntries(
        Map.entry("Lion;->"+FACTORY_KEY,"e6f59bade6871842d3ac7c43f18fac2982776eaafa3abf6fa1d7e37d12f10aa2"),
        Map.entry("Lilf;->"+VALUE_KEY,"a07e1b3b8f0364a41474eee191bbf27098a2c2d1cff29f490206a74fc5552379"),
        Map.entry("Lilf;-><init>[I]V","b08d09b1d7f6667853638a2d8f10a9f5902a75c0176850317230d3209ef30e3f"),
        Map.entry("Lilg;->a[]Ltwr;","53c6e85719bc2fbc1c8f527a9ab0d37dc17b228e87e0f4f2557aa2acf98dc4d1"),
        Map.entry("Lilh;->a[]Ltwr;","db512d7095c02fdbbc856493eae4b6c9bb8260f1c2cbb9808f8c1b9ab654dc31"),
        Map.entry("Lion;-><clinit>[]V","793a37bf3916855179d61ef30cfd7dbb7ef7ac1fc7c24e7ff6a535350d5bfa03"),
        Map.entry("Lmku;-><clinit>[]V","f56d8f0aed4cb8b258a53d34c1a8721e37814e0521f04b48e5d973feddb20d2a"),
        Map.entry("Lisu;->f[]Z","5353f178833ef4648b9c1a964c3b62f174880350e21a73e55e76a5d7fd9683f1"),
        Map.entry("List;->f[]Z","973e1421fc642fa041a8152b198d8a19dacdb856865aed3a683cb7b3d2d74372"),
        Map.entry("Lisk;->f[]Z","fb9952a219449fd86fa66f944c5576f1359288618168f3aad393146eaf7b0351")
    );
    // These are every original direct call, not merely the factory sites we change.
    // Their three caller-method fingerprints above pin all integer arguments to0..20.
    private static final Set<String> CALLS=Set.of(
        "Lilg;->a[]Ltwr;@0250","Lilg;->a[]Ltwr;@027d","Lilg;->a[]Ltwr;@03e9","Lilg;->a[]Ltwr;@0418",
        "Lilh;->a[]Ltwr;@0030","Lilh;->a[]Ltwr;@009f","Lilh;->a[]Ltwr;@00b3","Lilh;->a[]Ltwr;@00c2",
        "Lion;->"+FACTORY_KEY+"@0056","Lion;->"+FACTORY_KEY+"@006a","Lion;->"+FACTORY_KEY+"@007d",
        "Lion;->"+FACTORY_KEY+"@00f8","Lion;->"+FACTORY_KEY+"@010e","Lion;->"+FACTORY_KEY+"@0262",
        "Lion;->"+FACTORY_KEY+"@02d5","Lion;->"+FACTORY_KEY+"@05c5","Lion;->"+FACTORY_KEY+"@065d",
        "Lion;->"+FACTORY_KEY+"@06a8","Lion;->"+FACTORY_KEY+"@06ba","Lion;->"+FACTORY_KEY+"@0932",
        "Lion;->"+FACTORY_KEY+"@0957","Lion;->"+FACTORY_KEY+"@097c"
    );
    private static String key(Method m){return m.getName()+m.getParameterTypes()+m.getReturnType();}
    private static String fullKey(Method m){return m.getDefiningClass()+"->"+key(m);}
    private static String reference(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Native search alignment schema: "+message);}
 static String fingerprint(Method m){
  StringBuilder b=new StringBuilder().append(m.getImplementation().getRegisterCount()).append('\n');
  for(var i:m.getImplementation().getInstructions()){
   b.append(i.getOpcode()).append('/').append(i.getCodeUnits());
   if(i instanceof ThreeRegisterInstruction){var x=(ThreeRegisterInstruction)i;b.append(" abc=").append(x.getRegisterA()).append(',').append(x.getRegisterB()).append(',').append(x.getRegisterC());}
   else if(i instanceof TwoRegisterInstruction){var x=(TwoRegisterInstruction)i;b.append(" ab=").append(x.getRegisterA()).append(',').append(x.getRegisterB());}
   else if(i instanceof OneRegisterInstruction)b.append(" a=").append(((OneRegisterInstruction)i).getRegisterA());
   if(i instanceof FiveRegisterInstruction){var x=(FiveRegisterInstruction)i;b.append(" args=").append(x.getRegisterCount()).append(':').append(x.getRegisterC()).append(',').append(x.getRegisterD()).append(',').append(x.getRegisterE()).append(',').append(x.getRegisterF()).append(',').append(x.getRegisterG());}
   if(i instanceof RegisterRangeInstruction){var x=(RegisterRangeInstruction)i;b.append(" range=").append(x.getStartRegister()).append(',').append(x.getRegisterCount());}
   if(i instanceof ReferenceInstruction)b.append(" ref=").append(((ReferenceInstruction)i).getReference());
   if(i instanceof WideLiteralInstruction)b.append(" literal=").append(((WideLiteralInstruction)i).getWideLiteral());
   if(i instanceof OffsetInstruction)b.append(" offset=").append(((OffsetInstruction)i).getCodeOffset());
   if(i instanceof SwitchPayload)for(var e:((SwitchPayload)i).getSwitchElements())b.append(" switch=").append(e.getKey()).append(',').append(e.getOffset());
   b.append('\n');
  }
  for(var t:m.getImplementation().getTryBlocks()){
   b.append("try=").append(t.getStartCodeAddress()).append(',').append(t.getCodeUnitCount()).append('\n');
   for(var h:t.getExceptionHandlers())b.append("catch=").append(h.getExceptionType()).append(',').append(h.getHandlerCodeAddress()).append('\n');
  }
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.toString().getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}
 }
    private static MutableMethod mutable(BytecodePatchContext context,String type,String key){
        MutableMethod found=null;
        for(var m:context.mutableClassDefBy(type).getMethods())if(key(m).equals(key)){
            require(found==null,"ambiguous target "+type+key);found=m;
        }
        require(found!=null,"missing target "+type+key);return found;
    }
    private static int at(List<? extends Instruction> code,int address){
        int current=0;
        for(int i=0;i<code.size();i++){if(current==address)return i;current+=code.get(i).getCodeUnits();}
        throw new IllegalStateException("Native search alignment schema: missing address "+address);
    }
    static void apply(BytecodePatchContext context){
        Set<String> seen=new HashSet<>(),calls=new HashSet<>();
        context.classDefForEach(c->{
            for(var m:c.getMethods()){
                if(m.getImplementation()==null)continue;
                String id=fullKey(m);
                if(ORIGINAL.containsKey(id)){
                    require(seen.add(id)&&ORIGINAL.get(id).equals(fingerprint(m)),"original method changed: "+id);
                }
                int address=0;
                for(var i:m.getImplementation().getInstructions()){
                    if(reference(i).equals(CONSTRUCTOR)){
                        String call=id+"@"+String.format(Locale.ROOT,"%04x",address);
                        require(i.getOpcode()==Opcode.INVOKE_DIRECT&&CALLS.contains(call)&&calls.add(call),"unowned value-function constructor: "+call);
                    }
                    address+=i.getCodeUnits();
                }
            }
            return Unit.INSTANCE;
        });
        require(seen.equals(ORIGINAL.keySet()),"missing pinned native methods");
        require(calls.equals(CALLS),"native constructor set changed");
        var nativeProvider=context.classDefByOrNull(PROVIDER);
        require(nativeProvider!=null&&nativeProvider.getAccessFlags()==4113&&nativeProvider.getInterfaces().equals(List.of("Ltvf;")),"value-function class changed");
        int fields=0;
        for(var field:nativeProvider.getFields()){
            fields++;require(field.getName().equals("a")&&field.getType().equals("I")&&field.getAccessFlags()==4114,"selector must remain private final synthetic");
        }
        require(fields==1,"extra selector fields");

        MutableMethod factory=mutable(context,FACTORY,FACTORY_KEY),value=mutable(context,PROVIDER,VALUE_KEY);
        var f=factory.getImplementation();var code=f.getInstructions();
        require(f.getRegisterCount()==60&&code.size()==1203&&f.getTryBlocks().isEmpty(),"factory register/CFG shape");
        int begin=at(code,0x0626),after=at(code,0x0633);
        require(after-begin==6&&code.get(begin).getOpcode()==Opcode.NEW_INSTANCE&&reference(code.get(begin)).equals("Ltxh;"),"constant TOP attribute window");
        require(reference(code.get(begin+5)).equals("Ltxh;-><init>(Ltva;Ljava/lang/Object;Ltvb;Z)V"),"constant attribute constructor");
        require(value.getImplementation().getRegisterCount()==5&&value.getImplementation().getTryBlocks().isEmpty(),"value-function register/try shape");
        var first=value.getImplementation().getInstructions().get(0);
        require(first.getOpcode()==Opcode.IGET&&reference(first).equals("Lilf;->a:I")&&((TwoRegisterInstruction)first).getRegisterA()==3&&((TwoRegisterInstruction)first).getRegisterB()==3,"selector read");

        // Insert before the original constants/switch. On every original0..20 path,
        // v3 still holds the selector and the original next instructions reset v0/v1.
        // Neither the original default20 nor any old switch branch is rewritten.
        InstructionExtensions.INSTANCE.addInstructionsWithLabels(value,1,
            "const/16 v0, "+SEARCH_CASE+"\n"
            +"if-ne v3, v0, :native_search_stock_case\n"
            +"check-cast p1, Lisu;\n"
            +"invoke-virtual {p1}, Lisu;->f()Z\n"
            +"move-result v0\n"
            +"if-eqz v0, :native_search_collapsed\n"
            +"sget-object v0, Lion;->a:Luau;\n"
            +"return-object v0\n"
            +":native_search_collapsed\n"
            +"const/4 v0, 0x0\n"
            +"invoke-static {v0}, Ltyr;->f(I)Ltyr;\n"
            +"move-result-object v0\n"
            +"return-object v0\n"
            +":native_search_stock_case\n"
            +"nop");

        // Keep the preceding v10=Lion.a and all following START/END/radius code.
        // Only TOP changes from a constant property to a reactive value property.
        InstructionExtensions.INSTANCE.removeInstructions(factory,begin,after-begin);
        InstructionExtensions.INSTANCE.addInstructions(factory,begin,
            "new-instance v11, Ltxp;\n"
            +"new-instance v12, Lilf;\n"
            +"const/16 v14, "+SEARCH_CASE+"\n"
            +"invoke-direct {v12, v14}, Lilf;-><init>(I)V\n"
            +"move-object/from16 v14, v41\n"
            +"invoke-direct {v11, v14, v12, v4}, Ltxp;-><init>(Ltva;Ltvf;Ltvb;)V");
        System.out.println("Native search alignment: collapsed TOP0dp, expanded TOP12dp through native reactive DSL; START/END and old value-function cases0..20 preserved");
    }
    private SearchAlignmentHooks(){}
}
