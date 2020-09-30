package cz.to2.noe.load.testing;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by pavelnovotny on 26.09.20.
 */
public class Pokus {

    public static void main (String[] args) {
        Queue<Integer> myQ = new LinkedList<Integer>();
        myQ.add(6);
        myQ.add(1);
        myQ.add(3);
        System.out.println(myQ);   // 1 6 3
        int first = myQ.poll();    // retrieve and remove the first element
        System.out.println(first); // 1
        System.out.println(myQ);   // 6 3

    }

}
