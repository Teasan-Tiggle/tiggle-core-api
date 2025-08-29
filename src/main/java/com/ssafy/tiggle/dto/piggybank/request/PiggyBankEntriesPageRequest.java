package com.ssafy.tiggle.dto.piggybank.request;
import lombok.Data;
@Data
public class PiggyBankEntriesPageRequest {
    private String type;   // "TIGGLE" or "DUTCHPAY"
    private String cursor; // Base64(JSON)
    private Integer size;  // default 20, max 100
    private String from;   // yyyy-MM-dd (optional)
    private String to;     // yyyy-MM-dd (optional)
    private String sortKey;// 사용 안하면 null
}
