package ru.plextime.core;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.level.Level;
import cn.nukkit.level.generator.Flat;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.level.generator.Nether;
import cn.nukkit.level.generator.Normal;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main extends PluginBase implements Listener {

    private static final int FORM_MW_MAIN = 9001;
    private static final int FORM_MW_CREATE = 9002;
    private static final int FORM_MW_TP = 9003;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        File worldsDir = new File(this.getServer().getDataPath() + "worlds/");
        if (worldsDir.exists() && worldsDir.isDirectory()) {
            File[] files = worldsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && !this.getServer().isLevelLoaded(file.getName())) {
                        this.getServer().loadLevel(file.getName());
                    }
                }
            }
        }
        this.getLogger().info("§a[PlexTime] NewPlugin v1.0 (UI MultiWorld) запущен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mw")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Команда доступна только в игре.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("plextime.owner") && !player.isOp()) {
                player.sendMessage("§cНет прав.");
                return true;
            }
            openMainMenu(player);
            return true;
        }
        return false;
    }

    private void openMainMenu(Player player) {
        FormWindowSimple form = new FormWindowSimple("§5§lPlexTime §r§8| §fМиры", "Выберите действие:");
        form.addButton(new ElementButton("§a+ Создать новый мир"));
        form.addButton(new ElementButton("§bТелепортироваться в мир"));
        form.addButton(new ElementButton("§eУстановить спавн здесь"));
        player.showFormWindow(form, FORM_MW_MAIN);
    }

    private void openCreateMenu(Player player) {
        FormWindowCustom form = new FormWindowCustom("§5§lСоздание мира");
        form.addElement(new ElementInput("Название мира (на английском):", "например: spawn"));
        form.addElement(new ElementDropdown("Тип генерации:", Arrays.asList(
                "Пустой мир (Void - для спавна)",
                "Обычный мир (Normal)",
                "Плоский мир (Flat)",
                "Незер (Nether)"
        )));
        player.showFormWindow(form, FORM_MW_CREATE);
    }

    private void openTeleportMenu(Player player) {
        FormWindowSimple form = new FormWindowSimple("§5§lСписок миров", "Нажмите на мир для телепортации:");
        for (Level level : this.getServer().getLevels().values()) {
            form.addButton(new ElementButton("§f" + level.getFolderName()));
        }
        player.showFormWindow(form, FORM_MW_TP);
    }

    @EventHandler
    public void onFormResponse(PlayerFormRespondedEvent event) {
        if (event.wasClosed() || event.getResponse() == null) return;
        Player player = event.getPlayer();
        int id = event.getFormID();

        if (id == FORM_MW_MAIN) {
            FormResponseSimple res = (FormResponseSimple) event.getResponse();
            int btn = res.getClickedButtonId();
            if (btn == 0) {
                openCreateMenu(player);
            } else if (btn == 1) {
                openTeleportMenu(player);
            } else if (btn == 2) {
                player.getLevel().setSpawnLocation(player.getPosition());
                player.sendMessage("§a[PlexTime] Точка спавна мира §e" + player.getLevel().getFolderName() + " §aустановлена!");
            }
        } else if (id == FORM_MW_CREATE) {
            FormResponseCustom res = (FormResponseCustom) event.getResponse();
            String worldName = res.getInputResponse(0).trim();
            int typeIndex = res.getDropdownResponse(1).getElementID();

            if (worldName.isEmpty()) {
                player.sendMessage("§cВведите название мира!");
                return;
            }
            if (this.getServer().isLevelGenerated(worldName)) {
                player.sendMessage("§cМир с таким названием уже существует!");
                return;
            }

            player.sendMessage("§eГенерация мира §f" + worldName + "§e, подождите...");
            Map<String, Object> options = new HashMap<>();
            Class<? extends Generator> generator = Normal.class;

            if (typeIndex == 0) {
                generator = Flat.class;
                options.put("preset", "2;0;1;");
            } else if (typeIndex == 2) {
                generator = Flat.class;
            } else if (typeIndex == 3) {
                generator = Nether.class;
            }

            this.getServer().generateLevel(worldName, System.currentTimeMillis(), generator, options);
            Level level = this.getServer().getLevelByName(worldName);
            if (level != null) {
                if (typeIndex == 0) {
                    level.setBlock(new Vector3(0, 64, 0), Block.get(BlockID.BEDROCK));
                    level.setSpawnLocation(new Vector3(0.5, 65, 0.5));
                }
                player.teleport(level.getSafeSpawn());
                player.sendMessage("§a[PlexTime] Мир §f" + worldName + " §aуспешно создан и вы телепортированы!");
            }
        } else if (id == FORM_MW_TP) {
            FormResponseSimple res = (FormResponseSimple) event.getResponse();
            String worldName = res.getClickedButton().getText().replace("§f", "");
            Level level = this.getServer().getLevelByName(worldName);
            if (level != null) {
                player.teleport(level.getSafeSpawn());
                player.sendMessage("§aВы перемещены в мир §f" + worldName);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLevel().getFolderName().equalsIgnoreCase("spawn") && player.getY() < 0) {
            player.teleport(player.getLevel().getSafeSpawn());
            player.sendMessage("§d[PlexTime] Вы вернулись на спавн.");
        }
    }
}
