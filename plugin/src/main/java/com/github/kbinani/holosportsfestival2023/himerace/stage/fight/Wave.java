package com.github.kbinani.holosportsfestival2023.himerace.stage.fight;

import javax.annotation.Nullable;

enum Wave {
  Wave1("hololive_sports_festival_2023_himerace_fight_wave1"),
  Wave2("hololive_sports_festival_2023_himerace_fight_wave2"),
  Wave3("hololive_sports_festival_2023_himerace_fight_wave3");

  final String tag;

  Wave(String tag) {
    this.tag = tag;
  }

  @Nullable
  Wave next() {
    return switch (this) {
      case Wave1 -> Wave2;
      case Wave2 -> Wave3;
      case Wave3 -> null;
    };
  }
}
