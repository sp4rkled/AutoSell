package sp4rkled.autosell;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;

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

        int centerX = this.width / 2;
        int fieldWidth = 220;

        priceField = new TextFieldWidget(
                this.textRenderer,
                centerX - fieldWidth / 2,
                this.height / 2 - 12,
                fieldWidth,
                20,
                Text.literal("Price")
        );
        priceField.setMaxLength(12);
        priceField.setText("1000");
        priceField.setPlaceholder(Text.literal("Enter price"));
        priceField.setFocused(true);
        this.addDrawableChild(priceField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start AutoSell"), button -> submit())
                .dimensions(centerX - 105, this.height / 2 + 18, 210, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(centerX - 105, this.height / 2 + 43, 210, 20)
                .build());
    }

    private void submit() {
        String value = priceField.getText().trim();
        if (value.isEmpty()) {
            return;
        }

        onSubmit.accept(client, value);
        if (AutoSellClient.isEnabled()) {
            close();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Enter the /sell price"),
                this.width / 2,
                this.height / 2 - 42,
                0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("AutoSell repeats every second"),
                this.width / 2,
                this.height / 2 - 62,
                0xAAAAAA
        );
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(null);
    }
}
