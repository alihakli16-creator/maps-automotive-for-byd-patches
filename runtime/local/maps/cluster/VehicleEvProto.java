package local.maps.cluster;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the pinned 26.09 external EV message consumed by dpp.p/hlh.
 * Capacity is a manual owner profile, current energy is derived from that profile
 * and SOC, and the constant J/m term is derived from the car's estimated range.
 * No native implementation, charging power, charging curve or demo defaults live here.
 */
final class VehicleEvProto {
    private VehicleEvProto() {}

    static Object build(ClassLoader loader, int capacityWh, double soc, double rangeKm,
                        int nativeConnectorEnum) throws Exception {
        Values values = values(capacityWh, soc, rangeKm, nativeConnectorEnum);
        Message current = message(loader, "afpd");
        set(current.value, "b", values.currentWh);
        Message capacity = message(loader, "afpd");
        set(capacity.value, "b", capacityWh);

        Message energy = message(loader, "afpx");
        set(energy.value, "b", 0x2 | 0x4);
        set(energy.value, "d", finish(current));
        set(energy.value, "e", finish(capacity));

        Message coefficient = message(loader, "afor");
        set(coefficient.value, "b", values.joulesPerMetre);
        Message consumption = message(loader, "afpm");
        set(consumption.value, "b", 0x1);
        set(consumption.value, "c", finish(coefficient));

        Message vehicle = message(loader, "afpv");
        set(vehicle.value, "b", 0x1 | 0x2);
        set(vehicle.value, "c", finish(energy));
        set(vehicle.value, "d", finish(consumption));
        if (nativeConnectorEnum != -1) {
            Message connector = message(loader, "afpe");
            set(connector.value, "c", nativeConnectorEnum);
            // afpe.d is optional maximum power. Keep it absent when unknown.
            Message connectors = message(loader, "afnv");
            Field listField = field(connectors.value.getClass(), "b");
            Object oldList = listField.get(connectors.value);
            Class<?> base = loader.loadClass("akcx");
            Method copy = base.getDeclaredMethod("cV", listField.getType());
            copy.setAccessible(true);
            Object mutableList = copy.invoke(null, oldList);
            @SuppressWarnings("unchecked") List<Object> entries = (List<Object>) mutableList;
            entries.add(finish(connector));
            listField.set(connectors.value, mutableList);
            set(vehicle.value, "g", finish(connectors));
            set(vehicle.value, "b", 0x1 | 0x2 | 0x10);
        }
        Object result = finish(vehicle);
        verify(result, capacityWh, soc, rangeKm, nativeConnectorEnum);
        return result;
    }

