package net.vorium.currencies;

import com.henryfabio.sqlprovider.connector.SQLConnector;
import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import lombok.Getter;
import me.lucko.helper.Schedulers;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import net.vorium.currencies.command.MoneyCommand;
import net.vorium.currencies.command.subcommands.*;
import net.vorium.currencies.entities.Account;
import net.vorium.currencies.entities.services.AccountServices;
import net.vorium.currencies.entities.services.RankingServices;
import net.vorium.currencies.integrations.VaultIntegration;
import net.vorium.currencies.listeners.PlayerListener;
import net.vorium.currencies.storarge.DatabaseFactory;
import net.vorium.currencies.storarge.dao.AccountRepo;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

@Getter
public class Main extends JavaPlugin {

    private static Main instance;
    private LuckPerms luckPerms;

    private SQLConnector connector;
    private AccountRepo repository;

    private AccountServices accountServices;
    private RankingServices rankingServices;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        setupDatabase();

        accountServices = new AccountServices(this);
        rankingServices = new RankingServices(this);
        rankingServices.load();

        setupEconomy();

        setupSyncTask();
        setupLuckPerms();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        setupCommands();
    }

    @Override
    public void onDisable() {
        for (Account account : accountServices.getAccounts()) {
            if (account.isToSync()) repository.insertOne(account);
        }

        rankingServices.unload();
    }

    public static Main getInstance() {
        return instance;
    }

    public void setupEconomy() {
        getServer().getServicesManager().register(
                Economy.class,
                new VaultIntegration(this),
                this,
                ServicePriority.Highest);
    }

    public void setupDatabase() {
        connector = DatabaseFactory.createConnection(getConfig().getConfigurationSection("mysql"));
        repository = new AccountRepo(this);
        repository.createTable();
    }

    public void setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        luckPerms = provider.getProvider();
    }

    public void setupCommands() {
        CommandService service = Drink.get(this);
        service.register(new MoneyCommand(this), "money", "coins", "coin")
                .registerSub(new TopCommand(this))
                .registerSub(new PayCommand(this))
                .registerSub(new AddCommand(this))
                .registerSub(new RemoveCommand(this))
                .registerSub(new SetCommand(this));
        service.registerCommands();
    }

    public void setupSyncTask() {
        Schedulers.sync().runRepeating(() -> {
            for (Account account : accountServices.getAccounts()) {
                if (account.isToSync()) {
                    repository.update(account);
                    Bukkit.getConsoleSender().sendMessage("Currencies sync completed!");
                }
            }

            rankingServices.update();
        }, 0L, 300L);
    }
}
