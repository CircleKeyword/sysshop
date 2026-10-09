package com.sysshop.core;

import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Legacy config seed reader used to initialize the first multiplayer system shop. */
public final class TradeStore {
    private static final String FILE_NAME = "sysshop-offers.txt";
    private static final String[] DEFAULTS = {
            "minecraft:iron_ingot|8|minecraft:emerald|1",
            "minecraft:coal|16|minecraft:iron_ingot|4",
            "minecraft:oak_log|8|minecraft:bread|4"
    };

    private TradeStore() { }

    public static final class Offer {
        public final String inputId;
        public final int inputCount;
        public final String outputId;
        public final int outputCount;

        public Offer(String inputId, int inputCount, String outputId, int outputCount) {
            this.inputId = inputId;
            this.inputCount = inputCount;
            this.outputId = outputId;
            this.outputCount = outputCount;
        }
    }

    private static Path file() { return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME); }

    private static void ensureFile() throws IOException {
        Path path = file();
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.write(path, Arrays.asList(DEFAULTS), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
    }

    public static List<Offer> load() {
        Map<String, Offer> offers = new LinkedHashMap<>();
        try {
            ensureFile();
            for (String line : Files.readAllLines(file(), StandardCharsets.UTF_8)) {
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                String[] parts = value.split("\\|", -1);
                if (parts.length != 4) continue;
                try {
                    int inputCount = Integer.parseInt(parts[1]);
                    int outputCount = Integer.parseInt(parts[3]);
                    if (inputCount < 1 || inputCount > 64 || outputCount < 1 || outputCount > 64) continue;
                    if (resolve(parts[0]) == null || resolve(parts[2]) == null) continue;
                    offers.put(parts[0], new Offer(parts[0], inputCount, parts[2], outputCount));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed rows but keep loading valid offers.
                }
            }
        } catch (IOException ex) {
            SysShopMod.LOGGER.error("Could not read SysShop offer file", ex);
        }
        return new ArrayList<>(offers.values());
    }

    public static boolean save(List<Offer> offers) {
        List<String> lines = new ArrayList<>();
        lines.add("# SysShop offers: input_item|input_count|output_item|output_count");
        for (Offer offer : offers) lines.add(offer.inputId + "|" + offer.inputCount + "|" + offer.outputId + "|" + offer.outputCount);
        try {
            Files.createDirectories(file().getParent());
            Files.write(file(), lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return true;
        } catch (IOException ex) {
            SysShopMod.LOGGER.error("Could not save SysShop offer file", ex);
            return false;
        }
    }

    public static Item resolve(String id) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            return item == null || item == net.minecraft.item.Items.AIR ? null : item;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static Offer find(String inputId, int inputCount, String outputId, int outputCount) {
        for (Offer offer : load()) if (offer.inputId.equals(inputId) && offer.inputCount == inputCount
                && offer.outputId.equals(outputId) && offer.outputCount == outputCount) return offer;
        return null;
    }

    public static boolean put(Offer replacement) {
        List<Offer> offers = load();
        offers.removeIf(offer -> offer.inputId.equals(replacement.inputId));
        offers.add(replacement);
        return save(offers);
    }

    public static boolean remove(String inputId) {
        List<Offer> offers = load();
        boolean removed = offers.removeIf(offer -> offer.inputId.equals(inputId));
        return removed && save(offers);
    }
}
