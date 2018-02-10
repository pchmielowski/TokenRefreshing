package net.chmielowski.token;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import okhttp3.Request;
import okhttp3.Response;

class Flow {

    List<Event> events = new LinkedList<>();

    private final int id = new Random().nextInt();
    private final int color = MainActivity.generateColor();

    Flow() {
        Recorder.INSTANCE.add(this);
    }

    void onRequest(Request request) {
        events.add(new Event(request));
    }

    void onResponse(Request request, Response response) {
        events.add(new Event(request, response));
    }

    void onRetryRequest(Request request) {
        events.add(new Event(request));
    }

    void onRetryResponse(Request request, Response response) {
        events.add(new Event(request, response));
    }

    public void onRefreshTriggered() {
        // TODO
    }

    class Event {
        final long time;
        @Nullable
        Integer code;
        final String type;
        final String url;
        final String token;

        Event(Request request) {
            time = System.currentTimeMillis();
            type = "request";
            url = request.url().toString();
            token = request.header("Authorization");
        }

        Event(Request request, Response response) {
            time = System.currentTimeMillis();
            type = "response";
            url = request.url().toString();
            token = request.header("Authorization");
            code = response.code();
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            switch (type) {
                case "request":
                    return String.format("Phone -[%s]> Server: %s %s", color(), endpoint(), token);
                case "response":
                    return String.format("Server -[%s]-> Phone: %s %d", color(), endpoint(), code);
                default:
                    throw new RuntimeException();
            }
        }

        private String color() {
            return "#" + Integer.toHexString(color - 0xFF000000);
        }

        private String endpoint() {
            return url.split("5000/")[1];
        }
    }

    @Override
    public String toString() {
        return events.stream()
                .map(Event::toString)
                .collect(Collectors.joining("\n"));
    }
}


/*
Participant Phone
Participant Network
Participant Server

Phone -[#red]> Server
Phone -[#green]> Server

Server -[#red]-> Phone: token-expired
note left: Triggers refresh
Server -[#green]-> Network: token-expired
Phone -[#red]> Server: refresh

Server -[#red]-> Phone: refresh
note left: Triggers retry

Network -[#green]-> Phone: token-expired
note left: Also triggers refresh token

Phone -[#red]> Server
Server -[#red]-> Phone:
 */
