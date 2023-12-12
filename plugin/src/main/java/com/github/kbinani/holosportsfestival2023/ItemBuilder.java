package com.github.kbinani.holosportsfestival2023;

import lombok.experimental.ExtensionMethod;
import net.kyori.adventure.text.Component;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.UUID;
import java.util.function.Consumer;

@ExtensionMethod({ItemStackExtension.class})
public class ItemBuilder {
  private final ItemStack item;

  public static ItemBuilder For(Material material) {
    return new ItemBuilder(material);
  }

  private ItemBuilder(Material material) {
    item = new ItemStack(material, 1);
  }

  public ItemBuilder amount(int amount) {
    item.setAmount(amount);
    return this;
  }

  public ItemBuilder displayName(Component name) {
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.displayName(name);
      item.setItemMeta(meta);
    }
    return this;
  }

  public ItemBuilder customTag(String name) {
    item.addCustomTag(name);
    return this;
  }

  public ItemBuilder potion(PotionType type) {
    item.editMeta(PotionMeta.class, it -> {
      it.setBasePotionType(type);
    });
    return this;
  }

  public ItemBuilder firework(FireworkEffect effect) {
    item.editMeta(FireworkMeta.class, it -> {
      it.addEffect(effect);
    });
    return this;
  }

  public ItemBuilder enchant(Enchantment ench, int level) {
    item.addUnsafeEnchantment(ench, level);
    return this;
  }

  public ItemBuilder attributeModifier(Attribute attribute, String name, double amount, AttributeModifier.Operation op) {
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.addAttributeModifier(attribute, new AttributeModifier(UUID.randomUUID(), name, amount, op));
      item.setItemMeta(meta);
    }
    return this;
  }

  public ItemBuilder flags(ItemFlag... flags) {
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.addItemFlags(flags);
      item.setItemMeta(meta);
    }
    return this;
  }

  public <M extends ItemMeta> ItemBuilder meta(Class<M> metaClass, Consumer<? super M> consumer) {
    item.editMeta(metaClass, consumer);
    return this;
  }

  public ItemStack build() {
    return this.item.clone();
  }
}
