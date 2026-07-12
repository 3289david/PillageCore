package com.mingyu.pillage.shop;

import org.bukkit.Material;

public record ShopOffer(int id, Material inputMaterial, int inputAmount, Material outputMaterial, int outputAmount) {
}
