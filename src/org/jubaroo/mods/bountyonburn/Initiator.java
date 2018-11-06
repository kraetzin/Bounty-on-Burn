
package org.jubaroo.mods.bountyonburn;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.text.DecimalFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Initiator implements WurmServerMod, ServerStartedListener, Configurable, PreInitable {
    private static Logger logger;
    static Long corpseBounty;
    static String bountyMessage;
    private static boolean debug;
    static int actionTime;
    static boolean magicFire;

    static {
        logger = Logger.getLogger(Initiator.class.getName());
        corpseBounty = 0L;
        bountyMessage = "Iron coin has been deposited into your bank account for burning the corpse";
        actionTime = 5;
        magicFire = true;
        debug = false;
    }

    @Override
    public void configure(Properties properties) {
        DecimalFormat s = new DecimalFormat("#,###,###");
        Initiator.corpseBounty = Long.valueOf(properties.getProperty("corpseBounty", Long.toString(Initiator.corpseBounty)));
        Initiator.actionTime = Integer.valueOf(properties.getProperty("actionTime", Integer.toString(Initiator.actionTime)));
        Initiator.bountyMessage = String.valueOf(properties.getProperty("bountyMessage", String.valueOf(Initiator.bountyMessage)));
        Initiator.magicFire = Boolean.parseBoolean(properties.getProperty("magicFire", String.valueOf(Initiator.magicFire)));
        Initiator.debug = Boolean.parseBoolean(properties.getProperty("debug", String.valueOf(Initiator.debug)));
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
        Initiator.jDebug("corpseBounty: " + s.format(Initiator.corpseBounty) + " iron coin");
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
        } catch (IllegalArgumentException | ClassCastException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Error in onServerStarted()", e);
        }
        jDebug("all onServerStarted completed");
    }

    public String getVersion() {
        return "v1.0";
    }

}