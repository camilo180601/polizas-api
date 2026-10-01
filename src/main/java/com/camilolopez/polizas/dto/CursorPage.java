package com.camilolopez.polizas.dto;

import java.util.List;

public record CursorPage<T>(List<T> items, Long nextCursor) {
    public CursorPage {
        items = List.copyOf(items);
    }

    public boolean hasMore() {
        return nextCursor != null;
    }
}
