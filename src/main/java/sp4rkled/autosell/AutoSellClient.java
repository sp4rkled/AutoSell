package sp4rkled.autosell;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AutoSellClient implements ClientModInitializer {
    private static final int SELL_INTERVAL_TICKS = 20;
    private static final int PEARL_INTERVAL_TICKS = 20;
    private static final int FIRST_HOTBAR_SCREEN_SLOT = 36;

    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(
            Identifier.of("autosell", "autosell")
    );

    private static boolean enabled;
    private static int price;
    private static int sellTicks;
    private static int pearlTicks;

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autosell.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (enabled) {
                    disable(client, true);
                } else {
                    openPricePrompt(client);
                }
            }

            if (!enabled || client.player == null || client.getNetworkHandler() == null) {
                return;
            }

            if (++sellTicks >= SELL_INTERVAL_TICKS) {
                sellTicks = 0;
                client.getNetworkHandler().sendChatCommand("sell " + price);
            }

            if (++pearlTicks >= PEARL_INTERVAL_TICKS) {
                pearlTicks = 0;
                ensurePearlInFirstHotbarSlot(client);
            }
        });
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("autoenablenow")
                .executes(context -> {
                    openPricePrompt(MinecraftClient.getInstance());
                    return 1;
                }));

        dispatcher.register(ClientCommandManager.literal("autodisablenow")
                .executes(context -> {
                    disable(MinecraftClient.getInstance(), true);
                    return 1;
                }));
    }

    private static void openPricePrompt(MinecraftClient client) {
        if (enabled) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("AutoSell is already running."), false);
            }
            return;
        }

        client.setScreen(new PriceScreen(client, AutoSellClient::enableWithPrice));
    }

    private static void enableWithPrice(MinecraftClient client, String rawPrice) {
        try {
            int parsed = Integer.parseInt(rawPrice.trim());
            if (parsed < 0) {
                throw new NumberFormatException();
            }

            price = parsed;
            enabled = true;
            sellTicks = SELL_INTERVAL_TICKS - 1;
            pearlTicks = PEARL_INTERVAL_TICKS - 1;

            if (client.player != null) {
                client.player.sendMessage(Text.literal("AutoSell enabled: /sell " + price), false);
            }
        } catch (NumberFormatException ignored) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Invalid price. Enter a whole number such as 1000."), false);
            }
        }
    }

    static boolean isEnabled() {
        return enabled;
    }

    private static void disable(MinecraftClient client, boolean notify) {
        boolean wasEnabled = enabled;
        enabled = false;
        sellTicks = 0;
        pearlTicks = 0;

        if (notify && wasEnabled && client.player != null) {
            client.player.sendMessage(Text.literal("AutoSell disabled."), false);
        }
    }

    private static void ensurePearlInFirstHotbarSlot(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        // Fixed slot numbers below are valid only for the player's own inventory handler.
        // Do not send a SWAP while a chest, shop, crafting table, etc. is open.
        if (!(client.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            return;
        }

        ItemStack firstHotbar = client.player.getInventory().getStack(0);
        if (firstHotbar.isOf(Items.ENDER_PEARL)) {
            return;
        }

        int sourceInventoryIndex = findPearl(client);
        if (sourceInventoryIndex < 0) {
            return;
        }

        int screenSlot = inventoryIndexToScreenSlot(sourceInventoryIndex);
        if (screenSlot < 0) {
            return;
        }

        client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                screenSlot,
                0,
                SlotActionType.SWAP,
                client.player
        );
    }

    private static int findPearl(MinecraftClient client) {
        for (int i = 9; i < 36; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                return i;
            }
        }

        for (int i = 1; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                return i;
            }
        }

        return -1;
    }

    private static int inventoryIndexToScreenSlot(int inventoryIndex) {
        if (inventoryIndex >= 9 && inventoryIndex < 36) {
            return inventoryIndex;
        }
        if (inventoryIndex >= 0 && inventoryIndex < 9) {
            return FIRST_HOTBAR_SCREEN_SLOT + inventoryIndex;
        }
        return -1;
    }
}
