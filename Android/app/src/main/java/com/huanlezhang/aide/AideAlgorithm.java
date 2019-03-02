package com.huanlezhang.aide;

import android.util.Log;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.commons.collections4.iterators.PermutationIterator;

public class AideAlgorithm {

    static final String TAG = "DTC AideAlgorithm";

    /*
    return HashMap<String, String>
        String: location string key
        String: the corresponding device address
    */
    static public HashMap<String, String> result(final HashMap<String, BleLocViewInfoStore> locViewMap) {

        HashMap<String, String> ret = new HashMap<>();

        int M = 0; // device
        int N = 0; // location

        for (String locKey: locViewMap.keySet()) {
            M = locViewMap.get(locKey).getDeviceNumber();
            N = locViewMap.size();
        }

        if (M < N) {
            // #devices must >= #location
            Log.e(TAG, String.format("#device: %d < #location: %d", M, N));
            System.exit(-1);
        }

        double[][] D = new double[M][N]; // RSS matrix

        int locIndex = 0, decIndex = 0;

        // alphabetically sorted location string key
        SortedSet<String> keySet = new TreeSet<>(locViewMap.keySet());
        for (String locKey: keySet) {
            BleLocViewInfoStore bleLocViewInfoStore = locViewMap.get(locKey);

            if (bleLocViewInfoStore == null) {
                Log.e(TAG, "bleLocViewInfoStore null pointer");
                System.exit(-1);
            }

            ArrayList<Double> meanArray = bleLocViewInfoStore.calculateMean();

            decIndex = 0;
            for (Double meanPoint: meanArray) {
                D[decIndex][locIndex] = meanPoint;
                decIndex++;
            }

            locIndex++;
        }

        double[][] V = new double[M][N]; // likelihood matrix

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                V[i][j] = 0;
                for (int k = 0; k < N; k++) {
                    V[i][j] += D[i][j] - D[i][k];
                }
            }
        }

        double max_voting_value = -1E9;
        List<Integer> P_item_save = null;

        Iterator<int[]> C_iterator = CombinatoricsUtils.combinationsIterator(M, N);
        while (C_iterator.hasNext()) {
            int[] C_item = C_iterator.next();// array length = N
            List<Integer> deviceSelectedList = Ints.asList(C_item);

            PermutationIterator P_iterator = new PermutationIterator(deviceSelectedList);
            while (P_iterator.hasNext()) {
                List<Integer> P_item = P_iterator.next();

                double temp_voting_value = 0;
                for (int i = 0; i < N; i++) {
                    temp_voting_value += V[P_item.get(i)][i];
                }
                if (temp_voting_value > max_voting_value) {
                    max_voting_value = temp_voting_value;
                    P_item_save = P_item;
                }
            }
        }

        // sorted loc key to array for indexing
        ArrayList<String> locKeyArray = new ArrayList<>();
        for (String locKey: keySet) {
            locKeyArray.add(locKey);
        }

        for (int i = 0; i < N; i++) {
            BleLocViewInfoStore bleLocViewInfoStore = locViewMap.get(locKeyArray.get(i));
            ArrayList<String> deviceAddrArray = bleLocViewInfoStore.getSortedDeviceAddrArray();

            ret.put(locKeyArray.get(i), deviceAddrArray.get(P_item_save.get(i)));
        }

        return ret;
    }

    static void printMatrix(double[][] data) {

        String str;
        for (int i = 0; i < data.length; i++) {
            str = "";
            for (int j = 0; j < data[0].length; j++) {
                str += Double.toString(data[i][j]) + " ";
            }
            Log.d(TAG, str);
        }
    }

    static void printArray(double[] data) {
        String str = "";
        for (int i = 0; i < data.length; i++){
            str += Double.toString(data[i]) + " ";
        }
        Log.d(TAG, str);
    }

    static void printArray(int[] data) {
        String str = "";
        for (int i = 0; i < data.length; i++){
            str += Integer.toString(data[i]) + " ";
        }
        Log.d(TAG, str);
    }

    static void printArray(List<Integer> data) {
        String str = "";
        for (Integer integer: data) {
            str += Integer.toString(integer) + " ";
        }
        Log.d(TAG, str);
    }

}
