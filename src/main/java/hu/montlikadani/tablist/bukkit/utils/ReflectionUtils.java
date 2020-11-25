package hu.montlikadani.tablist.bukkit.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class ReflectionUtils {

	private static final Gson GSON = new GsonBuilder().create();
	private static final List<JsonObject> JSONLIST = new ArrayList<>();

	private ReflectionUtils() {
	}

	public static Object getHandle(Object obj) throws Exception {
		return invokeMethod(obj, "getHandle");
	}

	public static Object getAsIChatBaseComponent(final String text) throws Exception {
		Class<?> iChatBaseComponent = getNMSClass("IChatBaseComponent");

		if (Version.isCurrentEqualOrHigher(Version.v1_16_R1) && text.contains("#")) {
			JSONLIST.clear();

			JsonObject obj = new JsonObject();
			StringBuilder builder = new StringBuilder();

			String res = text;
			char charBefore = ' ';
			for (int i = 0; i < res.length(); i++) {
				if (i >= res.length()) {
					break;
				}

				// Ignore the last char begins with '&'
				if (charBefore == '&') {
					charBefore = ' ';
					continue;
				}

				char charAt = res.charAt(i);
				if (charAt == '#') {
					String hex = res.substring(i, i + 7);

					if (builder.length() > 0) {
						obj.addProperty("text", builder.toString());
						JSONLIST.add(obj);
						builder = new StringBuilder();
					}

					obj = new JsonObject();
					obj.addProperty("color", hex);
					i += 6; // Increase loop with 6 to ignore hex digit
				} else if (charAt == '&') {
					charBefore = charAt;

					char colorCode = res.charAt(i + 1);
					if (Character.isDigit(colorCode)
							|| ((colorCode >= 'a' && colorCode <= 'f') || (colorCode == 'k' || colorCode == 'l'
									|| colorCode == 'm' || colorCode == 'n' || colorCode == 'o' || colorCode == 'r'))) {
						switch (colorCode) {
						case 'k':
							obj.addProperty("obfuscated", true);
							break;
						case 'o':
							obj.addProperty("italic", true);
							break;
						case 'n':
							obj.addProperty("underlined", true);
							break;
						case 'm':
							obj.addProperty("strikethrough", true);
							break;
						case 'l':
							obj.addProperty("bold", true);
							break;
						case 'r':
							obj.addProperty("text", builder.toString());
							JSONLIST.add(obj);

							obj = new JsonObject();
							builder = new StringBuilder();
							obj.addProperty("color", "white");
							break;
						default:
							obj.addProperty("text", builder.toString());
							JSONLIST.add(obj);

							obj = new JsonObject();
							builder = new StringBuilder();

							String colorName = org.bukkit.ChatColor.getByChar(colorCode).name().toLowerCase();
							obj.addProperty("color", colorName);
							break;
						}
					}
				} else {
					builder.append(charAt);
				}
			}

			obj.addProperty("text", builder.toString());
			JSONLIST.add(obj);

			Method m = iChatBaseComponent.getDeclaredClasses()[0].getMethod("a", String.class);
			return m.invoke(iChatBaseComponent, GSON.toJson(JSONLIST));
		}

		if (Version.isCurrentLower(Version.v1_8_R2)) {
			Class<?> chatSerializer = getNMSClass("ChatSerializer");
			Method m = chatSerializer.getMethod("a", String.class);
			return iChatBaseComponent.cast(m.invoke(chatSerializer, "{\"text\":\"" + text + "\"}"));
		}

		Method m = iChatBaseComponent.getDeclaredClasses()[0].getMethod("a", String.class);
		return m.invoke(iChatBaseComponent, "{\"text\":\"" + text + "\"}");
	}

	public static Object invokeMethod(Object obj, String name) throws Exception {
		return invokeMethod(obj, name, true, false);
	}

	public static Object invokeMethod(Object obj, String name, boolean superClass) throws Exception {
		return invokeMethod(obj, name, true, superClass);
	}

	public static Object invokeMethod(Object obj, String name, boolean declared, boolean superClass) throws Exception {
		Class<?> c = superClass ? obj.getClass().getSuperclass() : obj.getClass();
		Method met = declared ? c.getDeclaredMethod(name) : c.getMethod(name);
		met.setAccessible(true);
		return met.invoke(obj);
	}

	public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + getPackageVersion() + "." + name);
	}

	public static Class<?> getCraftClass(String className) throws ClassNotFoundException {
		return Class.forName("org.bukkit.craftbukkit." + getPackageVersion() + "." + className);
	}

	public static Field getField(Object clazz, String name) throws Exception {
		return getField(clazz, name, true);
	}

	public static Field getField(Object clazz, String name, boolean declared) throws Exception {
		return getField(clazz.getClass(), name, declared);
	}

	public static Field getField(Class<?> clazz, String name) throws Exception {
		return getField(clazz, name, true);
	}

	public static Field getField(Class<?> clazz, String name, boolean declared) throws Exception {
		Field field = declared ? clazz.getDeclaredField(name) : clazz.getField(name);
		field.setAccessible(true);
		return field;
	}

	public static void modifyFinalField(Field field, Object target, Object newValue) throws Exception {
		if (!ClassMethods.isAccessible(field, target)) {
			field.setAccessible(true);
		}

		int mods = field.getModifiers();
		if (!Modifier.isFinal(mods)) {
			return;
		}

		Field modifiersField = null;
		try {
			modifiersField = getField(Field.class, "modifiers");
		} catch (NoSuchFieldException e) { // Java 12+
			Method meth = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
			boolean accessibleBeforeSet = ClassMethods.isAccessible(meth, null);
			meth.setAccessible(true);

			Field[] fields = (Field[]) meth.invoke(Field.class, false);
			meth.setAccessible(accessibleBeforeSet);

			for (Field f : fields) {
				if ("modifiers".equals(f.getName())) {
					modifiersField = f;
					break;
				}
			}
		}

		if (modifiersField == null) {
			return;
		}

		boolean accessibleBeforeSet = ClassMethods.isAccessible(modifiersField, null);
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, mods & ~Modifier.FINAL);
		modifiersField.setAccessible(accessibleBeforeSet);
		field.set(target, newValue);
	}

	public static Object getFieldObject(Object object, Field field) throws Exception {
		return field.get(object);
	}

	public static void setField(Object object, String fieldName, Object fieldValue) throws Exception {
		getField(object, fieldName).set(object, fieldValue);
	}

	public static void sendPacket(Player player, Object packet) {
		try {
			Object playerHandle = getHandle(player);
			Object playerConnection = getFieldObject(playerHandle, getField(playerHandle, "playerConnection"));

			playerConnection.getClass().getDeclaredMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection,
					packet);
		} catch (Exception e) {
		}
	}

	public static String getPackageVersion() {
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}

	public static class Classes {

		public static Object getPlayerConstructor(Player player, Object profile) {
			Class<?> server = getMinecraftServer();
			Object serverIns = getServer(server);

			try {
				Class<?> manager = getNMSClass("PlayerInteractManager");
				Object managerIns = null, world = null;
				if (Version.isCurrentEqualOrHigher(Version.v1_14_R1)) {
					world = getHandle(player.getWorld());
					managerIns = manager.getConstructor(world.getClass()).newInstance(world);
				} else if (Version.isCurrentEqual(Version.v1_13_R1) || Version.isCurrentEqual(Version.v1_13_R2)) {
					world = getHandle(player.getWorld());
				} else {
					world = server.getDeclaredMethod("getWorldServer", int.class).invoke(serverIns, 0);
				}

				if (managerIns == null) {
					managerIns = manager.getConstructors()[0].newInstance(world);
				}

				Object playerHandle = getHandle(player);
				return playerHandle.getClass().getConstructor(server, world.getClass(), profile.getClass(), manager)
						.newInstance(serverIns, world, profile, managerIns);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		public static Class<?> getMinecraftServer() {
			Class<?> server = null;

			try {
				server = getNMSClass("MinecraftServer");
			} catch (ClassNotFoundException c) {
				try {
					server = getNMSClass("DedicatedServer");
				} catch (ClassNotFoundException e) {
				}
			}

			return server;
		}

		public static Object getServer(Class<?> server) {
			Object serverIns = null;

			try {
				serverIns = server.getMethod("getServer")
						.invoke(ReflectionUtils.getCraftClass("CraftServer").cast(Bukkit.getServer()));
			} catch (Exception x) {
				try {
					serverIns = server.getMethod("getServer").invoke(server);
				} catch (Exception e) {
				}
			}

			return serverIns;
		}

		public static Class<?> getEnumPlayerInfoAction(Class<?> packetPlayOutPlayerInfo) {
			Class<?> enumPlayerInfoAction = null;

			try {
				if (Version.isCurrentEqual(Version.v1_8_R1)) {
					enumPlayerInfoAction = getNMSClass("EnumPlayerInfoAction");
				} else if (Version.isCurrentEqualOrHigher(Version.v1_11_R1)) {
					enumPlayerInfoAction = packetPlayOutPlayerInfo.getDeclaredClasses()[1];
				}

				if (enumPlayerInfoAction == null) {
					enumPlayerInfoAction = packetPlayOutPlayerInfo.getDeclaredClasses()[2];
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return enumPlayerInfoAction;
		}
	}

	@SuppressWarnings("deprecation")
	public abstract static class ClassMethods {

		public static boolean isAccessible(Field field, Object target) {
			return getCurrentVersion() >= 9 && target != null ? field.canAccess(target) : field.isAccessible();
		}

		public static boolean isAccessible(Method method, Object target) {
			return getCurrentVersion() >= 9 && target != null ? method.canAccess(target) : method.isAccessible();
		}

		public static int getCurrentVersion() {
			String currentVersion = System.getProperty("java.version");
			if (currentVersion.contains("_")) {
				currentVersion = currentVersion.split("_")[0];
			}

			currentVersion = currentVersion.replaceAll("[^\\d]|_", "");

			for (int i = 8; i <= 18; i++) {
				if (currentVersion.contains(Integer.toString(i))) {
					return i;
				}
			}

			return 0;
		}
	}
}