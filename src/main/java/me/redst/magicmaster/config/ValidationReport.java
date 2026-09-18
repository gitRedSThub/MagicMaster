package me.redst.magicmaster.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationReport {

    private final List<String> problems = new ArrayList<>();

    void reject(String path, String reason) {
        problems.add(path + " (" + reason + ")");
    }

    public int count() {
        return problems.size();
    }

    public List<String> problems() {
        return Collections.unmodifiableList(problems);
    }
}
