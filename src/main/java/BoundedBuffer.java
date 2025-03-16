import java.util.Arrays;

/**
 * BoundedBuffer.java
 *
 * @author D.L. Bailey,
 * Systems and Computer Engineering,
 * Carleton University
 * @version 1.2, January 23, 2002
 */

public class BoundedBuffer
{
    // A simple ring buffer is used to hold the data

    // buffer capacity
    public static final int SIZE = 100;
    private Object[] buffer = new Object[SIZE];
    private int inIndex = 0, outIndex = 0, count = 0;

    // If true, there is room for at least one object in the buffer.
    private boolean writeable = true;

    // If true, there is at least one object stored in the buffer.
    private boolean readable = false;

    public synchronized void addLast(Object item)
    {
        while (!writeable) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        buffer[inIndex] = item;
        readable = true;

        inIndex = (inIndex + 1) % SIZE;
        count++;
        if (count == SIZE)
            writeable = false;

        notifyAll();
    }

    public synchronized Object removeFirst()
    {
        Object item;

        while (!readable) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        item = buffer[outIndex];
        writeable = true;

        outIndex = (outIndex + 1) % SIZE;
        count--;
        if (count == 0)
            readable = false;

        notifyAll();

        return item;
    }

    public synchronized int getCount()
    {
        return this.count;
    }

    public String toString(){
        return Arrays.toString(buffer);
    }

    public synchronized Object removeFireEventByID(int fireID) {
        while (true) {
            while (!readable) {
                try {
                    wait(); // Wait if buffer is empty
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null; // Exit if interrupted
                }
            }

            // Search for the FireEvent
            for (int i = 0; i < buffer.length; i++) {
                if (buffer[i] instanceof Integer) { // Check if it's an Integer
                    int id = (Integer) buffer[i];  // Safely cast
                    if (id == fireID) {
                        buffer[i] = null; // Remove the event
                        notifyAll(); // Notify waiting threads
                        return true;
                    }
                }
            }

            // If FireEvent not found, wait for new data and try again
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null; // Exit if interrupted
            }
        }
    }






}