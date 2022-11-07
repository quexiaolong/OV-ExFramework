package com.vivo.services.popupcamera;

import java.util.LinkedList;

/* loaded from: classes.dex */
public class LimitQueue<E> {
    private int limit;
    private LinkedList<E> queue = new LinkedList<>();

    public LimitQueue(int limit) {
        this.limit = limit;
    }

    public void offer(E e) {
        if (this.queue.size() >= this.limit) {
            this.queue.poll();
        }
        this.queue.offer(e);
    }

    public E poll() {
        return this.queue.poll();
    }

    public E get(int position) {
        return this.queue.get(position);
    }

    public E getLast() {
        return this.queue.getLast();
    }

    public E getFirst() {
        return this.queue.getFirst();
    }

    public int getLimit() {
        return this.limit;
    }

    public E removeFirst() {
        return this.queue.removeFirst();
    }

    public E removeLast() {
        return this.queue.removeLast();
    }

    public int size() {
        return this.queue.size();
    }

    public LinkedList<E> getQueue() {
        return this.queue;
    }
}