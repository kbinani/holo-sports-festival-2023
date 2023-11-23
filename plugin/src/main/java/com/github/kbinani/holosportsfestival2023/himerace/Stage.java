package com.github.kbinani.holosportsfestival2023.himerace;

enum Stage {
  CARRY("Stage.1 向こうの足場までお姫様を運んであげましょう！"),
  BUILD("Stage.2 騎士達が作っているものを答えよう！"),
  COOK("Stage.3 姫が食べたい物をプレゼントしてあげよう！"),
  SOLVE("Stage.4 姫と一緒に問題に答えよう！"),
  FIGHT("Stage.5 姫と一緒にモンスターを倒そう！"),
  GOAL("Stage.5 姫と一緒にモンスターを倒そう！");

  final String description;

  Stage(String description) {
    this.description = description;
  }
}
