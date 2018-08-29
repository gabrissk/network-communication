package redes;

import java.io.Serializable;

public class Timestamp implements Serializable {

    private long secs;
    private int nanos;

    public Timestamp(long secs, int nanos) {
        this.secs = secs;
        this.nanos = nanos;
    }

    public long getSecs() {
        return secs;
    }

    public int getNanos() {
        return nanos;
    }
}
