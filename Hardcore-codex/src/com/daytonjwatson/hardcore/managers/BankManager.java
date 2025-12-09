package com.daytonjwatson.hardcore.managers;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.daytonjwatson.hardcore.config.ConfigValues;

public class BankManager {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d HH:mm")
            .withZone(ZoneId.systemDefault());

    private static BankManager instance;

    private final JavaPlugin plugin;
    private final File dataFile;
    private final FileConfiguration config;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> transactions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingCustomSends = new ConcurrentHashMap<>();

    private final double startingBalance;
    private final int maxTransactions;

    private BankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "bank.yml");
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        this.startingBalance = ConfigValues.bankStartingBalance();
        this.maxTransactions = ConfigValues.bankMaxTransactions();

        load();
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new BankManager(plugin);
        }
    }

    public static BankManager get() {
        return instance;
    }

    private void load() {
        if (config.isConfigurationSection("accounts")) {
            for (String key : config.getConfigurationSection("accounts").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double balance = config.getDouble("accounts." + key + ".balance", startingBalance);
                    balances.put(uuid, balance);

                    List<String> history = config.getStringList("accounts." + key + ".transactions");
                    if (!history.isEmpty()) {
                        Deque<String> log = new LinkedList<>(history);
                        transactions.put(uuid, log);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        for (UUID uuid : balances.keySet()) {
            String base = "accounts." + uuid + ".";
            config.set(base + "balance", balances.get(uuid));
            config.set(base + "transactions", new ArrayList<>(getTransactions(uuid)));
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save bank data: " + e.getMessage());
        }
    }

    public void ensureAccount(UUID uuid) {
        if (balances.containsKey(uuid)) {
            return;
        }

        balances.put(uuid, startingBalance);
        addTransaction(uuid, "Account created with starting balance " + formatCurrency(startingBalance));
        save();
    }

    public double getBalance(UUID uuid) {
        ensureAccount(uuid);
        return balances.getOrDefault(uuid, startingBalance);
    }

    public List<String> getTransactions(UUID uuid) {
        ensureAccount(uuid);
        Deque<String> log = transactions.get(uuid);
        if (log == null) {
            return Collections.emptyList();
        }
        return List.copyOf(log);
    }

    public boolean deposit(UUID uuid, double amount, String reason) {
        if (amount <= 0) return false;
        ensureAccount(uuid);
        balances.merge(uuid, amount, Double::sum);
        addTransaction(uuid, "+" + formatCurrency(amount) + " — " + reason);
        save();
        return true;
    }

    public boolean withdraw(UUID uuid, double amount, String reason) {
        if (amount <= 0) return false;
        ensureAccount(uuid);
        double current = balances.getOrDefault(uuid, 0.0);
        if (current < amount) return false;

        balances.put(uuid, current - amount);
        addTransaction(uuid, "-" + formatCurrency(amount) + " — " + reason);
        save();
        return true;
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return false;
        ensureAccount(from);
        ensureAccount(to);

        double senderBalance = balances.getOrDefault(from, 0.0);
        if (senderBalance < amount) {
            return false;
        }

        balances.put(from, senderBalance - amount);
        balances.merge(to, amount, Double::sum);

        OfflinePlayer sender = Bukkit.getOfflinePlayer(from);
        OfflinePlayer recipient = Bukkit.getOfflinePlayer(to);

        String recipientName = recipient.getName() != null ? recipient.getName() : "Unknown";
        String senderName = sender.getName() != null ? sender.getName() : "Unknown";

        String outgoing = "Sent " + formatCurrency(amount) + " to " + recipientName;
        String incoming = "Received " + formatCurrency(amount) + " from " + senderName;

        addTransaction(from, "-" + formatCurrency(amount) + " — " + outgoing);
        addTransaction(to, "+" + formatCurrency(amount) + " — " + incoming);

        save();
        return true;
    }

    private void addTransaction(UUID uuid, String description) {
        Deque<String> log = transactions.computeIfAbsent(uuid, __ -> new LinkedList<>());
        String timestamp = TIME_FORMAT.format(Instant.now());
        String entry = "[" + timestamp + "] " + description;
        log.addFirst(entry);
        while (log.size() > maxTransactions) {
            log.removeLast();
        }
    }

    public String formatCurrency(double amount) {
        return ConfigValues.bankCurrencySymbol() + DECIMAL_FORMAT.format(amount);
    }

    public void setPendingCustomSend(UUID sender, UUID target) {
        pendingCustomSends.put(sender, target);
    }

    public UUID getPendingCustomSend(UUID sender) {
        return pendingCustomSends.get(sender);
    }

    public void clearPendingCustomSend(UUID sender) {
        pendingCustomSends.remove(sender);
    }

    public boolean hasPendingCustomSend(UUID sender) {
        return pendingCustomSends.containsKey(sender);
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(balances.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        if (limit > 0 && entries.size() > limit) {
            return new ArrayList<>(entries.subList(0, limit));
        }
        return entries;
    }
}
