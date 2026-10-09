package com.sysshop.core;

import java.util.ArrayList;
import java.util.List;

/** Wire and persistence DTOs. Offers intentionally store registry IDs/counts, not item NBT. */
public final class ShopData {
    public static final int STORAGE_SLOTS = 54;
    public static final int MAX_OFFERS = 16;
    public static final int MAX_NAME_LENGTH = 32;

    private ShopData() { }

    public static final class ItemRef {
        public String id = "";
        public int count;

        public ItemRef() { }
        public ItemRef(String id, int count) { this.id = id == null ? "" : id; this.count = count; }
        public boolean isEmpty() { return id == null || id.isEmpty() || count < 1; }
        public ItemRef copy() { return new ItemRef(id, count); }
    }

    public static final class Offer {
        public ItemRef[] input = new ItemRef[] { new ItemRef(), new ItemRef() };
        public ItemRef[] output = new ItemRef[] { new ItemRef(), new ItemRef() };
        /** -1 means unlimited (system store) or not yet calculated. */
        public int available = -1;
        public Offer() { }
        public void normalize() {
            input = normalizePair(input);
            output = normalizePair(output);
        }
        private static ItemRef[] normalizePair(ItemRef[] values) {
            ItemRef[] result = new ItemRef[] { new ItemRef(), new ItemRef() };
            if (values != null) for (int i = 0; i < Math.min(2, values.length); i++)
                if (values[i] != null) result[i] = values[i];
            return result;
        }
    }

    public static final class Shop {
        public String id = "";
        public String name = "";
        public String ownerUuid = "";
        public String ownerName = "";
        public boolean system;
        public List<Offer> offers = new ArrayList<>();
        public List<ItemRef> stock = new ArrayList<>();

        public Shop() { }
        public void normalize(boolean includeStock) {
            if (offers == null) offers = new ArrayList<>();
            for (Offer offer : offers) if (offer != null) offer.normalize();
            if (stock == null) stock = new ArrayList<>();
            if (includeStock) {
                while (stock.size() < STORAGE_SLOTS) stock.add(new ItemRef());
                while (stock.size() > STORAGE_SLOTS) stock.remove(stock.size() - 1);
                for (int i = 0; i < stock.size(); i++) if (stock.get(i) == null) stock.set(i, new ItemRef());
            } else {
                stock.clear();
            }
        }

        public Shop view(boolean includeStock) {
            Shop copy = new Shop();
            copy.id = id;
            copy.name = name;
            copy.ownerUuid = ownerUuid;
            copy.ownerName = ownerName;
            copy.system = system;
            if (offers != null) for (Offer offer : offers) {
                if (offer == null) continue;
                Offer next = new Offer();
                next.available = offer.available;
                for (int i = 0; i < 2; i++) {
                    next.input[i] = offer.input[i] == null ? new ItemRef() : offer.input[i].copy();
                    next.output[i] = offer.output[i] == null ? new ItemRef() : offer.output[i].copy();
                }
                copy.offers.add(next);
            }
            if (includeStock && stock != null) for (ItemRef item : stock)
                copy.stock.add(item == null ? new ItemRef() : item.copy());
            copy.normalize(includeStock);
            return copy;
        }
    }

    public static final class Snapshot {
        public String mode = "browse";
        public String selectedId = "";
        public String notice = "";
        public List<Shop> shops = new ArrayList<>();
        public Shop selected;
        public Snapshot() { }
    }
}
