package com.example.tiggle.dto.piggy.response;

import lombok.AllArgsConstructor; import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PiggyEntriesPageResponse {
    private List<PiggyEntryItemDto> items;
    private String nextCursor; // 더 없으면 null
    private int size;
    private boolean hasNext;
}
