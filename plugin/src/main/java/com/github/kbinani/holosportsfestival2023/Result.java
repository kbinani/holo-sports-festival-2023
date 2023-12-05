package com.github.kbinani.holosportsfestival2023;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

public class Result<T, Reason> {
  public final T value;
  public final Reason reason;

  public Result(T value, Reason reason) {
    this.value = value;
    this.reason = reason;
  }

  public void use(Consumer<T> cb) {
    if (value != null) {
      cb.accept(value);
    }
  }

  public <R> @Nullable R use(Function<T, R> cb) {
    if (value == null) {
      return null;
    } else {
      return cb.apply(value);
    }
  }
}
