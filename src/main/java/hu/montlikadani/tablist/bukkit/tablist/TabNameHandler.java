package hu.montlikadani.tablist.bukkit.tablist;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.Global;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.config.ConfigValues;
import hu.montlikadani.tablist.bukkit.utils.PluginUtils;

public class TabNameHandler {

	private TabList plugin;

	private final Set<Predicate<String>> restrictedNames = new HashSet<>();

	public TabNameHandler(TabList plugin) {
		this.plugin = plugin;
	}

	public Set<Predicate<String>> getRestrictedNames() {
		return new HashSet<>(restrictedNames);
	}

	public void loadRestrictedNames() {
		restrictedNames.clear();

		if (!ConfigValues.isTabNameEnabled()) {
			return;
		}

		for (String rn : ConfigValues.getRestrictedTabNames()) {
			try {
				restrictedNames.add(Pattern.compile(rn).asPredicate());
			} catch (java.util.regex.PatternSyntaxException e) {
			}
		}
	}

	public void loadTabName(Player p) {
		if (!ConfigValues.isTabNameEnabled()) {
			return;
		}

		String result = "";
		if (ConfigValues.isTabNameUsePluginNickName()) {
			String nickName = PluginUtils.getNickName(p);
			if (!nickName.isEmpty()) {
				result = colorMsg(nickName + "&r");
			}
		} else {
			String name = getTabName(p);
			if (!name.isEmpty()) {
				name = plugin.getPlaceholders().setPlaceholders(p, Global.setSymbols(name));

				if (ConfigValues.isDefaultColorEnabled()) {
					result = colorMsg(ConfigValues.getDefaultTabNameColor() + name + "&r");
				} else {
					result = ConfigValues.isTabNameColorCodeEnabled() ? colorMsg(name + "&r") : name + "\u00a7r";
				}
			} else if (ConfigValues.isDefaultColorEnabled()) {
				result = colorMsg(ConfigValues.getDefaultTabNameColor() + p.getName());
			}
		}

		if (!result.isEmpty()) {
			p.setPlayerListName(result);
		}
	}

	public boolean isNameDisabled(String arg) {
		for (Predicate<String> b : restrictedNames) {
			if (b.test(arg)) {
				return true;
			}
		}

		return false;
	}

	public String getTabName(Player p) {
		return getTabName(p.getName());
	}

	private String getTabName(String name) {
		return ConfigValues.isTabNameEnabled() && plugin.getConf().getNames().contains("players." + name)
				? plugin.getConf().getNames().getString("players." + name + ".tabname", "")
				: "";
	}

	public void setTabName(Player p, String name) {
		if (!ConfigValues.isTabNameEnabled()) {
			return;
		}

		String result = "", tName = "";

		if (ConfigValues.isTabNameUsePluginNickName()) {
			String nickName = PluginUtils.getNickName(p);
			if (!nickName.isEmpty()) {
				result = colorMsg(nickName);
				tName = nickName;
			}
		} else {
			if (ConfigValues.isDefaultColorEnabled()) {
				result = colorMsg(ConfigValues.getDefaultTabNameColor()
						+ plugin.getPlaceholders().setPlaceholders(p, Global.setSymbols(name)) + "&r");
			} else {
				result = ConfigValues.isTabNameColorCodeEnabled()
						? colorMsg(plugin.getPlaceholders().setPlaceholders(p, Global.setSymbols(name)) + "&r")
						: name + "\u00a7r";
			}

			tName = name;
		}

		if (!result.isEmpty() && !ConfigValues.isUseTabName()) {
			p.setPlayerListName(result);
		}

		if (!tName.isEmpty()) {
			plugin.getConf().getNames().set("players." + p.getName() + ".tabname", tName);

			try {
				plugin.getConf().getNames().save(plugin.getConf().getNamesFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void unSetTabName(Player p) {
		if (!ConfigValues.isTabNameEnabled()) {
			return;
		}

		if (ConfigValues.isUseTabName()
				&& plugin.getGroups().getTLPlayerMap().containsKey(p.getUniqueId().toString())) {
			plugin.getGroups().getTLPlayerMap().get(p.getUniqueId().toString()).setTabName(null);
		} else {
			p.setPlayerListName(p.getName());
		}

		plugin.getConf().getNames().set("players." + p.getName() + ".tabname", null);
		plugin.getConf().getNames().set("players." + p.getName(), null);

		try {
			plugin.getConf().getNames().save(plugin.getConf().getNamesFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
