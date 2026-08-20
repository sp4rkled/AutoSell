package sp4rkled.autosell;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;

/**
 * Invisible price input screen.
 * Type the price directly and press Enter; no popup is rendered.
 */
final class PriceScreen extends Screen {
    private final MinecraftClient client;
    private final BiConsumer<MinecraftClient, String> onSubmit;
    private TextFieldWidget priceField;

    PriceScreen(MinecraftClient client, BiConsumer<MinecraftClient, String> onSubmit) {
        super(Text.literal("AutoSell Price"));
        this.client = client;
        this.onSubmit = onSubmit;
    }

    @Override
    protected void init() {
        super.init();

        // The field is tiny and off-screen. It captures keyboard input without
        // drawing a visible GUI over the game.
        priceField = new TextFieldWidget(
                this.textRenderer,
                -1000,
                -1000,
                1,
                1,
                Text.literal("Price")
        ) {
            @Override
            public boolean keyPressed(KeyInput input) {
                if (input.isEnter()) {
                    submit();
                    return true;
                }
                return super.keyPressed(input);
            }
        };

        priceField.setMaxLength(12);
        priceField.setTextPredicate(value -> value.isEmpty()
                || value.chars().allMatch(Character::isDigit));
        priceField.setText("");
        priceField.setFocused(true);
        this.addDrawableChild(priceField);
        this.setFocused(priceField);
    }

    private void submit() {
        String value = priceField.getText().trim();
        if (value.isEmpty()) {
            return;
        }

        onSubmit.accept(client, value);
        if (AutoSellClient.isEnabled()) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            client.setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally draw nothing: keyboard-only UX.
    }

    @Override
    public void close() {
        client.setScreen(null);
    }
}
