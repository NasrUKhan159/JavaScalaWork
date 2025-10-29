package org.example;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;

// Use LM-algorithm to find solution for the following equation:
// f(x, y, z) = (5xy - 1)^{4} + (4yz - 11)^{3} + (6xz - 7)^{2}

public class Main{

    public static void LMAlgorithm(double initialx, double initialy, double initialz){
        MultivariateVectorFunction model = point -> {
            double x = point[0];
            double y = point[1];
            double z = point[2];
            return new double[] {20*y*Math.pow(5*x*y - 1, 3) + 12*z*(6*x*z - 7),
                    12*z*Math.pow(4*y*z - 11, 2) + 20*x*Math.pow(5*x*y - 1, 3),
                    12*x*(6*x*z - 7) + 12*y*Math.pow(4*y*z - 11, 2)};
        };
        MultivariateMatrixFunction mtx = point -> {
            double x = point[0];
            double y = point[1];
            double z = point[2];
            double x11 = 300*Math.pow(y, 2)*Math.pow(5*x*y - 1, 2) + 72*Math.pow(z, 2);
            double x12 = 20*Math.pow(5*x*y - 1, 2)*(20*x*y - 1);
            double x13 = 12*(12*x*z - 7);
            double x21 = x12;
            double x22 = 96*Math.pow(z, 2)*(4*y*z - 11) + 300*Math.pow(x, 2)*Math.pow(5*x*y - 1, 2);
            double x23 = 12*(4*y*z - 11)*(12*y*z - 11);
            double x31 = x13;
            double x32 = x23;
            double x33 = 72*Math.pow(x, 2) + 96*Math.pow(y,2)*(4*y*z - 11);
            return new double[][] {{x11, x12, x13}, {x21, x22, x23}, {x31, x32, x33}};
        };

        // Initial guess
        RealVector initialGuess = new ArrayRealVector(new double[] {initialx, initialy, initialz});

        // Build the least squares problem
        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .start(initialGuess)
                .model(model, mtx)
                .target(new double[] {0, 0, 0})
                // since we want residuals to equate to zero
                .lazyEvaluation(false)
                .maxEvaluations(1000)
                .maxIterations(1000)
                .build();
        // Solve using Levenberg-Marquadt
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        double[] evaluation = optimizer.optimize(problem).getPoint().toArray();
        System.out.printf("Optimal values: x = %.5f, y = %.5f, z = %.5f%n", evaluation[0], evaluation[1], evaluation[2]);
    }
    public static void main(String[] args){
        double case1Initialx = 0;
        double case1Initialy = 0;
        double case1Initialz = 0;
        double case2Initialx = 0.5;
        double case2Initialy = 0.5;
        double case2Initialz = 0.5;
        System.out.println("Running LM-Algorithm for case 1 set of initial values:");
        System.out.println("x = " + case1Initialx);
        System.out.println("y = " + case1Initialy);
        System.out.println("z = " + case1Initialz);
        LMAlgorithm(case1Initialx, case1Initialy, case1Initialz);
        System.out.println("Running LM-Algorithm for case 2 set of initial values:");
        System.out.println("x = " + case2Initialx);
        System.out.println("y = " + case2Initialy);
        System.out.println("z = " + case2Initialz);
        LMAlgorithm(case2Initialx, case2Initialy, case2Initialz);
    }
}