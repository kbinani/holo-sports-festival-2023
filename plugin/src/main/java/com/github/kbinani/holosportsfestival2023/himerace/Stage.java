package com.github.kbinani.holosportsfestival2023.himerace;

public enum Stage {
  CARRY("Stage.1 向こうの足場までお姫様を運んであげましょう！", "hololive_sports_festival_2023_himerace_carry_stage"),
  BUILD("Stage.2 騎士達が作っているものを答えよう！", "hololive_sports_festival_2023_himerace_build_stage"),
  COOK("Stage.3 姫が食べたい物をプレゼントしてあげよう！", "hololive_sports_festival_2023_himerace_cook_stage"),
  SOLVE("Stage.4 姫と一緒に問題に答えよう！", "hololive_sports_festival_2023_himerace_solve_stage"),
  FIGHT("Stage.5 姫と一緒にモンスターを倒そう！", "hololive_sports_festival_2023_himerace_fight_stage"),
  GOAL("Stage.5 姫と一緒にモンスターを倒そう！", "hololive_sports_festival_2023_himerace_goal_stage");

  public final String description;
  public final String tag;

  Stage(String description, String tag) {
    this.description = description;
    this.tag = tag;
  }
}
