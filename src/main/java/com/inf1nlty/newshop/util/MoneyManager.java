package com.inf1nlty.newshop.util;

import net.minecraft.ChatMessageComponent;
import net.minecraft.EntityPlayer;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.UUID;

/** In-memory balances in tenths, persisted via EntityPlayerMixin (shop_money) and shop_balances.dat.
 *
 * <p>Can also be instantiated as a per-player wrapper (used by {@link ShopPlayer#newShop$getMoneyManager})
 * to provide an instance-based API
 */
public class MoneyManager {

    private static final WeakHashMap<EntityPlayer, Integer> BALANCES = new WeakHashMap<>();
    private static final Map<UUID, Integer> BALANCES_BY_UUID = new HashMap<>();

    private static File SHOP_DIR = null;
    private static File BALANCES_FILE = null;
    private static boolean initialized = false;

    public static void init(File shopDir) {
        if (initialized) return;
        SHOP_DIR = shopDir;
        BALANCES_FILE = new File(SHOP_DIR, "shop_balances.dat");
        if (!SHOP_DIR.exists()) SHOP_DIR.mkdirs();
        initialized = true;
    }

    private static void ensureInitialized() {
        if (!initialized || SHOP_DIR == null || BALANCES_FILE == null)
            throw new IllegalStateException("MoneyManager not initialized.");
    }

    private final EntityPlayer instancePlayer;

    /** Creates a per-player instance wrapper. Use {@link ShopPlayer#getMoneyManager(EntityPlayer)}. */
    public MoneyManager(EntityPlayer player) {
        this.instancePlayer = player;
    }

    /** Static-only usage; keeps backward compat. */
    private MoneyManager() {
        this.instancePlayer = null;
    }

    /** Returns the player's balance as a double (converted from internal tenths). */
    public double getMoney() {
        return getBalanceTenths(instancePlayer) / 10.0;
    }

    /** Sets the player's balance (converted to internal tenths). */
    public void setMoney(double money) {
        int tenths = toTenths(money);
        setBalanceTenths(instancePlayer, tenths);
        if (instancePlayer != null) {
            setBalanceTenths(PlayerIdentityUtil.getOfflineUUID(instancePlayer.username), tenths);
        }
    }

    /** Adds {@code money} to the player's balance. */
    public void addMoney(double money) {
        addTenths(instancePlayer, toTenths(money));
    }

    /** Subtracts {@code money} from the player's balance. */
    public void subMoney(double money) {
        addTenths(instancePlayer, -toTenths(money));
    }

    public void addMoneyWithSimplify(double money) {
        addMoney(money);
        simplify();
    }

    public void subMoneyWithSimplify(double money) {
        subMoney(money);
        simplify();
    }

    /** Rounds the player's balance to 2 decimal places. */
    public void simplify() {
        double current = getMoney();
        BigDecimal bd = new BigDecimal(current).setScale(2, RoundingMode.HALF_UP);
        setMoney(bd.doubleValue());
    }

    public String getMoneyText() {
        return String.format("%.2f", getMoney());
    }

    public void sendBalanceHint() {
        if (instancePlayer == null || instancePlayer.worldObj.isRemote) return;
        int balance = getBalanceTenths(instancePlayer);
        instancePlayer.sendChatToPlayer(
            ChatMessageComponent
                .createFromTranslationWithSubstitutions("shop.money.show", Money.format(balance))
                .setColor(net.minecraft.EnumChatFormatting.YELLOW)
        );
    }

    private static int toTenths(double d) {
        return (int) Math.round(d * 10.0);
    }

    public static int getBalanceTenths(EntityPlayer player) {
        ensureInitialized();
        Integer bal = BALANCES.get(player);
        if (bal != null) return bal;
        return getBalanceTenths(PlayerIdentityUtil.getOfflineUUID(player.username));
    }

    public static int getBalanceTenths(UUID uuid) {
        ensureInitialized();
        return BALANCES_BY_UUID.getOrDefault(uuid, 0);
    }

    public static void setBalanceTenths(EntityPlayer player, int v) {
        ensureInitialized();
        BALANCES.put(player, v);
    }

    public static void setBalanceTenths(UUID uuid, int v) {
        ensureInitialized();
        BALANCES_BY_UUID.put(uuid, v);
        saveBalancesToFile();
    }

    public static void addTenths(EntityPlayer player, int delta) {
        ensureInitialized();
        int newBal = getBalanceTenths(player) + delta;
        BALANCES.put(player, newBal);
        setBalanceTenths(PlayerIdentityUtil.getOfflineUUID(player.username), newBal);
    }

    public static void addTenths(UUID uuid, int delta) {
        ensureInitialized();
        int newBalance = getBalanceTenths(uuid) + delta;
        setBalanceTenths(uuid, newBalance);
        for (EntityPlayer player : BALANCES.keySet()) {
            if (PlayerIdentityUtil.getOfflineUUID(player.username).equals(uuid))
                BALANCES.put(player, newBalance);
        }
    }

    public static void saveBalancesToFile() {
        ensureInitialized();
        if (!SHOP_DIR.exists()) SHOP_DIR.mkdirs();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(BALANCES_FILE))) {
            out.writeObject(BALANCES_BY_UUID);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    public static void loadBalancesFromFile() {
        ensureInitialized();
        if (!BALANCES_FILE.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(BALANCES_FILE))) {
            Map<UUID, Integer> loaded = (Map<UUID, Integer>) in.readObject();
            BALANCES_BY_UUID.clear();
            BALANCES_BY_UUID.putAll(loaded);
        } catch (Exception ignored) {}
    }

    public static void clearStatic() {
        BALANCES.clear();
        BALANCES_BY_UUID.clear();
        SHOP_DIR = null;
        BALANCES_FILE = null;
        initialized = false;
    }

}