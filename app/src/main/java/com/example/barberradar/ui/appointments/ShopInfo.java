package com.example.barberradar.ui.appointments;

/**
 * Simple class to store shop information
 */
public class ShopInfo {
    private final String shopId;
    private final String ownerId;
    
    public ShopInfo(String shopId, String ownerId) {
        this.shopId = shopId;
        this.ownerId = ownerId;
    }
    
    public String getShopId() {
        return shopId;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
}
