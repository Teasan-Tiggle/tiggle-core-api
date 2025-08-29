package com.ssafy.tiggle.dto.piggybank.response;

import lombok.AllArgsConstructor; import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PiggyBankEntriesPageResponse {
    private List<PiggyBankEntryItemDto> items;
    private String nextCursor; // 더 없으면 null
    private int size;
    private boolean hasNext;
}
