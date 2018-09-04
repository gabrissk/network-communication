package redes;

import java.util.ArrayList;

public class SlidingWindow {
    private int size;
    private ArrayList<Integer> packs;

    public SlidingWindow(int size) {
        this.size = size;
        packs = new ArrayList<>(size);
    }
}
