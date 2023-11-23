package com.github.kbinani.holosportsfestival2023;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

public class WeakReference<T> {
  private @Nonnull java.lang.ref.WeakReference<T> ref;

  public WeakReference(@Nullable T v) {
    ref = new java.lang.ref.WeakReference<>(v);
  }

  public WeakReference() {
    ref = new java.lang.ref.WeakReference<>(null);
  }

  public @Nullable T get() {
    return ref.get();
  }

  public @Nullable T set(@Nullable T next) {
    var obj = ref.get();
    ref = new java.lang.ref.WeakReference<>(next);
    return obj;
  }

  public void clear() {
    ref = new java.lang.ref.WeakReference<>(null);
  }

  public void use(@Nonnull Consumer<T> consumer) {
    var obj = ref.get();
    if (obj != null) {
      consumer.accept(obj);
    }
  }

  public <R> @Nullable R use(@Nonnull Function<T, R> consumer) {
    var obj = ref.get();
    if (obj == null) {
      return null;
    }
    return consumer.apply(obj);
  }
}
