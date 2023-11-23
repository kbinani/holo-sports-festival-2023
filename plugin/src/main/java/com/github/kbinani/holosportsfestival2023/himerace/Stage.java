package com.github.kbinani.holosportsfestival2023.himerace;

enum Stage {
  CARRY("Stage.1 向こうの足場までお姫様を運んであげましょう！", 0, 0.19f),
  BUILD("Stage.2 騎士達が作っているものを答えよう！", 0.19f, 0.19f),
  COOK("Stage.3 姫が食べたい物をプレゼントしてあげよう！", 0.19f * 2, 0.19f),
  SOLVE("Stage.4 姫と一緒に問題に答えよう！", 0.19f * 3, 0.19f),
  FIGHT("Stage.5 姫と一緒にモンスターを倒そう！", 0.19f * 4, 0.19f),
  GOAL("Stage.5 姫と一緒にモンスターを倒そう！", 0.19f * 5, 0.05f);

  final String description;
  final float progressOffset;
  final float progressWeight;

  Stage(String description, float progressOffset, float progressWeight) {
    this.description = description;
    this.progressOffset = progressOffset;
    this.progressWeight = progressWeight;
  }
}
