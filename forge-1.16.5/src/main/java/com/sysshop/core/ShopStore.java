package com.sysshop.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Server-authoritative, per-world shop data and barter transactions. */
public final class ShopStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "sysshop-shops.json";
    private static final String SYSTEM_ID = "system";

    private ShopStore() { }

    public static void handle(ServerPlayerEntity player, String payload) {
        String mode = "browse";
        try {
            JsonObject request = new JsonParser().parse(payload).getAsJsonObject();
            String action = string(request, "action", "");
            mode = action.equals("manager") || action.equals("create") ? "manager"
                    : action.equals("restock") || action.equals("withdraw") ? "restock" : "browse";
            switch (action) {
                case "open": sendBrowse(player, ""); break;
                case "manager": sendManager(player, string(request, "shop", ""), ""); break;
                case "detail": sendDetail(player, string(request, "shop", ""), ""); break;
                case "create": createShop(player, request); break;
                case "set_offer": setOffer(player, request); break;
                case "add_offer": addOffer(player, request); break;
                case "delete_offer": deleteOffer(player, request); break;
                case "restock_view": sendRestock(player, string(request, "shop", ""), ""); break;
                case "restock": restock(player, request); break;
                case "withdraw": withdraw(player, request); break;
                case "purchase": purchase(player, request); break;
                default: sendError(player, mode, "未知的商店操作。");
            }
        } catch (RuntimeException ex) {
            SysShopMod.LOGGER.error("Rejected malformed or invalid shop request", ex);
            sendError(player, mode, "请求无效或商店数据不可读；未执行操作。请检查本世界 data/sysshop-shops.json。");
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (RuntimeException ex) { return fallback; }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(FolderName.ROOT).resolve("data").resolve(FILE_NAME);
    }

    private static List<ShopData.Shop> load(MinecraftServer server) {
        Path path = file(server);
        if (!Files.exists(path)) return new ArrayList<>();
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            ShopData.Shop[] loaded = GSON.fromJson(json, ShopData.Shop[].class);
            List<ShopData.Shop> shops = new ArrayList<>();
            if (loaded != null) for (ShopData.Shop shop : loaded) {
                if (shop == null || shop.id == null || shop.id.isEmpty()) continue;
                shop.normalize(true);
                if (shop.offers.size() > ShopData.MAX_OFFERS)
                    shop.offers = new ArrayList<>(shop.offers.subList(0, ShopData.MAX_OFFERS));
                shops.add(shop);
            }
            return shops;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read shop file", ex);
        }
    }

    private static boolean save(MinecraftServer server, List<ShopData.Shop> shops) {
        Path path = file(server);
        Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.write(temp, GSON.toJson(shops).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ex) {
            SysShopMod.LOGGER.error("Could not save world shop file", ex);
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            return false;
        }
    }

    private static boolean multiplayer(MinecraftServer server) {
        return server.isDedicatedServer() || server.isPublished();
    }

    private static boolean isAdmin(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        return server != null && server.getPlayerList().isOp(player.getGameProfile());
    }

    private static void ensureSystemShop(MinecraftServer server, List<ShopData.Shop> shops) {
        if (!multiplayer(server)) return;
        for (ShopData.Shop shop : shops) if (shop.system || SYSTEM_ID.equals(shop.id)) {
            shop.id = SYSTEM_ID;
            shop.system = true;
            shop.name = "系统商店";
            shop.ownerName = "系统";
            shops.remove(shop);
            shops.add(0, shop);
            return;
        }
        ShopData.Shop system = new ShopData.Shop();
        system.id = SYSTEM_ID;
        system.name = "系统商店";
        system.ownerName = "系统";
        system.system = true;
        for (TradeStore.Offer legacy : TradeStore.load()) {
            ShopData.Offer offer = new ShopData.Offer();
            offer.input[0] = new ShopData.ItemRef(legacy.inputId, legacy.inputCount);
            offer.output[0] = new ShopData.ItemRef(legacy.outputId, legacy.outputCount);
            system.offers.add(offer);
            if (system.offers.size() == ShopData.MAX_OFFERS) break;
        }
        system.normalize(true);
        shops.add(0, system);
        save(server, shops);
    }

    private static ShopData.Shop find(List<ShopData.Shop> shops, String id) {
        for (ShopData.Shop shop : shops) if (shop.id.equals(id)) return shop;
        return null;
    }

    private static boolean canManage(ServerPlayerEntity player, ShopData.Shop shop, MinecraftServer server) {
        if (shop.system) return multiplayer(server) && isAdmin(player);
        return shop.ownerUuid.equals(player.getUUID().toString());
    }

    private static List<ShopData.Shop> visibleShops(ServerPlayerEntity player, List<ShopData.Shop> shops) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> result = new ArrayList<>();
        for (ShopData.Shop shop : shops) {
            if (shop.system && !multiplayer(server)) continue;
            result.add(shop);
        }
        result.sort(Comparator.comparing((ShopData.Shop s) -> !s.system)
                .thenComparing(s -> s.name == null ? "" : s.name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static ShopData.Shop publicView(ShopData.Shop source) {
        ShopData.Shop view = source.view(false);
        for (int i = 0; i < view.offers.size(); i++)
            view.offers.get(i).available = source.system ? -1 : availableTransactions(source, source.offers.get(i));
        return view;
    }

    private static ShopData.Shop summaryView(ShopData.Shop source) {
        ShopData.Shop view = publicView(source);
        view.offers.clear();
        return view;
    }

    private static int availableTransactions(ShopData.Shop shop, ShopData.Offer offer) {
        int available = Integer.MAX_VALUE;
        boolean hasOutput = false;
        for (int i = 0; i < offer.output.length; i++) {
            ShopData.ItemRef ref = offer.output[i];
            if (ref == null || ref.isEmpty()) continue;
            hasOutput = true;
            boolean counted = false;
            for (int prior = 0; prior < i; prior++)
                if (offer.output[prior] != null && ref.id.equals(offer.output[prior].id)) counted = true;
            if (counted) continue;
            int required = 0;
            for (ShopData.ItemRef item : offer.output)
                if (item != null && ref.id.equals(item.id) && !item.isEmpty()) required += item.count;
            available = Math.min(available, stockCount(shop, ref.id) / Math.max(1, required));
        }
        return hasOutput ? available : 0;
    }

    private static void sendBrowse(ServerPlayerEntity player, String notice) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        ShopData.Snapshot snapshot = new ShopData.Snapshot();
        snapshot.mode = "browse";
        snapshot.notice = notice;
        for (ShopData.Shop shop : visibleShops(player, shops)) snapshot.shops.add(summaryView(shop));
        send(player, snapshot);
    }

    private static void sendDetail(ServerPlayerEntity player, String id, String notice) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        ShopData.Shop selected = find(visibleShops(player, shops), id);
        if (selected == null) { sendError(player, "browse", "找不到该商店。"); return; }
        ShopData.Snapshot snapshot = new ShopData.Snapshot();
        snapshot.mode = "browse";
        snapshot.selectedId = selected.id;
        snapshot.notice = notice;
        for (ShopData.Shop shop : visibleShops(player, shops)) snapshot.shops.add(summaryView(shop));
        snapshot.selected = publicView(selected);
        send(player, snapshot);
    }

    private static void sendManager(ServerPlayerEntity player, String selectedId, String notice) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        ShopData.Snapshot snapshot = new ShopData.Snapshot();
        snapshot.mode = "manager";
        snapshot.notice = notice;
        for (ShopData.Shop shop : visibleShops(player, shops))
            if (canManage(player, shop, server)) snapshot.shops.add(summaryView(shop));
        ShopData.Shop selected = find(shops, selectedId);
        if (selected == null || !canManage(player, selected, server))
            selected = snapshot.shops.isEmpty() ? null : find(shops, snapshot.shops.get(0).id);
        if (selected != null) {
            snapshot.selectedId = selected.id;
            snapshot.selected = publicView(selected);
        }
        send(player, snapshot);
    }

    private static void sendRestock(ServerPlayerEntity player, String id, String notice) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        ShopData.Shop selected = find(shops, id);
        if (selected == null || !canManage(player, selected, server)) {
            sendError(player, "manager", "没有管理该商店的权限。");
            return;
        }
        if (selected.system) {
            sendManager(player, id, "系统商店无限供货，不需要补货。");
            return;
        }
        ShopData.Snapshot snapshot = new ShopData.Snapshot();
        snapshot.mode = "restock";
        snapshot.notice = notice;
        snapshot.selectedId = selected.id;
        for (ShopData.Shop shop : visibleShops(player, shops))
            if (canManage(player, shop, server)) snapshot.shops.add(summaryView(shop));
        snapshot.selected = selected.view(true);
        send(player, snapshot);
    }

    private static void createShop(ServerPlayerEntity player, JsonObject request) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        String name = string(request, "name", "").trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > ShopData.MAX_NAME_LENGTH || name.matches(".*[\\p{Cntrl}].*")) {
            sendError(player, "create", "店名不能为空，且不能超过 32 个字符或包含控制字符。");
            return;
        }
        ShopData.Shop shop = new ShopData.Shop();
        shop.id = UUID.randomUUID().toString();
        shop.name = name;
        shop.ownerUuid = player.getUUID().toString();
        shop.ownerName = player.getGameProfile().getName();
        String ownerLabel = string(request, "ownerName", "").trim().replaceAll("\\s+", " ");
        if (isAdmin(player) && !ownerLabel.isEmpty() && ownerLabel.length() <= ShopData.MAX_NAME_LENGTH
                && !ownerLabel.matches(".*[\\p{Cntrl}].*")) shop.ownerName = ownerLabel;
        shop.offers.add(new ShopData.Offer());
        shop.normalize(true);
        shops.add(shop);
        if (!save(server, shops)) { sendError(player, "create", "保存失败，商店未创建。"); return; }
        sendManager(player, shop.id, "商店已创建。右键背包物品可复制到交易槽，不会扣除物品。");
    }

    private static void setOffer(ServerPlayerEntity player, JsonObject request) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        String id = string(request, "shop", "");
        ShopData.Shop shop = find(shops, id);
        int row = integer(request, "row", -1);
        int slot = integer(request, "slot", -1);
        String side = string(request, "side", "");
        String itemId = string(request, "item", "");
        int count = integer(request, "count", 0);
        if (shop == null || !canManage(player, shop, server)) { sendError(player, "manager", "没有管理该商店的权限。"); return; }
        if (row < 0 || row >= shop.offers.size() || slot < 0 || slot > 1
                || (!"input".equals(side) && !"output".equals(side))) {
            sendManager(player, id, "交易槽位置无效。"); return;
        }
        ShopData.ItemRef replacement = new ShopData.ItemRef();
        if (!itemId.isEmpty()) {
            if (count < 1 || count > 64 || resolve(itemId) == null) {
                sendManager(player, id, "物品或数量无效；仅保存物品 ID 和数量，不保存附魔/NBT。"); return;
            }
            replacement = new ShopData.ItemRef(itemId, count);
        }
        ShopData.Offer offer = shop.offers.get(row);
        if ("input".equals(side)) offer.input[slot] = replacement;
        else offer.output[slot] = replacement;
        if (!save(server, shops)) { sendError(player, "manager", "保存失败；交易未更新。"); return; }
        sendManager(player, id, "交易槽已更新；背包物品未扣除。");
    }

    private static void addOffer(ServerPlayerEntity player, JsonObject request) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        String id = string(request, "shop", "");
        ShopData.Shop shop = find(shops, id);
        if (shop == null || !canManage(player, shop, server)) { sendError(player, "manager", "没有管理该商店的权限。"); return; }
        if (shop.offers.size() >= ShopData.MAX_OFFERS) { sendManager(player, id, "每个商店最多配置 16 条交易。"); return; }
        shop.offers.add(new ShopData.Offer());
        if (!save(server, shops)) { sendError(player, "manager", "保存失败；未添加交易行。"); return; }
        sendManager(player, id, "已添加一条交易行。");
    }

    private static void deleteOffer(ServerPlayerEntity player, JsonObject request) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        String id = string(request, "shop", "");
        ShopData.Shop shop = find(shops, id);
        int row = integer(request, "row", -1);
        if (shop == null || !canManage(player, shop, server)) { sendError(player, "manager", "没有管理该商店的权限。"); return; }
        if (row < 0 || row >= shop.offers.size()) { sendManager(player, id, "交易行位置无效。"); return; }
        shop.offers.remove(row);
        if (!save(server, shops)) { sendError(player, "manager", "保存失败；交易行未删除。"); return; }
        sendManager(player, id, "交易行已删除。");
    }

    private static void restock(ServerPlayerEntity player, JsonObject request) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        String id = string(request, "shop", "");
        ShopData.Shop shop = find(shops, id);
        int source = integer(request, "source", -1);
        int target = integer(request, "target", -1);
        if (shop == null || shop.system || !canManage(player, shop, server)) { sendError(player, "restock", "没有该玩家商店的补货权限。"); return; }
        if (source < 0 || source >= 36 || target < 0 || target >= ShopData.STORAGE_SLOTS) { sendRestock(player, id, "背包或仓库槽位无效。"); return; }
        ItemStack sourceStack = player.inventory.getItem(source);
        if (sourceStack.isEmpty() || sourceStack.getTag() != null) { sendRestock(player, id, "请选择普通物品；带附魔或自定义 NBT 的物品暂不支持。"); return; }
        String itemId = itemId(sourceStack);
        if (itemId.isEmpty()) { sendRestock(player, id, "无法识别该物品。"); return; }
        shop.normalize(true);
        ShopData.ItemRef targetStack = shop.stock.get(target);
        if (!targetStack.isEmpty() && !targetStack.id.equals(itemId)) { sendRestock(player, id, "目标格已有其他物品，请先取出。"); return; }
        int maxStack = sourceStack.getMaxStackSize();
        int capacity = maxStack - (targetStack.isEmpty() ? 0 : targetStack.count);
        int moved = Math.min(Math.min(sourceStack.getCount(), capacity), integer(request, "count", sourceStack.getCount()));
        if (moved < 1) { sendRestock(player, id, "该仓库槽已满。"); return; }
        if (targetStack.isEmpty()) shop.stock.set(target, new ShopData.ItemRef(itemId, moved));
        else targetStack.count += moved;
        sourceStack.shrink(moved);
        if (sourceStack.isEmpty()) player.inventory.setItem(source, ItemStack.EMPTY);
        if (!save(server, shops)) {
            sourceStack = player.inventory.getItem(source);
            if (sourceStack.isEmpty()) player.inventory.setItem(source, new ItemStack(resolve(itemId), moved));
            else sourceStack.grow(moved);
            sendRestock(player, id, "保存失败；已取消本次补货。");
            return;
        }
        player.inventory.setChanged();
        player.containerMenu.broadcastChanges();
        sendRestock(player, id, "已补入 " + moved + " 个物品。");
    }

    private static void withdraw(ServerPlayerEntity player, JsonObject request) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        String id = string(request, "shop", "");
        ShopData.Shop shop = find(shops, id);
        int source = integer(request, "source", -1);
        if (shop == null || shop.system || !canManage(player, shop, server)) { sendError(player, "restock", "没有该玩家商店的取货权限。"); return; }
        if (source < 0 || source >= ShopData.STORAGE_SLOTS) { sendRestock(player, id, "仓库槽位无效。"); return; }
        shop.normalize(true);
        ShopData.ItemRef stored = shop.stock.get(source);
        if (stored.isEmpty()) { sendRestock(player, id, "该仓库槽为空。"); return; }
        Item item = resolve(stored.id);
        if (item == null) { sendRestock(player, id, "仓库中的物品 ID 无效。"); return; }
        List<ItemStack> planned = inventoryCopy(player);
        int remaining = insert(planned, item, stored.count);
        int moved = stored.count - remaining;
        if (moved < 1) { sendRestock(player, id, "背包没有空位。"); return; }
        stored.count -= moved;
        if (stored.count == 0) shop.stock.set(source, new ShopData.ItemRef());
        if (!save(server, shops)) { sendRestock(player, id, "保存失败；未取出物品。"); return; }
        applyInventory(player, planned);
        sendRestock(player, id, "已从仓库取出 " + moved + " 个物品。");
    }

    private static void purchase(ServerPlayerEntity player, JsonObject request) {
        MinecraftServer server = player.getServer();
        List<ShopData.Shop> shops = load(server);
        ensureSystemShop(server, shops);
        String id = string(request, "shop", "");
        ShopData.Shop shop = find(visibleShops(player, shops), id);
        int row = integer(request, "row", -1);
        if (shop == null || row < 0 || row >= shop.offers.size()) { sendError(player, "browse", "商店或交易不存在。"); return; }
        ShopData.Offer offer = shop.offers.get(row);
        offer.normalize();
        boolean hasInput = false, hasOutput = false;
        for (ShopData.ItemRef ref : offer.input) if (ref != null && !ref.isEmpty()) hasInput = true;
        for (ShopData.ItemRef ref : offer.output) if (ref != null && !ref.isEmpty()) hasOutput = true;
        if (!hasInput || !hasOutput) { sendDetail(player, id, "该交易尚未配置完整。"); return; }

        List<ItemStack> planned = inventoryCopy(player);
        for (ShopData.ItemRef ref : offer.input) {
            if (ref == null || ref.isEmpty()) continue;
            Item item = resolve(ref.id);
            if (item == null || !remove(planned, item, ref.count)) { sendDetail(player, id, "支付物品不足；交易未扣除。"); return; }
        }

        List<ShopData.ItemRef> plannedStock = copyStock(shop.stock);
        if (!shop.system) {
            for (ShopData.ItemRef ref : offer.output) {
                if (ref == null || ref.isEmpty()) continue;
                if (!removeStock(plannedStock, ref.id, ref.count)) { sendDetail(player, id, "商店库存不足；交易未扣除。"); return; }
            }
        }
        for (ShopData.ItemRef ref : offer.output) {
            if (ref == null || ref.isEmpty()) continue;
            Item item = resolve(ref.id);
            if (item == null || insert(planned, item, ref.count) > 0) { sendDetail(player, id, "背包放不下兑换物；交易未扣除。"); return; }
        }
        if (!shop.system) {
            for (ShopData.ItemRef ref : offer.input) {
                if (ref == null || ref.isEmpty()) continue;
                if (!addStock(plannedStock, ref.id, ref.count)) { sendDetail(player, id, "店铺 54 格仓库空间不足；整笔交易未扣除、未发货。"); return; }
            }
            shop.stock = plannedStock;
            if (!save(server, shops)) { sendDetail(player, id, "保存失败；交易未执行。"); return; }
        }
        applyInventory(player, planned);
        sendDetail(player, id, "兑换成功。");
    }

    private static List<ItemStack> inventoryCopy(ServerPlayerEntity player) {
        List<ItemStack> copy = new ArrayList<>(36);
        for (int slot = 0; slot < 36; slot++) copy.add(player.inventory.getItem(slot).copy());
        return copy;
    }

    private static void applyInventory(ServerPlayerEntity player, List<ItemStack> planned) {
        for (int slot = 0; slot < planned.size(); slot++) player.inventory.setItem(slot, planned.get(slot));
        player.inventory.setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static boolean remove(List<ItemStack> inventory, Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.get(slot);
            if (!stack.isEmpty() && stack.getItem() == item && stack.getTag() == null) {
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
                if (stack.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            }
        }
        return remaining == 0;
    }

    private static int insert(List<ItemStack> inventory, Item item, int count) {
        int remaining = count;
        for (ItemStack stack : inventory) {
            if (remaining == 0) return 0;
            if (!stack.isEmpty() && stack.getItem() == item && stack.getTag() == null && stack.getCount() < stack.getMaxStackSize()) {
                int moved = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved);
                remaining -= moved;
            }
        }
        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            if (inventory.get(slot).isEmpty()) {
                int moved = Math.min(remaining, new ItemStack(item).getMaxStackSize());
                inventory.set(slot, new ItemStack(item, moved));
                remaining -= moved;
            }
        }
        return remaining;
    }

    private static List<ShopData.ItemRef> copyStock(List<ShopData.ItemRef> stock) {
        List<ShopData.ItemRef> result = new ArrayList<>(ShopData.STORAGE_SLOTS);
        for (int i = 0; i < ShopData.STORAGE_SLOTS; i++) {
            ShopData.ItemRef ref = stock != null && i < stock.size() && stock.get(i) != null ? stock.get(i) : new ShopData.ItemRef();
            result.add(ref.copy());
        }
        return result;
    }

    private static int stockCount(ShopData.Shop shop, String id) {
        int count = 0;
        if (shop.stock != null) for (ShopData.ItemRef ref : shop.stock)
            if (ref != null && !ref.isEmpty() && ref.id.equals(id)) count += ref.count;
        return count;
    }

    private static boolean removeStock(List<ShopData.ItemRef> stock, String id, int count) {
        int remaining = count;
        for (int i = 0; i < stock.size() && remaining > 0; i++) {
            ShopData.ItemRef ref = stock.get(i);
            if (!ref.isEmpty() && ref.id.equals(id)) {
                int taken = Math.min(remaining, ref.count);
                ref.count -= taken;
                remaining -= taken;
                if (ref.count == 0) stock.set(i, new ShopData.ItemRef());
            }
        }
        return remaining == 0;
    }

    private static boolean addStock(List<ShopData.ItemRef> stock, String id, int count) {
        Item item = resolve(id);
        if (item == null || count < 1) return false;
        int maxStack = new ItemStack(item).getMaxStackSize();
        int remaining = count;
        for (ShopData.ItemRef ref : stock) {
            if (remaining == 0) return true;
            if (!ref.isEmpty() && ref.id.equals(id) && ref.count < maxStack) {
                int moved = Math.min(remaining, maxStack - ref.count);
                ref.count += moved;
                remaining -= moved;
            }
        }
        for (int i = 0; i < stock.size() && remaining > 0; i++) {
            if (stock.get(i).isEmpty()) {
                int moved = Math.min(remaining, maxStack);
                stock.set(i, new ShopData.ItemRef(id, moved));
                remaining -= moved;
            }
        }
        return remaining == 0;
    }

    private static Item resolve(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return null;
        Item item = ForgeRegistries.ITEMS.getValue(key);
        return item == null || item == net.minecraft.item.Items.AIR ? null : item;
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static void sendError(ServerPlayerEntity player, String mode, String message) {
        ShopData.Snapshot snapshot = new ShopData.Snapshot();
        snapshot.mode = mode;
        snapshot.notice = message;
        send(player, snapshot);
    }

    private static void send(ServerPlayerEntity player, ShopData.Snapshot snapshot) {
        ShopNetwork.sendTo(player, GSON.toJson(snapshot));
    }
}
