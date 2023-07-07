package com.example.phoneimu_socket;

import java.util.Arrays;

public class DSP {
    private double[] originalSignal;
    private double[] smoothedSignal;

    public DSP() {

    }

    public double[] smoothSignal(int windowSize, String mode, double[] originalSignal) {
        // We smooth the signal with triangular formulation

        this.originalSignal = originalSignal;
        this.smoothedSignal = new double[windowSize];

        for (int i = 2; i <= windowSize - 3; i++) {
            this.smoothedSignal[i] += this.originalSignal[i - 2] + 2 * this.originalSignal[i - 1];
            this.smoothedSignal[i] += 3 * this.originalSignal[i];
            this.smoothedSignal[i] += 2 * this.originalSignal[i + 1] + this.originalSignal[i + 2];
            this.smoothedSignal[i] /= 9;
        }
        this.smoothedSignal[0] = this.smoothedSignal[1] = this.smoothedSignal[2];
        this.smoothedSignal[windowSize - 1] = this.smoothedSignal[windowSize - 2] = this.smoothedSignal[windowSize - 3];

        return smoothedSignal;

    }

    public double medianGetSignal(int windowSize, double[] signal) {
//        this.smoothedSignal = new double[windowSize];

        Arrays.sort(signal);
        return signal[windowSize / 2 + 1];

//        int mean = 0;
//        for (int i = 0; i < windowSize; i++)
//            mean += originalSignal[i];

//        return mean / windowSize;
    }

    public double[] getOriginalSignal() {
        return originalSignal;
    }

    public void setOriginalSignal(double[] originalSignal) {
        this.originalSignal = originalSignal;
    }

    public double[] getSmoothedSignal() {
        return smoothedSignal;
    }

    public void setSmoothedSignal(double[] smoothedSignal) {
        this.smoothedSignal = smoothedSignal;
    }
}
