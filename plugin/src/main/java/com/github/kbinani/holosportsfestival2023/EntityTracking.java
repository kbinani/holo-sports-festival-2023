package com.github.kbinani.holosportsfestival2023;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import javax.annotation.Nonnull;

public class EntityTracking<T extends Entity> {
  private @Nonnull T entity;

  public EntityTracking(@Nonnull T entity) {
    this.entity = entity;
  }

  public @Nonnull T get() {
    if (!entity.isValid()) {
      var id = entity.getUniqueId();
      var next = Bukkit.getServer().getEntity(id);
      if (entity.getClass().isInstance(next)) {
        entity = (T) next;
      }
    }
    return entity;
  }
}
