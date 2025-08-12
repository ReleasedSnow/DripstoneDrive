package me.releasedsnow.com.hackathon;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {


    public static FileConfiguration config = Plugin.getPlugin().getConfig();

    public ConfigManager() {
        this.deathMessages();
        this.defaults();
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    private void deathMessages(){

    }

    public void defaults(){
        FileConfiguration config = getConfig();
        String path = "Abilities.Earth.DripstoneDrive";
        config.addDefault(path + "Range", 8);
        config.addDefault(path + "LingerDuration", 3500);
        config.addDefault(path + "SpikeGrowthDelay", 0);
        config.addDefault(path + "Damage", 2);
        config.addDefault(path + "Knockup", 2);
        config.addDefault(path + "TrailBlockRadius", 2);



        config.options().copyDefaults(true);
        Plugin.getPlugin().saveConfig();
    }
}