    /** Read the exact external fields back; throws rather than publishing a wrong schema. */
    static Map<String, Object> verify(Object vehicle, int capacityWh, double soc, double rangeKm,
                                      int nativeConnectorEnum) throws Exception {
        Values expected = values(capacityWh, soc, rangeKm, nativeConnectorEnum);
        require(vehicle != null && "afpv".equals(vehicle.getClass().getName()), "External EV type");
        require(number(vehicle, "b").intValue() == (nativeConnectorEnum == -1 ? 3 : 19), "Vehicle presence");
        Object energy = get(vehicle, "c"), consumption = get(vehicle, "d");
        require(number(energy, "b").intValue() == 6, "Energy presence");
        require(number(get(energy, "d"), "b").intValue() == expected.currentWh, "Current Wh");
        require(number(get(energy, "e"), "b").intValue() == capacityWh, "Capacity Wh");
        require(get(energy, "c") == null, "Unknown energy quantity must be absent");
        require(number(consumption, "b").intValue() == 1, "Consumption presence");
        float actual = number(get(consumption, "c"), "b").floatValue();
        require(Float.floatToIntBits(actual) == Float.floatToIntBits(expected.joulesPerMetre), "Distance coefficient J/m");
        require(get(consumption, "d") == null && get(consumption, "e") == null, "Unknown consumption terms must be absent");
        // dpp.p intrinsically copies these proto3 zero defaults. They are not measured range/power.
        require(number(energy, "g").floatValue() == 0f && number(energy, "h").floatValue() == 0f,
                "Unverified energy scalars must remain default");
        require(get(vehicle, "e") == null && get(vehicle, "f") == null && get(vehicle, "j") == null,
                "Unknown charging curves must be absent");
        Object connectors = get(vehicle, "g");
        if (nativeConnectorEnum == -1) require(connectors == null, "Unknown connector must be absent");
        else {
            List<?> entries = (List<?>) get(connectors, "b");
            require(entries.size() == 1, "Connector count");
            Object connector = entries.get(0);
            require(number(connector, "c").intValue() == nativeConnectorEnum, "Connector enum");
            require(number(connector, "b").intValue() == 0 && get(connector, "d") == null,
                    "Unknown maximum charging power must be absent");
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("profile_capacity_wh", capacityWh);
        summary.put("derived_current_wh", expected.currentWh);
        summary.put("derived_joules_per_metre", actual);
        summary.put("derived_range_km", expected.currentWh * 3.6 / actual);
        summary.put("native_connector_enum", nativeConnectorEnum);
        summary.put("charging_power_present", false);
        summary.put("charging_curve_present", false);
        summary.put("unverified_scalars_are_proto_defaults", true);
        return summary;
    }

    private static Values values(int capacityWh, double soc, double rangeKm, int connector) {
        if (capacityWh <= 0 || !Double.isFinite(soc) || soc <= 0 || soc > 100
                || !Double.isFinite(rangeKm) || rangeKm <= 0 || rangeKm > 3000
                || (connector != -1 && connector <= 0))
            throw new IllegalArgumentException("Usable positive EV profile and readings required");
        long currentWh = Math.round(capacityWh * (soc / 100.0));
        if (currentWh <= 0 || currentWh > capacityWh)
            throw new IllegalArgumentException("Positive derived current energy required");
        // Wh × 3600 J/Wh ÷ (km × 1000 m/km) = J/m.
        float coefficient = (float) (currentWh * 3.6 / rangeKm);
        if (!Float.isFinite(coefficient) || coefficient <= 0)
            throw new IllegalArgumentException("Finite positive distance coefficient required");
        return new Values((int) currentWh, coefficient);
    }

    private static Message message(ClassLoader loader, String name) throws Exception {
        Class<?> type = loader.loadClass(name);
        Object defaultValue = field(type, "a").get(null);
        Object builder = invoke(defaultValue, "cC");
        invoke(builder, "bI");
        Object value = get(builder, "b");
        require(type.isInstance(value) && value != defaultValue, "Mutable protobuf instance");
        return new Message(builder, value);
    }
    private static Object finish(Message message) throws Exception { return invoke(message.builder, "bE"); }
    private static Object invoke(Object object, String name) throws Exception {
        Method method = object.getClass().getMethod(name);
        method.setAccessible(true);
        return method.invoke(object);
    }
    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { Field field = current.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException absent) { /* Search the native protobuf base. */ }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
    private static Object get(Object object, String name) throws Exception {
        if (object == null) throw new IllegalStateException("Missing native EV message");
        return field(object.getClass(), name).get(object);
    }
    private static Number number(Object object, String name) throws Exception { return (Number) get(object, name); }
    private static void set(Object object, String name, Object value) throws Exception { field(object.getClass(), name).set(object, value); }
    private static void require(boolean valid, String what) { if (!valid) throw new IllegalStateException(what); }
    private static final class Message {
        final Object builder, value;
        Message(Object builder, Object value) { this.builder = builder; this.value = value; }
    }
    private static final class Values {
        final int currentWh;
        final float joulesPerMetre;
        Values(int currentWh, float joulesPerMetre) { this.currentWh = currentWh; this.joulesPerMetre = joulesPerMetre; }
    }
}
