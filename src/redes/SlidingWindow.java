package redes;

import java.util.HashMap;

public class SlidingWindow {
    private long size;
    private long ptr;
    private HashMap<Long, Boolean> packs;

    SlidingWindow(int size) {
        this.size = size;
        this.ptr = 0;  // MUDAR PRA 1 CASO SEQNUM COMECE POR 1
        this.packs = new HashMap<>();
    }

    // Insere pacote na janela
    void insert(long seq) {
        this.packs.put(seq, false);
    }

    // Retorna se o pacote pode ser enviado
    public boolean insideWindow(long seq) throws InterruptedException {
        Thread.sleep(0,1); // Sleep essencial
        return seq >= this.ptr && seq < this.size+this.ptr;
    }

    // Atualiza a janela com pacotes que ja foram confirmados
    public void update(long seq) {
        packs.put(seq, true);
        while(packs.containsKey(ptr) && packs.get(ptr)) {
            ptr++;
            if(ptr >= packs.size()) {
                break;
            }
        }
    }

    public HashMap<Long, Boolean> getPacks() {
        return packs;
    }

    public void print() {
        HashMap<Long, Boolean> pc = this.packs;
        for(long i = 0; i< pc.size(); i++) { // MUDAR PRA 1 CASO SEQNUM COMECE POR 1
            System.out.print(i+":"+pc.get(i)+" ");
        }
        System.out.println();

    }
}
