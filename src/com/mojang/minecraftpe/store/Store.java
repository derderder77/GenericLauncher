package com.mojang.minecraftpe.store;

import android.util.Log;

public class Store
{
  private StoreListener listener;
  
  public Store(StoreListener paramStoreListener)
  {
    this.listener = paramStoreListener;
  }

  public void acknowledgePurchase(String str1, String str2)
  {
    Log.i("GenericLauncher","Store: Acknowledge " + " " + str1 + " " + str2);
  }

  public void destructor()
  {
    Log.i("GenericLauncher","Store: Destructor");
  }
  
  public String getStoreId()
  {
    Log.i("GenericLauncher","Store: Get store ID");
    return "Placeholder store ID";
  }
  
  public void purchase(String str1, boolean bool, String str2)
  {
    Log.i("GenericLauncher","Store: Purchase " + str1 + " " + bool + " "+str2);
  }
  
  public void queryProducts(String[] paramArrayOfString)
  {
    Log.i("GenericLauncher","Store: Query products");
  }
  
  public void queryPurchases()
  {
    Log.i("GenericLauncher","Store: Query purchases");
  }
}

