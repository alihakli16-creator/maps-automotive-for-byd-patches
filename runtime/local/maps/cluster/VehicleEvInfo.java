package local.maps.cluster;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/** Pinned native vehicle-info facade. Added invocation work is in-memory only. */
final class VehicleEvInfo {
    private VehicleEvInfo() {}

    static Object wrap(Object original) {
        if (original == null) return null;
        if (Proxy.isProxyClass(original.getClass()) && Proxy.getInvocationHandler(original) instanceof Adapter)
            return original;
        try {
            ClassLoader loader = original.getClass().getClassLoader();
            Class<?> type = loader.loadClass("fxe");
            if (!type.isInterface() || !type.isInstance(original)) return original;
            Class<?> list = loader.loadClass("abrt"), fuel = loader.loadClass("fwz");
            require(type.getMethod("d").getReturnType() == list, "Native fuel-list ABI");
            Class<?> optional = type.getMethod("e").getReturnType();
            require("j$.util.Optional".equals(optional.getName()), "Native connector-optional ABI");
            for (String name : new String[]{"i", "j", "k", "l"})
                require(type.getMethod(name).getReturnType() == boolean.class, "Native vehicle gate ABI");
            Field electricField = fuel.getDeclaredField("k"); electricField.setAccessible(true);
            Object electric = electricField.get(null);
            require(electric instanceof Enum && "ELECTRIC".equals(((Enum<?>) electric).name()), "Native electric fuel enum");

            // Catalog/CarInfo connector numbering differs from the energy protobuf enum6.
            Field mapping = loader.loadClass("fxm").getDeclaredField("b"); mapping.setAccessible(true);
            Object combo2 = ((Map<?, ?>) mapping.get(null)).get("COMBO_2");
            require(Integer.valueOf(5).equals(combo2), "Pinned native CCS2 catalog mapping");
            Method singleton = list.getDeclaredMethod("q", Object.class); singleton.setAccessible(true);
            Object fuelTypes = singleton.invoke(null, electric);
            Object connectors = singleton.invoke(null, combo2);
            Method of = optional.getMethod("of", Object.class); of.setAccessible(true);
            Object connectorTypes = of.invoke(null, connectors);
            return Proxy.newProxyInstance(loader, new Class<?>[]{type}, new Adapter(original, fuelTypes, connectorTypes));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError incompatible) {
            // An unknown Maps ABI retains its original vehicle information and capabilities.
            return original;
        }
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }

    private static final class Adapter implements InvocationHandler {
        private final Object original, fuelTypes, connectorTypes;
        Adapter(Object original, Object fuelTypes, Object connectorTypes) {
            this.original = original; this.fuelTypes = fuelTypes; this.connectorTypes = connectorTypes;
        }
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                if ("equals".equals(method.getName())) return proxy == args[0];
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                if ("toString".equals(method.getName())) return "VehicleEvInfo(" + original.getClass().getName() + ")";
            }
            String name = method.getName();
            if (method.getParameterTypes().length == 0) {
                // Identity is available at construction, including before async profile loading.
                if (VehicleEvBridge.hasVehicleProfile()) {
                    if ("d".equals(name)) return fuelTypes;
                    if ("e".equals(name)) return connectorTypes;
                    if ("j".equals(name) || "k".equals(name)) return true;
                    if ("l".equals(name)) return false;
                }
                // Charging-plan capability follows an accepted, still-fresh BYD native model.
                if ("i".equals(name) && VehicleEvBridge.canPlanRoutes()) return true;
            }
            try { method.setAccessible(true); return method.invoke(original, args); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
        }
    }
}
