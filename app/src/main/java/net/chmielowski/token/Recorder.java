package net.chmielowski.token;

import android.util.Log;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.stream.Collectors;

enum Recorder {
    INSTANCE;
    private LinkedList<Flow> flows = new LinkedList<>();

    public void add(Flow flow) {
        flows.add(flow);
    }

    public void printGroupingByFlow() {
        Log.d("pchm", flows.stream()
                .map(Flow::toString)
                .collect(Collectors.joining("\n")));
    }

    public void printSortedByTime() {
        final String collect = flows.stream()
                .flatMap(flow -> flow.events.stream())
                .sorted(Comparator.comparingLong(o -> o.time))
                .map(Flow.Event::toString)
                .collect(Collectors.joining("\n"));
        Log.d("pchm", collect);
    }
}
