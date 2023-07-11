package kr.gbsv.inventorysaveticket;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class InventorySaveTicket extends JavaPlugin implements Listener {
    private HashMap<UUID, ItemStack[]> savedInventories;
    private LuckPerms luckPerms;
    private ContextManager contextManager;

    @Override
    public void onEnable() {
        System.out.println("인벤 세이브 플러그인 [ver.1.1] 시작중. . .");
        savedInventories = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, this);
        setupLuckPerms();
    }

    private void setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            contextManager = luckPerms.getContextManager();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("giveinvensave")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }

            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage("이 명령어를 사용할 권한이 없습니다.");
                return true;
            }

            if (args.length == 0) {
                player.sendMessage("발급할 대상 플레이어의 이름을 입력해주세요.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("대상 플레이어를 찾을 수 없습니다.");
                return true;
            }

            savedInventories.put(target.getUniqueId(), target.getInventory().getContents());
            player.sendMessage(target.getName() + "님에게 인벤토리 세이브 권한이 발급되었습니다.");

            // LuckPerms를 통해 권한 설정
            if (luckPerms != null && contextManager != null) {
                User user = luckPerms.getUserManager().getUser(target.getUniqueId());
                if (user != null) {
                    Node keepInventoryNode = Node.builder("keepinventory").value(true).build();
                    user.data().remove(keepInventoryNode);
                    luckPerms.getUserManager().saveUser(user);
                }
            }

            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (savedInventories.containsKey(player.getUniqueId())) {
            ItemStack[] savedInventory = savedInventories.get(player.getUniqueId());
            player.getInventory().setContents(savedInventory);
            savedInventories.remove(player.getUniqueId());

            // LuckPerms를 통해 권한 제거
            if (luckPerms != null && contextManager != null) {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    Node keepInventoryNode = Node.builder("keepinventory").value(true).build();
                    user.data().add(keepInventoryNode);
                    luckPerms.getUserManager().saveUser(user);
                }
            }
        }
    }
}
