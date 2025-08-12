package me.releasedsnow.com.hackathon;

import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable(){
        plugin = this;
        new ConfigManager();
        CoreAbility.registerPluginAbilities(plugin, "me.releasedsnow.com.hackathon.Ability");

    }


    public static Plugin getPlugin() {
        return plugin;
    }
}
