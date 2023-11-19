package com.github.kbinani.holosportsfestival2023.kibasen;

import com.github.kbinani.holosportsfestival2023.TeamColor;
import com.github.kbinani.holosportsfestival2023.Teams;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Session {
  private final Teams teams;
  private final Map<TeamColor, ArrayList<Unit>> participants;

  Session(@Nonnull Teams teams, Map<TeamColor, ArrayList<Unit>> participants) {
    this.teams = teams;
    this.participants = new HashMap<>(participants);
  }

  void abort() {
    //TODO:
  }

  void timeout() {

  }
}
