
package org.kraetzin.mods.bountyonburn;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Initiator implements WurmServerMod, ServerStartedListener, Configurable, PreInitable {
    private static Logger logger;
    static Long coinBountyDefault;
    static int karmaBounty;
    static String bountyMessage;
    private static boolean debug;
    static int actionTime;
    static boolean magicFire;
    private static boolean burnInForges;
    private static boolean burnAllAction;
    static boolean burnUniques;
    static boolean burnDragons;
    public static HashMap<String, Integer> coinBountyMap;
    public static HashMap<String, Float> statusMultiplierMap;

    static {
        logger = Logger.getLogger(Initiator.class.getName());
        coinBountyDefault = 0L;
        karmaBounty = 0;
        bountyMessage = "Iron coin has been deposited into your bank account for burning the corpse";
        actionTime = 5;
        magicFire = true;
        burnInForges = true;
        burnAllAction = true;
        burnUniques = false;
        burnDragons = false;
        debug = false;
    }

    @Override
    public void configure(Properties properties) {
        DecimalFormat s = new DecimalFormat("#,###,###");
        Initiator.coinBountyDefault = Long.valueOf(properties.getProperty("bountyDefault", Long.toString(Initiator.coinBountyDefault)));
        Initiator.karmaBounty = Integer.parseInt(properties.getProperty("karmaBounty", Integer.toString(Initiator.karmaBounty)));
        Initiator.actionTime = Integer.valueOf(properties.getProperty("actionTime", Integer.toString(Initiator.actionTime)));
        Initiator.bountyMessage = String.valueOf(properties.getProperty("bountyMessage", String.valueOf(Initiator.bountyMessage)));
        Initiator.magicFire = Boolean.parseBoolean(properties.getProperty("magicFire", String.valueOf(Initiator.magicFire)));
        Initiator.burnInForges = Boolean.parseBoolean(properties.getProperty("burnInForges", String.valueOf(Initiator.burnInForges)));
        Initiator.burnAllAction = Boolean.parseBoolean(properties.getProperty("burnAllAction", String.valueOf(Initiator.burnAllAction)));
        Initiator.burnUniques = Boolean.parseBoolean(properties.getProperty("burnUniques", String.valueOf(Initiator.burnUniques)));
        Initiator.burnDragons = Boolean.parseBoolean(properties.getProperty("burnDragons", String.valueOf(Initiator.burnDragons)));
        Initiator.debug = Boolean.parseBoolean(properties.getProperty("debug", String.valueOf(Initiator.debug)));
        // logging
        logger.log(Level.INFO, "========================== Bounty On Burn Mod Settings =============================");
        if (Initiator.debug) {
            logger.log(Level.INFO, "Mod Logging: Enabled");
        } else {
            logger.log(Level.INFO, "Mod Logging: Disabled");
        }
        if (Initiator.magicFire) {
            Initiator.jDebug("Magic Fire: Enabled");
        } else {
            Initiator.jDebug("Normal Fire: Enabled");
        }
        if (Initiator.burnInForges) {
            Initiator.jDebug("Burning In Forges/Ovens/Kilns/Campfires/Piles/Smelters: Enabled");
        } else {
            Initiator.jDebug("Burning In Forges/Ovens/Kilns/Campfires/Piles/Smelters: Disabled");
        }
        if (Initiator.burnAllAction) {
            Initiator.jDebug("Burn All Action: Enabled");
        } else {
            Initiator.jDebug("Burn All Action: Disabled");
        }
        if (Initiator.burnUniques) {
            Initiator.jDebug("Burn Uniques: Enabled");
        } else {
            Initiator.jDebug("Burn Uniques: Disabled");
        }
        if (Initiator.burnDragons) {
            Initiator.jDebug("Burn Dragons: Enabled");
        } else {
            Initiator.jDebug("Burn Dragons: Disabled");
        }
        loadBounties(properties);
        loadStatusMultipliers(properties);
        //Initiator.jDebug("coinBounty: " + s.format(Initiator.coinBounty) + " iron coin");
        Initiator.jDebug("karmaBounty: " + s.format(Initiator.karmaBounty) + " karma points");
        Initiator.jDebug("actionTime: " + Initiator.actionTime + " seconds");
        Initiator.jDebug("bountyMessage: " + Initiator.bountyMessage);
        logger.log(Level.INFO, "========================== Bounty On Burn Mod Settings =============================");
    }

    static void jDebug(String msg) {
        if (debug) {
            logger.log(Level.INFO, msg);
        }
    }

    @Override
    public void preInit() {
        ModActions.init();
    }

    @Override
    public void onServerStarted() {
        jDebug("onServerStarted called");
        try {
            ModActions.registerAction(new BurnCorpseAction());
            if (burnAllAction) {
                ModActions.registerAction(new BurnAllAction());
            }
        } catch (IllegalArgumentException | ClassCastException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Error in onServerStarted()", e);
        }
        jDebug("all onServerStarted completed");
    }

    static boolean canUse(Creature performer, Item source, Item target) {
        int ttemp = target.getTemplateId();
        int stemp = source.getTemplateId();
        return performer.isPlayer() && source != null && target != null && ttemp == ItemList.corpse && (source.isOnFire() || stemp == ItemList.flintSteel || stemp == ItemList.wandDeity) && source.getTopParent() == performer.getInventory().getWurmId() && (target.getTopParent() != performer.getInventory().getWurmId());
    }

    static boolean canUseInForge(Creature performer, Item source, Item target) {
        int ttemp = target.getTemplateId();
        int stemp = source.getTemplateId();
        return performer.isPlayer() && source != null && target != null && stemp == ItemList.corpse && target.isOnFire() && (ttemp == ItemList.kiln || target.isForgeOrOven() || target.isFireplace() || ttemp == ItemList.charcoalPile || ttemp == ItemList.smelter || ttemp == ItemList.campfire || Initiator.burnInForges);
    }

    static boolean isUniqueCorpse(Item source, Item target) {
        String tname = target.getName();
        String sname = source.getName();
        return source != null && target != null && target.getName() != null && (source.getTemplateId() == ItemList.corpse || target.getTemplateId() == ItemList.corpse) && (tname.contains("forest giant") || tname.contains("goblin leader") || tname.contains("kyklops") || tname.contains("troll king")) || (sname.contains("forest giant") || sname.contains("goblin leader") || sname.contains("kyklops") || sname.contains("troll king"));
    }

    public static void loadBounties(Properties properties) {
        Initiator.coinBountyMap = new HashMap<String, Integer>();
        for (String key : properties.stringPropertyNames()) {
            if (key.contains("_bounty")) {
                String creature = key.replace("_bounty", "").replace("_", " ");
                int bounty = Integer.parseInt(properties.getProperty(key));
                Initiator.coinBountyMap.put(creature.toLowerCase(), bounty);
                Initiator.jDebug("Loaded bounty for creature:" + creature + ": " + bounty + " iron");
            }
        }
    }
    public static void loadStatusMultipliers(Properties properties) {
        Initiator.statusMultiplierMap = new HashMap<String, Float>();
        for (String key : properties.stringPropertyNames()) {
            if (key.contains("_status")) {
                String status = key.replace("_status", "").replace("_", " ");
                float multiplier = Float.parseFloat(properties.getProperty(key));
                Initiator.statusMultiplierMap.put(status.toLowerCase(), multiplier);
                Initiator.jDebug("Loaded multiplier for status:" + status + ": " + multiplier + "x");
            }
        }
    }
    static long getCoinBounty(Item target) {
        String tname = target.getName();
        long creature_bounty = Initiator.coinBountyDefault;
        for (String creature : coinBountyMap.keySet()) {
            if (tname.contains(creature)) {
                creature_bounty = coinBountyMap.get(creature);
            }
        }
        for (String status : statusMultiplierMap.keySet()) {
            if (tname.contains(status)) {
                creature_bounty *= statusMultiplierMap.get(status);
            }
        }
        return creature_bounty;
    }

    static String coinConversion(long bounty) {
        long iron = bounty % 100;
        long copper = bounty / 100;
        String message = "";
        if (copper > 0) {
            message += String.format("%sc", copper);
        }
        if (iron > 0) {
            message += String.format("%si", iron);
        }
        return message;
    }

    public String getVersion() {
        return "v1.2";
    }
}