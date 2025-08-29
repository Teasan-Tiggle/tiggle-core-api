package com.ssafy.tiggle.constants;

public class FinancialApiEndpoints {
    
    // Base paths
    public static final String MEMBER_BASE = "/member";
    public static final String DEMAND_DEPOSIT_BASE = "/edu/demandDeposit";
    public static final String ACCOUNT_AUTH_BASE = "/edu/accountAuth";
    
    // Member endpoints
    public static final String CREATE_USER = ""; // 사용자 계정 생성
    public static final String SEARCH_USER = "/search"; // 사용자 조회
    
    // Demand Deposit endpoints
    public static final String CREATE_DEMAND_DEPOSIT_ACCOUNT = "/createDemandDepositAccount"; // 계좌 생성
    public static final String INQUIRE_DEMAND_DEPOSIT_ACCOUNT_LIST = "/inquireDemandDepositAccountList"; // 계좌 목록 조회
    public static final String INQUIRE_DEMAND_DEPOSIT_ACCOUNT_BALANCE = "/inquireDemandDepositAccountBalance"; // 계좌 잔액 조회
    public static final String INQUIRE_DEMAND_DEPOSIT_ACCOUNT = "/inquireDemandDepositAccount"; // 계좌 조회(단건)
    public static final String UPDATE_DEMAND_DEPOSIT_ACCOUNT_DEPOSIT = "/updateDemandDepositAccountDeposit"; // 계좌 입금
    public static final String UPDATE_DEMAND_DEPOSIT_ACCOUNT_WITHDRAWAL = "/updateDemandDepositAccountWithdrawal"; // 계좌 출금
    public static final String UPDATE_DEMAND_DEPOSIT_ACCOUNT_TRANSFER = "/updateDemandDepositAccountTransfer"; // 계좌 이체
    public static final String INQUIRE_TRANSACTION_HISTORY_LIST = "/inquireTransactionHistoryList"; // 계좌 거래 내역 조회
    public static final String INQUIRE_TRANSACTION_HISTORY = "/inquireTransactionHistory"; // 계좌 거래 내역 조회(단건)
    public static final String DELETE_DEMAND_DEPOSIT_ACCOUNT = "/deleteDemandDepositAccount"; // 계좌 해지
    public static final String INQUIRE_DEMAND_DEPOSIT_ACCOUNT_HOLDER_NAME = "/inquireDemandDepositAccountHolderName"; // 예금주 조회
    
    // Account Auth endpoints
    public static final String OPEN_ACCOUNT_AUTH = "/openAccountAuth"; // 1원 송금(계좌 인증)
    public static final String CHECK_AUTH_CODE = "/checkAuthCode"; // 1원 송금 인증 검증
}