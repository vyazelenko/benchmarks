package uk.co.real_logic.benchmarks;

import org.HdrHistogram.DoubleHistogram;
import org.HdrHistogram.Histogram;
import org.openjdk.jmh.annotations.*;

import java.util.Random;

@State(Scope.Thread)
public class MatrixBenchmark
{
    public static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 2;
    public static final int RUNS = 200_000;
    public static final int PERIODS = 30 * 12;
    public static final int TOTAL_RUNS = RUNS * PERIODS;
    public static final double MONTHLY_PAYMENT = 2500.0; // pennies rather than pounds

    final double[] lumpSums = new double[PERIODS];
    final double[] riskFactors = new double[TOTAL_RUNS];
    final double[] returnFactors = new double[TOTAL_RUNS];
    final double[] totalReturns = new double[TOTAL_RUNS + RUNS];
    final double[][] quantiles = new double[PERIODS][];
    final Histogram histogram = new Histogram(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);

    @Setup
    public void setup()
    {
        final Random random = new Random();
        for (int i = 0; i < TOTAL_RUNS; i++)
        {
            riskFactors[i] = random.nextDouble();
        }

        for (int i = 0; i < PERIODS; i++)
        {
            quantiles[i] = new double[9];
            lumpSums[i] = MONTHLY_PAYMENT;
        }

        final double m = 0.7;
        final double c = 0.25 + 1.0;

        computeReturnFactors(m, c);
        computeTotalReturns();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    public double[] testReturnFactorsComputation()
    {
        final double m = 0.7;
        final double c = 0.25 + 1.0;

        return computeReturnFactors(m, c);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    public double[] testTotalReturnsComputation()
    {
        return computeTotalReturns();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    public double[][] testQuantilesComputation()
    {
        int simulation = RUNS;

        for (int p = 0; p < PERIODS; p++)
        {
            histogram.reset();

            for (int r = 0; r < RUNS; r++)
            {
                histogram.recordValue((long)totalReturns[simulation++]);
            }

            quantiles[p][0] = histogram.getValueAtPercentile(1.0);
            quantiles[p][1] = histogram.getValueAtPercentile(5.0);
            quantiles[p][2] = histogram.getValueAtPercentile(10.0);
            quantiles[p][3] = histogram.getValueAtPercentile(25.0);
            quantiles[p][4] = histogram.getValueAtPercentile(50.0);
            quantiles[p][5] = histogram.getValueAtPercentile(75.0);
            quantiles[p][6] = histogram.getValueAtPercentile(90.0);
            quantiles[p][7] = histogram.getValueAtPercentile(95.0);
            quantiles[p][8] = histogram.getValueAtPercentile(99.0);
        }

        return quantiles;
    }

    private double[] computeReturnFactors(final double m, final double c)
    {
        for (int i = 0; i < TOTAL_RUNS; i++)
        {
            returnFactors[i] = (riskFactors[i] * m) + c;
        }

        return returnFactors;
    }

    private double[] computeTotalReturns()
    {
        int simulationRun = RUNS;

        for (int p = 0; p < PERIODS; p++)
        {
            final double lumpSum = lumpSums[p];

            for (int r = 0; r < RUNS; r++)
            {
                final int resultIndex = simulationRun + r;
                final int inputIndex = resultIndex - RUNS;
                totalReturns[resultIndex] = (totalReturns[inputIndex] + lumpSum) * returnFactors[inputIndex];
            }

            simulationRun += RUNS;
        }

        return totalReturns;
    }
}
