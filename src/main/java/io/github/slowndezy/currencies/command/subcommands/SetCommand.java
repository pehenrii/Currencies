package io.github.slowndezy.currencies.command.subcommands;

import io.github.slowndezy.currencies.entities.Account;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import io.github.slowndezy.currencies.CurrenciesPlugin;
import io.github.slowndezy.currencies.command.MoneyCommand;
import io.github.slowndezy.currencies.events.MoneyUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCommand extends MoneyCommand {

    public SetCommand(CurrenciesPlugin plugin) {
        super(plugin);
    }

    @Command(name = "money.set", aliases = { "setar", "definir" }, permission = "currencies.admin")
    public void Command(Context<CommandSender> sender, Player target, int amount) {
        Account targetAccount = services.findByName(target.getName());

        if (targetAccount == null) {
            sender.sendMessage("§cNão foi possível encontrar este jogador.");
            return;
        }

        if (amount <= 0) {
            sender.sendMessage("§cInsira um número válido.");
            return;
        }

        MoneyUpdateEvent moneyUpdateEvent = new MoneyUpdateEvent(target, MoneyUpdateEvent.UpdateType.SET, amount);
        Bukkit.getPluginManager().callEvent(moneyUpdateEvent);

        if (!moneyUpdateEvent.isCancelled()) {
            targetAccount.set(amount);
            sender.sendMessage("§eVocê definiu §a" + format.format(amount) + " §ecoins para " + targetAccount.getName());
        }
    }
}