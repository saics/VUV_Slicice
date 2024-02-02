package com.example.vuv_slicice.models;

public class Trade {
    private String tradeId;
    private String offeredCardId;
    private String requestedCardId;
    private String offeringUserId;
    private String acceptingUserId;
    private boolean isAccepted;

    // Default constructor required for calls to DataSnapshot.getValue(Trade.class)
    public Trade() {
    }

    public Trade(String tradeId, String offeredCardId, String requestedCardId, String offeringUserId, String acceptingUserId, boolean isAccepted) {
        this.tradeId = tradeId;
        this.offeredCardId = offeredCardId;
        this.requestedCardId = requestedCardId;
        this.offeringUserId = offeringUserId;
        this.acceptingUserId = acceptingUserId;
        this.isAccepted = isAccepted;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getOfferedCardId() {
        return offeredCardId;
    }

    public void setOfferedCardId(String offeredCardId) {
        this.offeredCardId = offeredCardId;
    }

    public String getRequestedCardId() {
        return requestedCardId;
    }

    public void setRequestedCardId(String requestedCardId) {
        this.requestedCardId = requestedCardId;
    }

    public String getOfferingUserId() {
        return offeringUserId;
    }

    public void setOfferingUserId(String offeringUserId) {
        this.offeringUserId = offeringUserId;
    }

    public String getAcceptingUserId() {
        return acceptingUserId;
    }

    public void setAcceptingUserId(String acceptingUserId) {
        this.acceptingUserId = acceptingUserId;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean accepted) {
        isAccepted = accepted;
    }
}

