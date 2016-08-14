package com.mojang.minecraftpe.store;

public class Purchase
{
  public String mProductId;
  public boolean mPurchaseActive;
  public String mReceipt;
  
  public Purchase(String productId, String receipt, boolean active)
  {
    this.mProductId = productId;
    this.mReceipt = receipt;
    this.mPurchaseActive = active;
  }
}

