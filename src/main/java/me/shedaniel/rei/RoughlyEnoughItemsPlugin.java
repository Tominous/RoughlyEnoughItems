package me.shedaniel.rei;

import com.google.common.collect.Maps;
import com.google.gson.JsonParser;
import me.shedaniel.rei.api.IRecipePlugin;
import me.shedaniel.rei.api.Identifier;
import me.shedaniel.rei.api.REIPlugin;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoughlyEnoughItemsPlugin {
    
    public static final Logger LOGGER = LogManager.getFormatterLogger("REI");
    private static final Map<Identifier, IRecipePlugin> plugins = Maps.newHashMap();
    private static JsonParser parser = new JsonParser();
    private static boolean loaded = false;
    
    public static IRecipePlugin registerPlugin(Identifier identifier, IRecipePlugin plugin) {
        plugins.put(identifier, plugin);
        RoughlyEnoughItemsPlugin.LOGGER.info("REI: Registered Plugin from %s by %s.", identifier.toString(), plugin.getClass().getSimpleName());
        plugin.onFirstLoad(RoughlyEnoughItemsCore.getPluginDisabler());
        return plugin;
    }
    
    public static List<IRecipePlugin> getPlugins() {
        return new LinkedList<>(plugins.values());
    }
    
    public static Identifier getIdentifier(IRecipePlugin plugin) {
        for(Identifier identifier : plugins.keySet())
            if (plugins.get(identifier).equals(plugin))
                return identifier;
        return null;
    }
    
    @SubscribeEvent
    public static void discoverPlugins(FMLClientSetupEvent event) {
        if (loaded)
            return;
        loaded = true;
        LOGGER.info("REI: Discovering Plugins.");
        ModList.get().getAllScanData().forEach(scan -> {
            scan.getAnnotations().forEach(a -> {
                if (a.getAnnotationType().getClassName().equals(REIPlugin.class.getName())) {
                    String required = (String) a.getAnnotationData().getOrDefault("value", "");
                    if (required.isEmpty() || ModList.get().isLoaded(required)) {
                        try {
                            Class<?> clazz = Class.forName(a.getMemberName());
                            if (IRecipePlugin.class.isAssignableFrom(clazz)) {
                                IRecipePlugin plugin = (IRecipePlugin) clazz.newInstance();
                                registerPlugin(new Identifier(clazz.getAnnotation(REIPlugin.class).identifier()), plugin);
                            }
                        } catch (Exception e) {
                            LOGGER.error("REI: Can't load REI plugin. %s", a.getMemberName());
                        }
                    }
                }
            });
        });
        LOGGER.info("REI: Discovered %d REI Plugins%s", plugins.size(), (plugins.size() > 0 ? ": " + String.join(", ", plugins.keySet().stream().map(Identifier::toString).collect(Collectors.toList())) : "."));
    }
    
}