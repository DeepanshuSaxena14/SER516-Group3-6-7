package edu.asu.ser516.taiga;


public final class AucComputer {


   private AucComputer() {}


   public static record AucResult(double aucWork, double aucValue, double aucRatio) {}


   public static AucResult computeFromRemaining(double[] remaining) {
       if (remaining == null || remaining.length <= 1) {
           return new AucResult(0.0, 0.0, 1.0);
       }


       int n = remaining.length;
       int sprintDays = n - 1;


       // trapezoid integration for actual remaining curve
       double aucWork = trapezoid(remaining);


       // ideal straight-line remaining from totalPoints -> 0
       double totalPoints = remaining[0];
       double[] ideal = new double[n];
       for (int i = 0; i < n; i++) {
           ideal[i] = totalPoints * (1.0 - ((double) i / sprintDays));
       }
       double aucValue = trapezoid(ideal);


       double aucRatio = aucValue == 0.0 ? 1.0 : aucWork / aucValue;


       return new AucResult(round2(aucWork), round2(aucValue), round2(aucRatio));
   }


   private static double trapezoid(double[] y) {
       double area = 0.0;
       for (int i = 0; i < y.length - 1; i++) {
           area += (y[i] + y[i + 1]) / 2.0;
       }
       return area;
   }


   private static double round2(double v) {
       return Math.round(v * 100.0) / 100.0;
   }
}