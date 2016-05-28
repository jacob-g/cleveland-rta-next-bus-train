package org.futuresight.clevelandrtanextbustrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jacob on 5/27/16.
 */
public class MapType {
    public static final List<Item> ITEMS = new ArrayList<Item>();
    public static final Map<String, Item> ITEM_MAP = new HashMap<String, Item>();

    static {
        // Add some sample items.
        addItem(new Item("rail", "Rail", "This is the rail map", R.drawable.railmap));
    }

    private static void addItem(Item item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    public static class Item {
        public final String id;
        public final String content;
        public final String details;
        public final int imageId;


        public Item(String id, String content, String details, int imageId) {
            this.id = id;
            this.content = content;
            this.details = details;
            this.imageId = imageId;
        }
    }
}
